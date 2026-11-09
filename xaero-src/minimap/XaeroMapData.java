public final class XaeroMapData {
   private XaeroMapData() {
   }

   public static int topAt(lm ch, int lx, int lz) {
      if (ch == null || ch.b == null) {
         return -1;
      } else {
         int y = ch.h[lz << 4 | lx] & 255;
         if (y < 1 || y > 127) {
            y = 127;
         }

         while(y >= 0) {
            if ((ch.a(lx, y, lz) & 255) != 0) {
               return y;
            }

            --y;
         }

         return -1;
      }
   }

   private static int caveFloorAt(lm ch, int lx, int lz, int py) {
      if (ch == null || ch.b == null) {
         return -1;
      } else {
         int top = py + 8;
         if (top > 127) {
            top = 127;
         }

         int bottom = py - 40;
         if (bottom < 0) {
            bottom = 0;
         }

         for(int y = top; y > bottom; --y) {
            int id = ch.a(lx, y, lz) & 255;
            if (id == 0 || id == 8 || id == 9) {
               int below = y > 0 ? ch.a(lx, y - 1, lz) & 255 : -1;
               if (below > 0 && below != 8 && below != 9) {
                  return y - 1;
               }
            }
         }

         return -1;
      }
   }

   public static void sampleMap(fd w, double px, double pz, int span, int playerY, boolean cave, boolean circle, int[] pixels, short[] heights) {
      int x0 = in.b(px) - (span >> 1);
      int z0 = in.b(pz) - (span >> 1);
      float step = (float)span / 128.0F;
      int grid = 129;
      lm lastCh = null;
      int lastCx = Integer.MIN_VALUE;
      int lastCz = Integer.MIN_VALUE;

      int j;
      int i;
      for(j = 0; j < grid; ++j) {
         int wz = z0 + Math.round((float)j * step);

         for(i = 0; i < grid; ++i) {
            int wx = x0 + Math.round((float)i * step);
            int cx = wx >> 4;
            int cz = wz >> 4;
            if (cx != lastCx || cz != lastCz || lastCh == null) {
               lastCh = w.c(cx, cz);
               lastCx = cx;
               lastCz = cz;
            }

            int h = cave ? caveFloorAt(lastCh, wx & 15, wz & 15, playerY) : topAt(lastCh, wx & 15, wz & 15);
            heights[j * grid + i] = (short)h;
         }
      }

      lastCh = null;
      lastCx = Integer.MIN_VALUE;
      lastCz = Integer.MIN_VALUE;

      for(j = 0; j < 128; ++j) {
         int wz = z0 + Math.round((float)j * step);

         for(i = 0; i < 128; ++i) {
            int wx = x0 + Math.round((float)i * step);
            int cx = wx >> 4;
            int cz = wz >> 4;
            if (cx != lastCx || cz != lastCz || lastCh == null) {
               lastCh = w.c(cx, cz);
               lastCx = cx;
               lastCz = cz;
            }

            int idx = j * 128 + i;
            int r = heights[j * grid + i];
            int argb;
            if (r < 0) {
               argb = -15724524;
            } else {
               int id = lastCh.a(wx & 15, r, wz & 15) & 255;
               if (!cave && (id == 8 || id == 9)) {
                  int d = 0;

                  for(int yy = r - 1; yy >= 0 && yy > r - 14; --yy) {
                     int bid = lastCh.a(wx & 15, yy, wz & 15) & 255;
                     if (bid != 8 && bid != 9) {
                        break;
                     }

                     ++d;
                  }

                  float dk = 1.0F - Math.min(12, d) * 0.05F;
                  argb = XaeroColors.shade(XaeroColors.waterColor(w, wx, wz), 0.95F * dk);
               } else {
                  int hWest = i > 0 ? heights[j * grid + i - 1] : r;
                  if (hWest < 0) {
                     hWest = r;
                  }

                  int hNorth = j > 0 ? heights[(j - 1) * grid + i] : r;
                  if (hNorth < 0) {
                     hNorth = r;
                  }

                  float shade = 1.0F + (float)(hWest - r) * 0.06F + (float)(hNorth - r) * 0.04F;
                  shade += (float)(r - 64) * 6.0E-4F;
                  shade = shade < 0.6F ? 0.6F : (shade > 1.35F ? 1.35F : shade);
                  argb = XaeroColors.shade(XaeroColors.colorFor(w, id, wx, wz), shade);
               }
            }

            if (circle) {
               float dx = (float)i - 63.5F;
               float dy = (float)j - 63.5F;
               float dist = (float)Math.sqrt((double)(dx * dx + dy * dy));
               if (dist > 63.0F) {
                  argb = 0;
               } else if (dist > 61.0F) {
                  int a = (int)(255.0F * (63.0F - dist) / 2.0F);
                  argb = (argb & 16777215) | a << 24;
               }
            }

            pixels[idx] = argb;
         }
      }
   }

   public static int[] caveChunkPixels(lm ch, fd w, int py) {
      int bx0 = ch.j << 4;
      int bz0 = ch.k << 4;
      short[] hgt = new short[256];
      byte[] ids = new byte[256];

      int lz;
      int lx;
      int y;
      for(lz = 0; lz < 16; ++lz) {
         for(lx = 0; lx < 16; ++lx) {
            y = caveFloorAt(ch, lx, lz, py);
            int idx = lz << 4 | lx;
            hgt[idx] = (short)y;
            if (y >= 0) {
               ids[idx] = (byte)(ch.a(lx, y, lz) & 255);
            }
         }
      }

      int gTint = XaeroColors.grassTint(w, bx0 + 8, bz0 + 8);
      int fTint = XaeroColors.foliageTint(w, bx0 + 8, bz0 + 8);
      int bio = XaeroColors.biomeColor(w, bx0 + 8, bz0 + 8);
      int[] out = new int[256];

      for(lz = 0; lz < 16; ++lz) {
         for(lx = 0; lx < 16; ++lx) {
            int idx = lz << 4 | lx;
            y = hgt[idx];
            if (y < 0) {
               out[idx] = -15724524;
            } else {
               int wx = bx0 + lx;
               int wz = bz0 + lz;
               int id = ids[idx] & 255;
               float shade = 1.0F;
               int hWest = lx > 0 ? hgt[idx - 1] : y;
               if (hWest < 0) {
                  hWest = y;
               }

               int hNorth = lz > 0 ? hgt[idx - 16] : y;
               if (hNorth < 0) {
                  hNorth = y;
               }

               shade += (float)(hWest - y) * 0.06F + (float)(hNorth - y) * 0.04F;
               shade = shade < 0.6F ? 0.6F : (shade > 1.35F ? 1.35F : shade);
               int ly = y + 1 < 127 ? y + 1 : 127;
               int sky = ch.f.a(lx, ly, lz) & 15;
               int blk = ch.e.a(lx, ly, lz) & 15;
               int light = Math.max(sky, blk);
               float lb = 0.3F + 0.7F * (float)Math.min(15, light + 2) / 15.0F;
               shade *= lb * XaeroColors.detailNoise(wx, wz);
               int argb = XaeroColors.colorForTinted(id, gTint, fTint, bio);
               out[idx] = XaeroColors.shade(argb, shade);
            }
         }
      }

      return out;
   }

   public static boolean hasAny(byte[] arr) {
      for(int i = 0; i < arr.length; ++i) {
         if (arr[i] != 0) {
            return true;
         }
      }

      return false;
   }

   public static int floorDiv16(int v) {
      return v >= 0 ? v >> 4 : v - 15 >> 4;
   }

   public static long chunkKey(int cx, int cz) {
      return (long)cx << 32 | (long)cz & 4294967295L;
   }

   public static int[] buildChunkPixels(lm ch, fd w) {
      int bx0 = ch.j << 4;
      int bz0 = ch.k << 4;
      short[] hgt = new short[256];
      byte[] ids = new byte[256];
      byte[] depth = new byte[256];

      int lz;
      int lx;
      int y;
      for(lz = 0; lz < 16; ++lz) {
         for(lx = 0; lx < 16; ++lx) {
            y = topAt(ch, lx, lz);
            int idx = lz << 4 | lx;
            hgt[idx] = (short)y;
            if (y >= 0) {
               int id = ch.a(lx, y, lz) & 255;
               ids[idx] = (byte)id;
               if (id == 8 || id == 9) {
                  int d = 0;

                  for(int yy = y - 1; yy >= 0 && yy > y - 14; --yy) {
                     int below = ch.a(lx, yy, lz) & 255;
                     if (below != 8 && below != 9) {
                        break;
                     }

                     ++d;
                  }

                  depth[idx] = (byte)d;
               }
            }
         }
      }

      int gTint = XaeroColors.grassTint(w, bx0 + 8, bz0 + 8);
      int fTint = XaeroColors.foliageTint(w, bx0 + 8, bz0 + 8);
      int wTint = XaeroColors.waterTint(w, bx0 + 8, bz0 + 8);
      int bio = XaeroColors.biomeColor(w, bx0 + 8, bz0 + 8);
      int[] out = new int[256];

      for(lz = 0; lz < 16; ++lz) {
         for(lx = 0; lx < 16; ++lx) {
            int idx = lz << 4 | lx;
            y = hgt[idx];
            if (y < 0) {
               out[idx] = -16777213;
            } else {
               int wx = bx0 + lx;
               int wz = bz0 + lz;
               int id = ids[idx] & 255;
               int argb;
               if (id == 8 || id == 9) {
                  float dk = 1.0F - Math.min(12, depth[idx] & 255) * 0.05F;
                  argb = XaeroColors.shade(XaeroColors.waterColorTinted(wTint), 0.95F * dk);
               } else {
                  float shade = 1.0F;
                  int hWest = lx > 0 ? hgt[idx - 1] : y;
                  int hNorth = lz > 0 ? hgt[idx - 16] : y;
                  shade += (float)(hWest - y) * 0.08F + (float)(hNorth - y) * 0.05F;
                  shade += (float)(y - 64) * 8.0E-4F;
                  shade = shade < 0.55F ? 0.55F : (shade > 1.35F ? 1.35F : shade);
                  shade *= XaeroColors.detailNoise(wx, wz);
                  argb = XaeroColors.colorForTinted(id, gTint, fTint, bio);
                  argb = XaeroColors.shade(argb, shade);
               }

               out[idx] = argb;
            }
         }
      }

      return out;
   }

   public static void blit(int[] dst, int dstW, int ox, int oz, int[] src) {
      for(int z = 0; z < 16; ++z) {
         int row = oz + z;
         if (row >= 0 && row < dstW) {
            int col = ox;
            int srcPos = 0;
            int len = 16;
            if (col < 0) {
               srcPos = -col;
               len = 16 + col;
               col = 0;
            }

            if (col + len > dstW) {
               len = dstW - col;
            }

            if (len > 0) {
               System.arraycopy(src, (z << 4) + srcPos, dst, row * dstW + col, len);
            }
         }
      }
   }
}
