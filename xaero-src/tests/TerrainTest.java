import dev.liber.api.EnvType;
import dev.liber.api.Liber;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;

public class TerrainTest {
   static int fails = 0;

   public static void main(String[] args) throws Exception {
      File root = Files.createTempDirectory("xaero-terrain").toFile();
      Liber.bootstrap(EnvType.CLIENT, root.toPath(), root.toPath(), java.util.Collections.emptyList());
      tq fmt = new tq(new File(root, "saves"));
      wt sh = fmt.a("TerraWorld", false);
      fd w = new fd(sh, "TerraWorld", 987654321L);
      int[] pixels = new int[16384];
      short[] heights = new short[16641];

      long[][] spots = new long[][]{{0L, 0L}, {200L, 200L}, {-350L, 150L}, {600L, -450L}, {1200L, 900L}, {-800L, -800L}};
      long[] caveSpot = findCave(w, 0, 0);
      long t0 = System.nanoTime();
      lm tch = w.c(3, 3);

      for(int tt = 0; tt < 25; ++tt) {
         XaeroMapData.buildChunkPixels(tch, w);
      }

      long t1 = System.nanoTime();
      double msPerChunk = (double)(t1 - t0) / 1000000.0D / 25.0D;
      System.out.println("[INFO] buildChunkPixels avg " + Math.round(msPerChunk * 100.0D) / 100.0D + " ms/chunk");

      long[][] var20 = spots;
      int n = spots.length;

      int i;
      for(i = 0; i < n; ++i) {
         long[] s = var20[i];
         checkSpot(w, (double)s[0], (double)s[1], false, 70, pixels, heights);
      }

      System.out.println("[INFO] cave pocket at " + caveSpot[0] + "," + caveSpot[1] + " (floorY=" + caveSpot[2] + ")");
      if (caveSpot[2] > 0L) {
         checkSpot(w, (double)caveSpot[0], (double)caveSpot[1], true, (int)caveSpot[2] + 1, pixels, heights);
      } else {
         check("cave found near spawn", false);
      }

      wt sh2 = fmt.a("TerraNether", false);
      fd nw = new fd(sh2, "TerraNether", 987654321L, xa.a(-1));
      checkSpotNether(nw, pixels, heights);

      System.out.println(fails == 0 ? "== TERRAIN TEST: ALL PASS ==" : "== TERRAIN TEST: " + fails + " FAILURES ==");
      System.exit(fails == 0 ? 0 : 1);
   }

   private static void checkSpot(fd w, double px, double pz, boolean cave, int py, int[] pixels, short[] heights) {
      XaeroMapData.sampleMap(w, px, pz, 128, py, cave, false, pixels, heights);
      int voids = 0;
      HashSet colors = new HashSet();
      int bad = 0;

      int i;
      for(i = 0; i < pixels.length; ++i) {
         int p = pixels[i];
         if (p == -15724524) {
            ++voids;
         } else {
            colors.add(Integer.valueOf(p & 16777215));
            if ((p >>> 24 & 255) != 255) {
               ++bad;
            }
         }
      }

      String tag = (cave ? "cave" : "surface") + "@" + (int)px + "," + (int)pz;
      check(tag + ": no transparent leak (" + bad + ")", bad == 0);
      check(tag + ": not all void (" + voids + "/16384)", voids < 16384);
      check(tag + ": color variety " + colors.size() + " >= 3", colors.size() >= 3);
      int pcx = XaeroMapData.floorDiv16((int)px);
      int pcz = XaeroMapData.floorDiv16((int)pz);
      lm ch = w.c(pcx, pcz);
      if (ch != null && ch.b != null && XaeroMapData.hasAny(ch.b)) {
         int[] t1 = cave ? XaeroMapData.caveChunkPixels(ch, w, py) : XaeroMapData.buildChunkPixels(ch, w);
         int[] t2 = cave ? XaeroMapData.caveChunkPixels(ch, w, py) : XaeroMapData.buildChunkPixels(ch, w);
         boolean pure = true;

         for(i = 0; i < 256; ++i) {
            if (t1[i] != t2[i]) {
               pure = false;
               break;
            }
         }

         check(tag + ": tile builder pure", pure);
      } else {
         check(tag + ": center chunk available", false);
      }
   }

   private static void checkSpotNether(fd nw, int[] pixels, short[] heights) {
      XaeroMapData.sampleMap(nw, 0.0D, 0.0D, 128, 40, true, false, pixels, heights);
      int voids = 0;
      HashSet colors = new HashSet();

      for(int i = 0; i < pixels.length; ++i) {
         int p = pixels[i];
         if (p == -15724524) {
            ++voids;
         } else {
            colors.add(Integer.valueOf(p & 16777215));
         }
      }

      check("nether cave-mode: colored pixels " + (16384 - voids) + " > 2000", 16384 - voids > 2000);
      check("nether cave-mode: variety " + colors.size() + " >= 2", colors.size() >= 2);
      System.out.println("[INFO] nether: voids=" + voids + " colors=" + colors.size());
   }

   private static long[] findCave(fd w, int bx, int bz) {
      for(int x = bx + 4; x < bx + 60; ++x) {
         for(int z = bz + 4; z < bz + 60; ++z) {
            for(int y = 40; y >= 12; --y) {
               if (w.a(x, y, z) == 0 && w.a(x, y + 1, z) == 0 && w.a(x, y - 1, z) != 0 && w.a(x + 1, y, z) == 0 && w.a(x - 1, y, z) == 0) {
                  return new long[]{(long)x, (long)z, (long)(y - 1)};
               }
            }
         }
      }

      return new long[]{0L, 0L, -1L};
   }

   static void check(String name, boolean ok) {
      System.out.println((ok ? "[OK]   " : "[FAIL] ") + name);
      if (!ok) {
         ++fails;
      }
   }
}
