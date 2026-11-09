import dev.liber.api.Liber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class XaeroCfg {
   public static boolean enabled = true;
   public static int size = 96;
   public static boolean circle = false;
   public static boolean showEntities = true;
   public static boolean showPlayers = true;
   public static boolean showWaypoints = true;
   public static boolean showCoords = true;
   public static boolean showBiome = true;
   public static boolean caveMode = false;
   private static boolean loaded = false;

   private XaeroCfg() {
   }

   public static File file() {
      File dir = new File(Liber.getGameDir().toFile(), "xaero");
      if (!dir.exists()) {
         dir.mkdirs();
      }

      return new File(dir, "minimap.cfg");
   }

   public static void load() {
      try {
         Properties p = new Properties();
         FileInputStream in = new FileInputStream(file());

         try {
            p.load(in);
         } finally {
            in.close();
         }

         enabled = !"false".equalsIgnoreCase(p.getProperty("enabled", "true").trim());
         size = clamp(parseInt(p.getProperty("size", "96"), 96, 512), 32, 512);
         circle = p.getProperty("shape", "square").trim().equalsIgnoreCase("circle");
         showEntities = !"false".equalsIgnoreCase(p.getProperty("entities", "true").trim());
         showPlayers = !"false".equalsIgnoreCase(p.getProperty("players", "true").trim());
         showWaypoints = !"false".equalsIgnoreCase(p.getProperty("waypoints", "true").trim());
         showCoords = !"false".equalsIgnoreCase(p.getProperty("coords", "true").trim());
         showBiome = !"false".equalsIgnoreCase(p.getProperty("biome", "true").trim());
         caveMode = "true".equalsIgnoreCase(p.getProperty("cave", "false").trim());
      } catch (Throwable var5) {
         ;
      }

      loaded = true;
   }

   public static void ensureLoaded() {
      if (!loaded) {
         load();
      }
   }

   public static void save() {
      try {
         Properties p = new Properties();
         p.setProperty("enabled", Boolean.toString(enabled));
         p.setProperty("size", String.valueOf(size));
         p.setProperty("shape", circle ? "circle" : "square");
         p.setProperty("entities", Boolean.toString(showEntities));
         p.setProperty("players", Boolean.toString(showPlayers));
         p.setProperty("waypoints", Boolean.toString(showWaypoints));
         p.setProperty("coords", Boolean.toString(showCoords));
         p.setProperty("biome", Boolean.toString(showBiome));
         p.setProperty("cave", Boolean.toString(caveMode));
         FileOutputStream out = new FileOutputStream(file());

         try {
            p.store(out, "Xaero Minimap settings");
         } finally {
            out.close();
         }
      } catch (Throwable var5) {
         System.out.println("[Xaero] failed to save config");
      }
   }

   public static int clamp(int v, int min, int max) {
      return v < min ? min : (v > max ? max : v);
   }

   static int parseInt(String s, int def, int max) {
      try {
         return Integer.parseInt(s.trim());
      } catch (Throwable var4) {
         return def;
      }
   }
}
