import dev.liber.api.Liber;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;

final class XaeroTiles {
   private final Minecraft mc;
   private final LinkedHashMap mem = new LinkedHashMap();
   private final Set<Long> unsaved = new HashSet();
   private final Set<Long> loading = new HashSet();
   private final Set<Long> missing = new HashSet();
   private final ConcurrentLinkedQueue<long[]> loadQueue = new ConcurrentLinkedQueue();
   private final ConcurrentLinkedQueue<Object[]> saveQueue = new ConcurrentLinkedQueue();
   private final ConcurrentLinkedQueue<Object[]> resultQueue = new ConcurrentLinkedQueue();
   BufferedImage atlas = null;
   int texId = -1;
   int ax0 = 0;
   int az0 = 0;
   int awBlocks = 0;
   int atlasScale = 1;
   private int[] atlasPixels = null;
   private boolean needRebuild = true;
   private long lastRefresh = 0L;
   private int rebuildCooldown = 0;
   private int refreshCursor = 0;
   private int lastCx0 = Integer.MIN_VALUE;
   private int lastCz0 = Integer.MIN_VALUE;
   private int lastCx1 = Integer.MIN_VALUE;
   private int lastCz1 = Integer.MIN_VALUE;
   private int ensureTick = 0;
   private static final int MAX_MEM = 12288;

   XaeroTiles(Minecraft mc) {
      this.mc = mc;
      Thread t = new Thread(new Runnable() {
         public void run() {
            XaeroTiles.this.workerLoop();
         }
      }, "XaeroTileIO");
      t.setDaemon(true);
      t.start();
   }

   private void workerLoop() {
      while(true) {
         try {
            boolean idle = true;
            Object[] save = this.saveQueue.poll();
            if (save != null) {
               idle = false;
               writePng((File)save[2], (Integer)save[3], (Integer)save[4], (int[])save[1]);
            }

            long[] req = (long[])this.loadQueue.poll();
            if (req != null) {
               idle = false;
               int[] px = readPng(dirOf(this.mc.f), (int)req[1], (int)req[2]);
               this.resultQueue.add(new Object[]{Long.valueOf(req[0]), px});
            }

            if (idle) {
               Thread.sleep(4L);
            }
         } catch (InterruptedException var6) {
            return;
         } catch (Throwable var7) {
            ;
         }
      }
   }

   void ensure(fd w, double x0, double z0, double x1, double z1) {
      int cx0 = XaeroMapData.floorDiv16((int)Math.floor(x0));
      int cz0 = XaeroMapData.floorDiv16((int)Math.floor(z0));
      int cx1 = XaeroMapData.floorDiv16((int)Math.floor(x1));
      int cz1 = XaeroMapData.floorDiv16((int)Math.floor(z1));
      if (cx1 - cx0 > 63) {
         cx1 = cx0 + 63;
      }

      if (cz1 - cz0 > 63) {
         cz1 = cz0 + 63;
      }

      boolean added = false;
      int liveBudget = 2;
      boolean sameRange = cx0 == this.lastCx0 && cz0 == this.lastCz0 && cx1 == this.lastCx1 && cz1 == this.lastCz1;
      ++this.ensureTick;
      if (sameRange && this.ensureTick % 10 != 0 && this.loadQueue.isEmpty()) {
         this.pollResults();
         return;
      }

      this.lastCx0 = cx0;
      this.lastCz0 = cz0;
      this.lastCx1 = cx1;
      this.lastCz1 = cz1;

      for(int cx = cx0; cx <= cx1; ++cx) {
         for(int cz = cz0; cz <= cz1; ++cz) {
            Long k = Long.valueOf(XaeroMapData.chunkKey(cx, cz));
            if (!this.mem.containsKey(k) && !this.loading.contains(k) && !this.missing.contains(k)) {
               if (liveBudget <= 0) {
                  continue;
               }

               lm ch = w.c(cx, cz);
               if (ch != null && ch.b != null && XaeroMapData.hasAny(ch.b)) {
                  --liveBudget;
                  this.mem.put(k, XaeroMapData.buildChunkPixels(ch, w));
                  this.unsaved.add(k);
                  added = true;
               } else {
                  this.loading.add(k);
                  this.loadQueue.add(new long[]{k.longValue(), (long)cx, (long)cz});
               }
            }
         }
      }

      this.pollResults();
      if (added) {
         this.needRebuild = true;
      }

      if (this.mem.size() > MAX_MEM) {
         this.evictFar(cx0 - 96, cz0 - 96, cx1 + 96, cz1 + 96);
      }
   }

