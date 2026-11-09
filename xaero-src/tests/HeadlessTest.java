import dev.liber.api.EnvType;
import dev.liber.api.Liber;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;

public class HeadlessTest {
   static int fails = 0;

   public static void main(String[] args) throws Exception {
      File root = Files.createTempDirectory("xaero-headless").toFile();
      Liber.bootstrap(EnvType.CLIENT, root.toPath(), root.toPath(), new ArrayList());
      File saves = new File(root, "saves");
      tq fmt = new tq(saves);
      wt sh = fmt.a("XaeroTestWorld", false);
      fd w = new fd(sh, "XaeroTestWorld", 123456789L);

      String key = XaeroWorlds.worldKeyOf(w);
      check("world key from save folder [got: " + key + "]", "XaeroTestWorld".equals(key));

      XaeroColors.ensureColorizers((Minecraft)null);
      check("grass colorizer loaded from jar", XaeroColors.hasGrassColorizer());
      int tint = XaeroColors.grassTint(w, 0, 0);
      int tr = tint >>> 16 & 255;
      int tg = tint >>> 8 & 255;
      int tb = tint & 255;
      System.out.println("[INFO] grass tint sample: " + Integer.toHexString(tint));
      check("grass tint non-gray (" + Integer.toHexString(tint) + ")", tg > tb && tg >= tr);

      XaeroCfg.load();
      int size0 = XaeroCfg.size;
      XaeroCfg.size = 200;
      XaeroCfg.circle = true;
      XaeroCfg.enabled = false;
      XaeroCfg.showPlayers = false;
      XaeroCfg.showCoords = false;
      XaeroCfg.showBiome = false;
      XaeroCfg.save();
      XaeroCfg.size = 0;
      XaeroCfg.circle = false;
      XaeroCfg.enabled = true;
      XaeroCfg.showPlayers = true;
      XaeroCfg.load();
      check("cfg roundtrip size=200", XaeroCfg.size == 200);
      check("cfg roundtrip circle=true", XaeroCfg.circle);
      check("cfg roundtrip enabled=false", !XaeroCfg.enabled);
      check("cfg roundtrip players=false", !XaeroCfg.showPlayers);
      check("cfg roundtrip coords=false", !XaeroCfg.showCoords);
      check("cfg roundtrip biome=false", !XaeroCfg.showBiome);
      XaeroCfg.enabled = true;
      XaeroCfg.showPlayers = true;
      XaeroCfg.showCoords = true;
      XaeroCfg.showBiome = true;
      XaeroCfg.size = size0;
      XaeroCfg.circle = false;
      XaeroCfg.save();

      HashSet cols = new HashSet();
      int greenish = 0;
      int total = 0;
      int nonVoid = 0;
      HashSet topIdsSeen = new HashSet();

      int cx;
      for(cx = -2; cx <= 2; ++cx) {
         for(int cz = -2; cz <= 2; ++cz) {
            lm ch = w.c(cx, cz);
            check("chunk generated " + cx + "," + cz, ch != null && ch.b != null);
            if (ch != null && ch.b != null) {
               int topIdProbe = XaeroMapData.topAt(ch, 5, 7);
               if (topIdProbe >= 0) {
                  topIdsSeen.add(Integer.valueOf(ch.a(5, topIdProbe, 7) & 255));
               }

               int[] px = XaeroMapData.buildChunkPixels(ch, w);
               check("pixels len 256", px.length == 256);

               for(int i = 0; i < px.length; ++i) {
                  int p = px[i];
                  ++total;
                  if ((p >>> 24 & 255) > 0 && p != -16777213) {
                     ++nonVoid;
                     cols.add(Integer.valueOf(p & 16777215));
                     int r = p >>> 16 & 255;
                     int g = p >>> 8 & 255;
                     int b = p & 255;
                     if (g > r + 10 && g > b + 20 && g > 60) {
                        ++greenish;
                     }
                  }
               }

               int top = XaeroMapData.topAt(ch, 5, 7);
               check("topAt valid at chunk " + cx + "," + cz, top >= 0 && top < 128);
            }
         }
      }

      check("distinct colors " + cols.size() + " > 12", cols.size() > 12);
      System.out.println("[INFO] top block ids seen: " + topIdsSeen);
      System.out.println("[INFO] sample colors: " + (new ArrayList(cols)).subList(0, Math.min(10, cols.size())));
      check("greenish(grass) pixels " + greenish + " > 50 OR no-grass-biome fallback ok", greenish > 50 || !topIdsSeen.contains(Integer.valueOf(2)));
      check("non-void ratio ok (" + nonVoid + "/" + total + ")", nonVoid > total / 4);

      int[] dst = new int[4096];
      int[] src = XaeroMapData.buildChunkPixels(w.c(0, 0), w);
      XaeroMapData.blit(dst, 64, 16, 16, src);
      int filled = 0;

      for(int i = 1024; i < 2048; ++i) {
         if (dst[i] != 0) {
            ++filled;
         }
      }

      check("blit placed 256 px (" + filled + ")", filled == 256);
      int outside = 0;

      for(int i = 0; i < 1024; ++i) {
         if (dst[i] != 0) {
            ++outside;
         }
      }

      check("blit did not bleed (" + outside + ")", outside == 0);

      Method wr = XaeroTiles.class.getDeclaredMethod("writePng", new Class[]{File.class, Integer.TYPE, Integer.TYPE, int[].class});
      wr.setAccessible(true);
      Method rd = XaeroTiles.class.getDeclaredMethod("readPng", new Class[]{File.class, Integer.TYPE, Integer.TYPE});
      rd.setAccessible(true);
      File tileDir = new File(root, "tiles");
      tileDir.mkdirs();
      wr.invoke((Object)null, new Object[]{tileDir, Integer.valueOf(3), Integer.valueOf(-4), src});
      int[] back = (int[])rd.invoke((Object)null, new Object[]{tileDir, Integer.valueOf(3), Integer.valueOf(-4)});
      check("png roundtrip saved", back != null);
      if (back != null) {
         int mismatches = 0;

         for(int i = 0; i < 256; ++i) {
            if (back[i] != src[i]) {
               ++mismatches;
            }
         }

         check("png roundtrip identical (" + mismatches + " mismatch)", mismatches == 0);
         check("negative coord filename ok", (new File(tileDir, "3_-4.png")).isFile());
      }

      List before = XaeroWpts.all(w);
      int n0 = before.size();
      String nm = XaeroWpts.add(w, 10, 65, -20, "FF0000");
      check("waypoint added name [" + nm + "]", ("\u8def\u5f84\u70b9" + (n0 + 1)).equals(nm));
      File wf = new File(new File(new File(root, "xaero"), "XaeroTestWorld"), "waypoints.txt");
      check("waypoint file written", wf.isFile());
      XaeroWpts.loadFromFile(wf);
      check("reload count " + XaeroWpts.count() + " == " + (n0 + 1), XaeroWpts.count() == n0 + 1);
      XaeroWpts last = XaeroWpts.get(XaeroWpts.count() - 1);
      check("coords preserved", last.x == 10 && last.y == 65 && last.z == -20);
      check("color parsed 0xFFFF0000", last.color == -65536);
      check("remove works", XaeroWpts.remove(XaeroWpts.count() - 1));
      check("count restored", XaeroWpts.count() == n0);

      System.out.println(fails == 0 ? "== HEADLESS TEST: ALL PASS ==" : "== HEADLESS TEST: " + fails + " FAILURES ==");
      System.exit(fails == 0 ? 0 : 1);
   }

   static void check(String name, boolean ok) {
      System.out.println((ok ? "[OK]   " : "[FAIL] ") + name);
      if (!ok) {
         ++fails;
      }
   }
}
