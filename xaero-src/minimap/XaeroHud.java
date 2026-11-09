import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class XaeroHud {
   private static final int TEX = 128;
   private static final int[] SPANS = new int[]{64, 128, 256, 512};
   private static final int VOID_COLOR = -15724524;
   private static boolean dead = false;
   private static boolean failedOnce = false;
   private static int zoomIdx = 1;
   private static int texId = -1;
   private static BufferedImage img = null;
   private static long nextSample = 0L;
   private static long zoomToastUntil = 0L;
   private static final int[] pixels = new int[16384];
   private static final short[] heights = new short[16641];
   private static volatile boolean dirty = true;

   private XaeroHud() {
   }

   public static void disable(Throwable t) {
      if (!dead) {
         dead = true;
         System.out.println("[XaeroMinimap] disabled after error");
         if (!failedOnce) {
            failedOnce = true;
            t.printStackTrace();
         }
      }
   }

   public static void markAllDirty() {
      dirty = true;
   }

   public static void forceResample() {
      nextSample = 0L;
      dirty = true;
   }

   public static void cycleZoom() {
      zoomIdx = (zoomIdx + 1) % SPANS.length;
      nextSample = 0L;
      zoomToastUntil = System.currentTimeMillis() + 1500L;
   }

   public static String zoomLabel() {
      return "\u89c6\u91cf: " + SPANS[zoomIdx] + " \u683c";
   }

   public static int currentSpan() {
      return SPANS[zoomIdx];
   }

   public static boolean isAlive() {
      return !dead && texId >= 0 && img != null;
   }

   public static void onFrame() {
      if (!dead) {
         try {
            frame();
         } catch (Throwable var1) {
            disable(var1);
         }
      }
   }

   private static void drainWheel() {
      try {
         if (org.lwjgl.input.Mouse.isCreated() && org.lwjgl.input.Mouse.hasWheel()) {
            org.lwjgl.input.Mouse.getDWheel();
         }
      } catch (Throwable var1) {
         ;
      }
   }

   private static void frame() {
      Minecraft mc = XaeroKeys.mc();
      if (mc != null && mc.f != null && mc.h != null && !mc.w && mc.p != null && mc.q != null && mc.t != null) {
         XaeroCfg.ensureLoaded();
         if (mc.r != null) {
            drainWheel();
         } else if (XaeroCfg.enabled) {
            fd w = mc.f;
            dc p = mc.h;
            long now = System.currentTimeMillis();
            XaeroColors.ensureColorizers(mc);
            if (img == null || texId < 0) {
               initTexture(mc);
            }

            if (now >= nextSample) {
               nextSample = now + 500L;
               sample(w, p);
            }

            int size = drawSize(mc);
         mc.t.b();
         GL11.glPushMatrix();
         GL11.glDisable(2884);
         drawMap(mc, size);
            if (XaeroCfg.showEntities) {
               drawMobs(w, p, size);
            }

            if (XaeroCfg.showPlayers) {
               drawPlayers(w, p, size);
            }

            if (XaeroCfg.showWaypoints) {
               drawWaypoints(mc, p, size);
            }

            drawArrow(p, size);
            drawText(mc, p, size, now);
            restoreGL();
            GL11.glPopMatrix();
         }
      }
   }

   private static int drawSize(Minecraft mc) {
      if (cachedSr == null || cachedSrW != mc.d || cachedSrH != mc.e) {
         cachedSrW = mc.d;
         cachedSrH = mc.e;
         cachedSr = new qq(mc.z, mc.d, mc.e);
      }

      int maxSide = Math.min(cachedSr.a(), cachedSr.b()) - 30;
      return XaeroCfg.clamp(XaeroCfg.size, 32, Math.max(48, maxSide));
   }

   private static qq cachedSr = null;
   private static int cachedSrW = -1;
   private static int cachedSrH = -1;

   private static void initTexture(Minecraft mc) {
      BufferedImage im = new BufferedImage(128, 128, 2);
      Graphics2D g = im.createGraphics();
      g.setBackground(new java.awt.Color(16, 16, 20));
      g.clearRect(0, 0, 128, 128);
      g.dispose();

      try {
         texId = mc.p.a(im);
         img = im;
      } catch (Throwable var4) {
         disable(var4);
      }
   }

   private static int buildBudget = 0;
   private static int lastPcx = Integer.MIN_VALUE;
   private static int lastPcz = Integer.MIN_VALUE;

   private static void sample(fd w, dc p) {
      int span = SPANS[zoomIdx];
      int pcx = in.b(p.aM) >> 4;
      int pcz = in.b(p.aO) >> 4;
      boolean moved = Math.abs(p.aM - lastSampleX) > 0.75D || Math.abs(p.aO - lastSampleZ) > 0.75D || Math.abs(p.aN - lastSampleY) > 6.0D;
      if (!moved && span == lastSpan && XaeroCfg.caveMode == lastCave && !tilesDirty) {
         return;
      }

      lastSampleX = p.aM;
      lastSampleZ = p.aO;
      lastSampleY = p.aN;
      lastSpan = span;
      lastCave = XaeroCfg.caveMode;
      tilesDirty = false;
      int x0 = in.b(p.aM) - (span >> 1);
      int z0 = in.b(p.aO) - (span >> 1);
      float step = (float)span / 128.0F;
      buildBudget = 4;
      boolean budgetHit = false;
      if (pcx != lastPcx || pcz != lastPcz) {
         lastPcx = pcx;
         lastPcz = pcz;
         ++caveGen;
      }

      if (XaeroCfg.caveMode && Math.abs(p.aN - lastCaveY) > 6.0D) {
         lastCaveY = p.aN;
         ++caveGen;
      }

      if (!XaeroCfg.caveMode) {
         refreshNearby(w, pcx, pcz);
      }

      int i;
      for(i = 0; i < 128; ++i) {
         colX[i] = x0 + Math.round((float)i * step);
         colZ[i] = z0 + Math.round((float)i * step);
      }

      int j;
      for(j = 0; j < 128; ++j) {
         int wz = colZ[j];

         for(i = 0; i < 128; ++i) {
            int wx = colX[i];
            int argb;
            if (XaeroCfg.caveMode) {
               int cx = wx >> 4;
               int cz = wz >> 4;
               if (Math.abs(cx - pcx) > 8 || Math.abs(cz - pcz) > 8) {
                  argb = VOID_COLOR;
               } else {
                  int cidx = (cx * 31 + cz) & 63;
                  int[] tile;
                  if (ccGen[cidx] == caveGen && ccCx[cidx] == cx && ccCz[cidx] == cz) {
                     tile = ccTile[cidx];
                  } else {
                     if (buildBudget <= 0) {
                        budgetHit = true;
                        argb = VOID_COLOR;
                        pixels[j * 128 + i] = argb;
                        continue;
                     }

                     --buildBudget;
                     lm ch = w.c(cx, cz);
                     tile = ch != null && ch.b != null && XaeroMapData.hasAny(ch.b) ? XaeroMapData.caveChunkPixels(ch, w, (int)p.aN) : EMPTY_TILE;
                     ccGen[cidx] = caveGen;
                     ccCx[cidx] = cx;
                     ccCz[cidx] = cz;
                     ccTile[cidx] = tile;
                  }

                  argb = tile == EMPTY_TILE ? VOID_COLOR : tile[(wz & 15) << 4 | wx & 15];
               }
            } else {
               int[] tile = tileAt(w, wx >> 4, wz >> 4);
               if (tile == null) {
                  budgetHit = true;
               }

               argb = tile == null || tile == EMPTY_TILE ? VOID_COLOR : tile[(wz & 15) << 4 | wx & 15];
            }

            if (XaeroCfg.circle) {
               float dx = (float)i - 63.5F;
               float dy = (float)j - 63.5F;
               float d2 = dx * dx + dy * dy;
               if (d2 > 3969.0F) {
                  argb = 0;
               } else if (d2 > 3721.0F) {
                  int a = (int)(255.0F * (63.0F - (float)Math.sqrt((double)d2)) / 2.0F);
                  argb = (argb & 16777215) | a << 24;
               }
            }

            pixels[j * 128 + i] = argb;
         }
      }

      if (budgetHit) {
         tilesDirty = true;
      }

      if (img != null && texId >= 0) {
         synchronized(img) {
            img.setRGB(0, 0, 128, 128, pixels, 0, 128);
         }

         dirty = true;
      }
   }

   private static double lastSampleX = 1.0E9D;
   private static double lastSampleZ = 1.0E9D;
   private static double lastSampleY = 1.0E9D;
   private static int lastSpan = -1;
   private static boolean lastCave = false;
   private static boolean tilesDirty = false;
   private static final int[] colX = new int[128];
   private static final int[] colZ = new int[128];

   public static void markTilesDirty() {
      tilesDirty = true;
   }

   private static final java.util.LinkedHashMap tileCache = new java.util.LinkedHashMap();
   private static long lastTileRefresh = 0L;
   private static final int MAX_TILES = 700;
   private static final int[] EMPTY_TILE = new int[0];
   private static final long[] dcKey = new long[32];
   private static final int[][] dcTile = new int[32][];
   private static final int[] ccGen = new int[64];
   private static final int[] ccCx = new int[64];
   private static final int[] ccCz = new int[64];
   private static final int[][] ccTile = new int[64][];
   private static int caveGen = 1;
   private static double lastCaveY = 1.0E9D;

   static {
      for(int i = 0; i < 32; ++i) {
         dcKey[i] = Long.MIN_VALUE;
      }
   }

   private static int[] tileAt(fd w, int cx, int cz) {
      int idx = (cx * 31 + cz) & 31;
      long k = XaeroMapData.chunkKey(cx, cz);
      if (dcKey[idx] == k) {
         return dcTile[idx];
      }

      int[] t = getSurfaceTile(w, cx, cz);
      if (t != null) {
         dcKey[idx] = k;
         dcTile[idx] = t;
      }

      return t;
   }

   private static void directInvalidate(int cx, int cz) {
      int idx = (cx * 31 + cz) & 31;
      if (dcKey[idx] == XaeroMapData.chunkKey(cx, cz)) {
         dcKey[idx] = Long.MIN_VALUE;
      }
   }


   private static int[] getSurfaceTile(fd w, int cx, int cz) {
      Long k = Long.valueOf(XaeroMapData.chunkKey(cx, cz));
      int[] t = (int[])tileCache.get(k);
      if (t != null) {
         return t;
      } else {
         if (buildBudget <= 0) {
            return null;
         }

         --buildBudget;
         lm ch = w.c(cx, cz);
         if (ch != null && ch.b != null && XaeroMapData.hasAny(ch.b)) {
            t = XaeroMapData.buildChunkPixels(ch, w);
         } else {
            t = EMPTY_TILE;
         }

         tileCache.put(k, t);
         if (tileCache.size() > MAX_TILES) {
            int toRemove = 120;

            for(java.util.Iterator it = tileCache.entrySet().iterator(); toRemove > 0 && it.hasNext(); --toRemove) {
               it.next();
               it.remove();
            }
         }

         return t;
      }
   }

   private static void refreshNearby(fd w, int pcx, int pcz) {
      long now = System.currentTimeMillis();
      if (now - lastTileRefresh >= 2000L) {
         lastTileRefresh = now;

         for(int n = 0; n < 2; ++n) {
            int p = (refreshIdx + n * 2) % 25;
            int dx = p / 5 - 2;
            int dz = p % 5 - 2;
            Long k = Long.valueOf(XaeroMapData.chunkKey(pcx + dx, pcz + dz));
            if (tileCache.containsKey(k)) {
               lm ch = w.c(pcx + dx, pcz + dz);
               if (ch != null && ch.b != null && XaeroMapData.hasAny(ch.b)) {
                  tileCache.put(k, XaeroMapData.buildChunkPixels(ch, w));
                  directInvalidate(pcx + dx, pcz + dz);
                  markTilesDirty();
               }
            }
         }

         refreshIdx = (refreshIdx + 4) % 25;
      }
   }

   private static int refreshIdx = 0;

   private static void drawMap(Minecraft mc, int size) {
      if (dirty) {
         dirty = false;

         try {
            synchronized(img) {
               mc.p.a(img, texId);
            }
         } catch (Throwable var8) {
            disable(var8);
         }
      }

      int x0 = 6;
      int y0 = 6;
      boolean circle = XaeroCfg.circle;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      if (circle) {
         fillCircle((float)x0 + (float)size / 2.0F, (float)y0 + (float)size / 2.0F, (float)size / 2.0F + 3.0F, 1308622848);
         fillCircle((float)x0 + (float)size / 2.0F, (float)y0 + (float)size / 2.0F, (float)size / 2.0F, -16777216);
      } else {
         fillRect((float)(x0 - 3), (float)(y0 - 3), (float)(size + 6), (float)(size + 6), 1308622848);
         fillRect((float)x0, (float)y0, (float)size, (float)size, -16777216);
      }

      GL11.glEnable(3553);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.4F);
      if (mc.p != null && texId >= 0) {
         mc.p.b(texId);
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex2f((float)x0, (float)y0);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex2f((float)x0, (float)(y0 + size));
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex2f((float)(x0 + size), (float)(y0 + size));
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex2f((float)(x0 + size), (float)y0);
      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glDisable(3553);
      if (circle) {
         ringOutline((float)x0 + (float)size / 2.0F, (float)y0 + (float)size / 2.0F, (float)size / 2.0F, -14277082);
      } else {
         frameRect((float)x0, (float)y0, (float)size);
      }

      GL11.glEnable(3553);
      sj fnt = mc.q;
      fnt.a("N", x0 + size / 2 - 3, y0 - 9, 16777215);
      handleWheel(mc);
   }

   private static void handleWheel(Minecraft mc) {
      try {
         if (org.lwjgl.input.Mouse.isCreated() && org.lwjgl.input.Mouse.hasWheel()) {
            int dw = org.lwjgl.input.Mouse.getDWheel();
            if (dw != 0) {
               int old = zoomIdx;
               zoomIdx += dw > 0 ? -1 : 1;
               if (zoomIdx < 0) {
                  zoomIdx = 0;
               }

               if (zoomIdx >= SPANS.length) {
                  zoomIdx = SPANS.length - 1;
               }

               if (zoomIdx != old) {
                  nextSample = 0L;
                  zoomToastUntil = System.currentTimeMillis() + 1500L;
               }
            }
         }
      } catch (Throwable var3) {
         ;
      }
   }

   private static void drawMobs(fd w, dc self, int size) {
      int span = SPANS[zoomIdx];
      float half = (float)(span >> 1);
      double px = self.aM;
      double pz = self.aO;
      List ents = w.b;
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      int n = ents.size();

      for(int i = 0; i < n; ++i) {
         Object o = ents.get(i);
         if (!(o instanceof sn)) {
            continue;
         }

         sn e = (sn)o;
         if (!e.be && !(e instanceof gs) && e != self) {
            float dx = (float)(e.aM - px);
            float dz = (float)(e.aO - pz);
            if (!(Math.abs(dx) > half) && !(Math.abs(dz) > half)) {
               int color;
               if (e instanceof bg) {
                  color = -12525424;
               } else if (e instanceof gz) {
                  color = -986896;
               } else {
                  if (!(e instanceof ii)) {
                     continue;
                  }

                  color = -5128;
               }

               float sx = 6.0F + (dx + half) / (float)span * (float)size;
               float sy = 6.0F + (dz + half) / (float)span * (float)size;
               if (insideMap(sx, sy, size)) {
                  fillRect(sx - 1.5F, sy - 1.5F, 3.0F, 3.0F, color | -16777216);
               }
            }
         }
      }

      GL11.glEnable(3553);
   }

   private static void drawPlayers(fd w, dc self, int size) {
      int span = SPANS[zoomIdx];
      float half = (float)(span >> 1);
      double px = self.aM;
      double pz = self.aO;
      List ents = w.b;
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      sj fnt = XaeroKeys.mc().q;
      int drawn = 0;

      for(int i = 0; i < ents.size() && drawn < 12; ++i) {
         Object o = ents.get(i);
         if (o instanceof gs && o != self) {
            gs pl = (gs)o;
            if (!pl.be) {
               float dx = (float)(pl.aM - px);
               float dz = (float)(pl.aO - pz);
               if (!(Math.abs(dx) > half) && !(Math.abs(dz) > half)) {
                  float sx = 6.0F + (dx + half) / (float)span * (float)size;
                  float sy = 6.0F + (dz + half) / (float)span * (float)size;
                  if (insideMap(sx, sy, size)) {
                     ++drawn;
                     GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                     fillRect(sx - 2.0F, sy - 2.0F, 4.0F, 4.0F, -1);
                     GL11.glDisable(3553);
                     GL11.glEnable(3553);
                     String nm = pl.l;
                     if (nm != null && nm.length() != 0) {
                        GL11.glDisable(3553);
                        GL11.glEnable(3553);
                        fnt.a(nm, (int)(sx - (float)(fnt.a(nm) / 2)), (int)(sy - 11.0F), -1);
                     }
                  }
               }
            }
         }
      }

      GL11.glEnable(3553);
   }

   private static void drawWaypoints(Minecraft mc, dc self, int size) {
      int span = SPANS[zoomIdx];
      float half = (float)(span >> 1);
      double px = self.aM;
      double pz = self.aO;
      List list = XaeroWpts.allDirect(mc.f);
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      XaeroWpts nearest = null;
      float nearestD = Float.MAX_VALUE;
      int drawn = 0;

      for(int i = 0; i < list.size() && drawn < 16; ++i) {
         XaeroWpts wp = (XaeroWpts)list.get(i);
         float dx = (float)((double)wp.x + 0.5D - px);
         float dz = (float)((double)wp.z + 0.5D - pz);
         float sx = 6.0F + clamp01((dx + half) / (float)span) * (float)size;
         float sy = 6.0F + clamp01((dz + half) / (float)span) * (float)size;
         boolean inside = Math.abs(dx) <= half && Math.abs(dz) <= half;
         float distSq = dx * dx + dz * dz;
         if (inside && distSq < nearestD) {
            nearestD = distSq;
            nearest = wp;
         }

         if (!inside) {
            fillRect(sx - 2.0F, sy - 2.0F, 4.0F, 4.0F, mixDark(wp.color));
         }

         if (insideMap(sx, sy, size)) {
            fillRect(sx - 2.5F, sy - 2.5F, 5.0F, 5.0F, wp.color);
         }

         ++drawn;
      }

      if (nearest != null && nearestD < 3600.0F) {
         String label = nearest.name + " " + (int)Math.sqrt((double)nearestD) + "m";
         sj fnt = mc.q;
         fnt.a(label, (int)(6.0F + (float)size / 2.0F + 10.0F), (int)(6.0F + (float)size / 2.0F + 10.0F), 16777215);
      }

      GL11.glEnable(3553);
   }

   private static int mixDark(int argb) {
      int r = ((argb >>> 16 & 255) * 2 / 3) << 16;
      int g = ((argb >>> 8 & 255) * 2 / 3) << 8;
      int b = (argb & 255) * 2 / 3;
      return r | g | b | -16777216;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static boolean insideMap(float sx, float sy, int size) {
      if (!XaeroCfg.circle) {
         return sx >= 6.0F && sy >= 6.0F && sx <= (float)(6 + size) && sy <= (float)(6 + size);
      } else {
         float dx = sx - (6.0F + (float)size / 2.0F);
         float dy = sy - (6.0F + (float)size / 2.0F);
         return dx * dx + dy * dy <= (float)(size * size) / 4.0F - 4.0F;
      }
   }

   private static void drawArrow(dc p, int size) {
      float cx = 6.0F + (float)size / 2.0F;
      float cy = 6.0F + (float)size / 2.0F;
      double yawRad = Math.toRadians((double)p.bj);
      float fx = -(float)Math.sin(yawRad);
      float fz = (float)Math.cos(yawRad);
      float perX = -fz;
      float perY = fx;
      float len = size >= 96 ? 8.0F : 6.5F;
      float tipX = cx + fx * len;
      float tipY = cy + fz * len;
      float b1x = cx - fx * len * 0.62F + perX * len * 0.55F;
      float b1y = cy - fz * len * 0.62F + perY * len * 0.55F;
      float b2x = cx - fx * len * 0.62F - perX * len * 0.55F;
      float b2y = cy - fz * len * 0.62F - perY * len * 0.55F;
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glBegin(9);
      GL11.glColor4f(0.08F, 0.08F, 0.08F, 1.0F);
      GL11.glVertex2f(cx + fx * (len + 2.0F), cy + fz * (len + 2.0F));
      GL11.glVertex2f(cx - fx * len * 0.62F + perX * len * 0.75F, cy - fz * len * 0.62F + perY * len * 0.75F);
      GL11.glVertex2f(cx - fx * len * 0.62F - perX * len * 0.75F, cy - fz * len * 0.62F - perY * len * 0.75F);
      GL11.glEnd();
      GL11.glBegin(9);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glVertex2f(tipX, tipY);
      GL11.glVertex2f(b1x, b1y);
      GL11.glVertex2f(b2x, b2y);
      GL11.glEnd();
      fillRect(cx - 1.5F, cy - 1.5F, 3.0F, 3.0F, -16777216);
      GL11.glEnable(3553);
   }

   private static void drawText(Minecraft mc, dc p, int size, long now) {
      sj fnt = mc.q;
      int ty = 6 + size + 6;
      if (XaeroCfg.showCoords) {
         int bx = in.b(p.aM);
         int by = (int)p.aN;
         int bz = in.b(p.aO);
         if (bx != lastTxtX || by != lastTxtY || bz != lastTxtZ) {
            lastTxtX = bx;
            lastTxtY = by;
            lastTxtZ = bz;
            cachedCoords = "X:" + bx + " Y:" + by + " Z:" + bz;
         }

         fnt.a(cachedCoords, 6, ty, 16777215);
      }

      if (XaeroCfg.showBiome) {
         int bx = in.b(p.aM);
         int bz = in.b(p.aO);
         if (bx != lastBioX || bz != lastBioZ) {
            lastBioX = bx;
            lastBioZ = bz;
            cachedBiome = XaeroColors.biomeName(mc.f, bx, bz);
         }

         fnt.a(cachedBiome, 6 + size - fnt.a(cachedBiome), ty, 11776947);
      }

      if (now < zoomToastUntil) {
         fnt.a(zoomLabel(), 6, ty + 11, 15658596);
      } else {
         fnt.a("M\u5730\u56fe B\u8def\u5f84\u70b9 K\u7ba1\u7406 O\u8bbe\u7f6e C\u6d1e\u7a74", 6, ty + 11, 6710886);
      }
   }

   private static int lastTxtX = Integer.MIN_VALUE;
   private static int lastTxtY = Integer.MIN_VALUE;
   private static int lastTxtZ = Integer.MIN_VALUE;
   private static String cachedCoords = "";
   private static int lastBioX = Integer.MIN_VALUE;
   private static int lastBioZ = Integer.MIN_VALUE;
   private static String cachedBiome = "";

   private static void fillRect(float x, float y, float w, float h, int argb) {
      float a = (float)(argb >>> 24 & 255) / 255.0F;
      float r = (float)(argb >>> 16 & 255) / 255.0F;
      float g = (float)(argb >>> 8 & 255) / 255.0F;
      float b = (float)(argb & 255) / 255.0F;
      GL11.glColor4f(r, g, b, a);
      GL11.glBegin(7);
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x, y + h);
      GL11.glVertex2f(x + w, y + h);
      GL11.glVertex2f(x + w, y);
      GL11.glEnd();
   }

   private static void fillCircle(float cx, float cy, float rad, int argb) {
      float a = (float)(argb >>> 24 & 255) / 255.0F;
      float r = (float)(argb >>> 16 & 255) / 255.0F;
      float g = (float)(argb >>> 8 & 255) / 255.0F;
      float b = (float)(argb & 255) / 255.0F;
      GL11.glColor4f(r, g, b, a);
      GL11.glBegin(6);

      for(int i = 0; i < 40; ++i) {
         double ang = 6.283185307179586D * (double)i / 40.0D;
         GL11.glVertex2f(cx + (float)Math.cos(ang) * rad, cy + (float)Math.sin(ang) * rad);
      }

      GL11.glEnd();
   }

   private static void ringOutline(float cx, float cy, float rad, int rgb) {
      float r = (float)(rgb >>> 16 & 255) / 255.0F;
      float g = (float)(rgb >>> 8 & 255) / 255.0F;
      float b = (float)(rgb & 255) / 255.0F;
      GL11.glColor4f(r, g, b, 1.0F);
      GL11.glBegin(3);

      for(int i = 0; i <= 40; ++i) {
         double ang = 6.283185307179586D * (double)i / 40.0D;
         GL11.glVertex2f(cx + (float)Math.cos(ang) * rad, cy + (float)Math.sin(ang) * rad);
      }

      GL11.glEnd();
   }

   private static void frameRect(float x, float y, float size) {
      int rgb = -14277082;
      fillRect(x, y, size, 1.0F, rgb);
      fillRect(x, y + size - 1.0F, size, 1.0F, rgb);
      fillRect(x, y, 1.0F, size, rgb);
      fillRect(x + size - 1.0F, y, 1.0F, size, rgb);
   }

   private static void restoreGL() {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthFunc(515);
      GL11.glDisable(3042);
      GL11.glEnable(2884);
      GL11.glAlphaFunc(516, 0.1F);
   }
}