   void refreshAround(fd w, double px, double pz) {
      long now = System.currentTimeMillis();
      long gate = this.refreshCursor == 0 ? 4000L : 900L;
      if (now - this.lastRefresh > gate) {
         this.lastRefresh = now;
         int pcx = XaeroMapData.floorDiv16((int)Math.floor(px));
         int pcz = XaeroMapData.floorDiv16((int)Math.floor(pz));
         long deadline = System.nanoTime() + 1500000L;
         int n = 0;

         while(n < 25 && System.nanoTime() < deadline) {
            int idx = this.refreshCursor;
            this.refreshCursor = (this.refreshCursor + 1) % 25;
            ++n;
            int cx = pcx + idx % 5 - 2;
            int cz = pcz + idx / 5 - 2;
            Long k = Long.valueOf(XaeroMapData.chunkKey(cx, cz));
            lm ch = w.c(cx, cz);
            if (ch != null && ch.b != null && XaeroMapData.hasAny(ch.b)) {
               int[] fresh = XaeroMapData.buildChunkPixels(ch, w);
               int[] old = (int[])this.mem.get(k);
               if (old == null || !Arrays.equals(old, fresh)) {
                  this.mem.put(k, fresh);
                  this.unsaved.add(k);
                  this.missing.remove(k);
                  this.needRebuild = true;
               }
            }
         }
      }
   }

   private void pollResults() {
      Object[] r;
      while((r = (Object[])this.resultQueue.poll()) != null) {
         Long k = (Long)r[0];
         int[] px = (int[])r[1];
         this.loading.remove(k);
         if (px != null) {
            this.mem.put(k, px);
            this.unsaved.add(k);
         } else {
            this.missing.add(k);
         }

         this.needRebuild = true;
      }
   }

   private void evictFar(int cx0, int cz0, int cx1, int cz1) {
      Iterator it = this.mem.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry e = (Map.Entry)it.next();
         long k = ((Long)e.getKey()).longValue();
         int cx = (int)(k >> 32);
         int cz = (int)k;
         if (cx < cx0 || cx > cx1 || cz < cz0 || cz > cz1) {
            it.remove();
            this.unsaved.remove(Long.valueOf(k));
         }
      }
   }

   void rebuildIfNeeded(double camX, double camZ, double viewHalfW, double viewHalfH) {
      this.pollResults();
      double needBlocks = Math.ceil(Math.max(viewHalfW, viewHalfH) * 2.0D + 128.0D);
      int scale = 1;
      while(needBlocks / (double)scale > 1024.0D && scale < 8) {
         scale *= 2;
      }

      int wantSpan = (int)Math.min(1024.0D, needBlocks / (double)scale);
      wantSpan = (wantSpan + 15) / 16 * 16;
      if (this.atlas == null || this.texId < 0 || wantSpan != this.awBlocks || scale != this.atlasScale) {
         this.needRebuild = true;
      } else {
         double distC = Math.max(Math.abs(camX - (double)this.ax0 - (double)this.awBlocks / 2.0D), Math.abs(camZ - (double)this.az0 - (double)this.awBlocks / 2.0D));
         if (distC > (double)this.awBlocks * 0.25D) {
            this.needRebuild = true;
         }
      }

      if (viewHalfW * 2.0D > (double)this.awBlocks) {
         this.needRebuild = true;
      }

      if (this.needRebuild) {
         if (this.rebuildCooldown > 0) {
            --this.rebuildCooldown;
         } else {
            this.rebuild(camX, camZ, wantSpan, scale);
            this.rebuildCooldown = 10;
         }
      }
   }

   private void rebuild(double camX, double camZ, int spanPx, int scale) {
      if (this.atlas == null || this.atlas.getWidth() != spanPx) {
         BufferedImage im = new BufferedImage(spanPx, spanPx, 1);
         this.atlasPixels = ((DataBufferInt)im.getRaster().getDataBuffer()).getData();
         this.atlas = im;
      }

      this.awBlocks = spanPx * scale;
      this.atlasScale = scale;
      this.ax0 = (int)Math.floor(camX / (double)scale) * scale - this.awBlocks / 2;
      this.az0 = (int)Math.floor(camZ / (double)scale) * scale - this.awBlocks / 2;
      Arrays.fill(this.atlasPixels, -15658735);
      int ccx0 = XaeroMapData.floorDiv16(this.ax0);
      int ccz0 = XaeroMapData.floorDiv16(this.az0);
      int ccx1 = XaeroMapData.floorDiv16(this.ax0 + this.awBlocks - 1);
      int ccz1 = XaeroMapData.floorDiv16(this.az0 + this.awBlocks - 1);

      for(int cx = ccx0; cx <= ccx1; ++cx) {
         for(int cz = ccz0; cz <= ccz1; ++cz) {
            int[] px = (int[])this.mem.get(Long.valueOf(XaeroMapData.chunkKey(cx, cz)));
            if (px != null) {
               blitScaled(this.atlasPixels, spanPx, (cx << 4) - this.ax0, (cz << 4) - this.az0, px, scale);
            }
         }
      }

      try {
         if (this.texId < 0) {
            this.texId = this.mc.p.a(this.atlas);
         } else {
            this.mc.p.a(this.atlas, this.texId);
         }
      } catch (Throwable var13) {
         var13.printStackTrace();
      }

      this.needRebuild = false;
   }

   private static void blitScaled(int[] dst, int dstW, int ox, int oz, int[] src, int scale) {
      if (scale == 1) {
         XaeroMapData.blit(dst, dstW, ox, oz, src);
      } else {
         for(int z = 0; z < 16; z += scale) {
            for(int x = 0; x < 16; x += scale) {
               int color = src[z << 4 | x];
               int dCol = ox + x / scale;
               int dRow = oz + z / scale;
               if (dCol >= 0 && dRow >= 0 && dCol < dstW && dRow < dstW) {
                  dst[dRow * dstW + dCol] = color;
               }
            }
         }
      }
   }

   void flush(boolean force) {
      this.pollResults();
      if (!this.unsaved.isEmpty()) {
         File dir = dirOf(this.mc.f);
         Iterator it = this.unsaved.iterator();

         while(it.hasNext()) {
            long k = ((Long)it.next()).longValue();
            int[] px = (int[])this.mem.get(Long.valueOf(k));
            if (px != null) {
               int cx = (int)(k >> 32);
               int cz = (int)k;
               this.saveQueue.add(new Object[]{new long[]{k}, px, dir, Integer.valueOf(cx), Integer.valueOf(cz)});
            }
         }

         this.unsaved.clear();
      }
      if (force && !this.saveQueue.isEmpty()) {
         long deadline = System.currentTimeMillis() + 3000L;

         while(!this.saveQueue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException var9) {
               break;
            }
         }
      }
   }

   private static File dirOf(Object world) {
      String key = XaeroWorlds.worldKeyOf(world);
      File dir = new File(Liber.getGameDir().toFile(), "xaero");
      dir = new File(dir, "worldmap");
      dir = new File(dir, key);
      dir = new File(dir, "dim" + XaeroWorlds.dimension(world));
      if (!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   private static int[] readPng(File dir, int cx, int cz) {
      try {
         File f = new File(dir, cx + "_" + cz + ".png");
         if (f.isFile()) {
            BufferedImage im = ImageIO.read(f);
            if (im != null && im.getWidth() == 16 && im.getHeight() == 16) {
               int[] row = new int[256];
               im.getRGB(0, 0, 16, 16, row, 0, 16);
               int[] out = new int[256];

               for(int z = 0; z < 16; ++z) {
                  for(int x = 0; x < 16; ++x) {
                     out[z << 4 | x] = row[z * 16 + x];
                  }
               }

               return out;
            }
         }
      } catch (Throwable var8) {
         ;
      }

      return null;
   }

   private static void writePng(File dir, int cx, int cz, int[] px) {
      try {
         if (!dir.exists()) {
            dir.mkdirs();
         }

         BufferedImage im = new BufferedImage(16, 16, 1);

         for(int z = 0; z < 16; ++z) {
            for(int x = 0; x < 16; ++x) {
               im.setRGB(x, z, px[z << 4 | x]);
            }
         }

         File tmp = new File(dir, cx + "_" + cz + ".png.tmp");
         ImageIO.write(im, "png", tmp);
         File dst = new File(dir, cx + "_" + cz + ".png");
         if (dst.exists()) {
            dst.delete();
         }

         tmp.renameTo(dst);
      } catch (Throwable var8) {
         System.out.println("[Xaero] failed to save tile " + cx + "," + cz);
      }
   }
}
