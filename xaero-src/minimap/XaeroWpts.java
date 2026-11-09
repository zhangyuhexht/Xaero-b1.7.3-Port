import dev.liber.api.Liber;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class XaeroWpts {
   public String name;
   public int x;
   public int y;
   public int z;
   public int color;

   private static List<XaeroWpts> cache = new ArrayList();
   private static String loadedKey = null;
   private static File loadedFile = null;
   private static boolean dirty = false;
   private static int counter = 1;
   private static final Charset UTF8 = Charset.forName("UTF-8");

   public XaeroWpts(String name, int x, int y, int z, int color) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
      this.color = color | 0xFF000000;
   }

   private static File file(Object world) {
      File dir = new File(new File(Liber.getGameDir().toFile(), "xaero"), XaeroWorlds.worldKeyOf(world));
      if (!dir.exists()) {
         dir.mkdirs();
      }

      return new File(dir, "waypoints.txt");
   }

   public static File dirFor(Object world) {
      File dir = new File(new File(Liber.getGameDir().toFile(), "xaero"), XaeroWorlds.worldKeyOf(world));
      if (!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   public static synchronized void ensure(Object world) {
      File f = file(world);
      if (!f.equals(loadedFile)) {
         loadedFile = f;
         loadedKey = XaeroWorlds.worldKeyOf(world);
         loadFromFile(f);
      }
   }

   static synchronized void loadFromFile(File f) {
      cache = new ArrayList();
      counter = 1;

      try {
         if (f.isFile()) {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF8));

            String line;
            try {
               while((line = r.readLine()) != null) {
                  line = line.trim();
                  if (line.length() != 0 && !line.startsWith("#")) {
                     String[] p = split(line, ';');
                     if (p.length >= 5) {
                        cache.add(new XaeroWpts(p[0], parseInt(p[1]), parseInt(p[2]), parseInt(p[3]), parseHex(p[4])));
                        String nm = p[0].replace("\u8def\u5f84\u70b9", "").trim();

                        try {
                           int n = Integer.parseInt(nm);
                           if (n >= counter) {
                              counter = n + 1;
                           }
                        } catch (NumberFormatException var8) {
                           ;
                        }
                     }
                  }
               }
            } finally {
               r.close();
            }
         }
      } catch (Throwable var9) {
         System.out.println("[Xaero] failed to load waypoints");
         var9.printStackTrace();
      }
   }

   public static synchronized List<XaeroWpts> all(Object world) {
      ensure(world);
      return new ArrayList(cache);
   }

   /* \u6bcf\u5e27\u7ed8\u5236\u7528\uff1a\u76f4\u63a5\u8fd4\u56de\u5185\u90e8\u7f13\u5b58\uff0c\u52ff\u4fee\u6539 */
   public static synchronized List<XaeroWpts> allDirect(Object world) {
      ensure(world);
      return cache;
   }

   public static synchronized int count() {
      return cache.size();
   }

   public static synchronized XaeroWpts get(int i) {
      return i >= 0 && i < cache.size() ? (XaeroWpts)cache.get(i) : null;
   }

   public static synchronized String add(Object world, int x, int y, int z, String hexColor) {
      ensure(world);
      XaeroWpts wp = new XaeroWpts("\u8def\u5f84\u70b9" + counter, x, y, z, parseHex(hexColor));
      ++counter;
      cache.add(wp);
      save(world);
      return wp.name;
   }

   public static synchronized String addNamed(Object world, int x, int y, int z, String hexColor, String name) {
      ensure(world);
      XaeroWpts wp = new XaeroWpts(name != null && name.length() != 0 ? name : "\u8def\u5f84\u70b9" + counter, x, y, z, parseHex(hexColor));
      ++counter;
      cache.add(wp);
      save(world);
      return wp.name;
   }

   public static synchronized void addDirect(int x, int y, int z, int color, String name) {
      XaeroWpts wp = new XaeroWpts(name != null && name.length() != 0 ? name : "\u8def\u5f84\u70b9" + counter, x, y, z, color);
      ++counter;
      cache.add(wp);
      dirty = true;
   }

   public static synchronized boolean remove(int index) {
      if (index >= 0 && index < cache.size()) {
         cache.remove(index);
         dirty = true;
         return true;
      } else {
         return false;
      }
   }

   public static synchronized void cycleColor(int index) {
      XaeroWpts wp = get(index);
      if (wp != null) {
         int[] palette = new int[]{-12525424, -13447886, -16729153, -13312, -5388114, -6291466};
         int best = 0;

         for(int i = 0; i < palette.length; ++i) {
            if (palette[i] == wp.color) {
               best = i + 1;
               break;
            }
         }

         wp.color = palette[best % palette.length] | 0xFF000000;
         dirty = true;
      }
   }

   public static synchronized void save(Object world) {
      dirty = true;

      try {
         BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file(world)), UTF8));

         try {
            writeAll(w);
         } finally {
            w.close();
         }

         dirty = false;
      } catch (Throwable var6) {
         System.out.println("[Xaero] failed to save waypoints");
         var6.printStackTrace();
      }
   }

   public static synchronized void flushIfDirty(Object world) {
      if (dirty) {
         save(world);
      }
   }

   private static void writeAll(BufferedWriter w) throws java.io.IOException {
      w.write("# \u683c\u5f0f: \u540d\u79f0;x;y;z;RRGGBB");
      w.newLine();
      w.write("# \u53ef\u624b\u52a8\u7f16\u8f91\uff0c\u6e38\u620f\u4e2d K \u952e\u7ba1\u7406");
      w.newLine();

      for(int i = 0; i < cache.size(); ++i) {
         XaeroWpts p = (XaeroWpts)cache.get(i);
         w.write(p.name + ";" + p.x + ";" + p.y + ";" + p.z + ";" + String.format("%06X", Integer.valueOf(p.color & 0xFFFFFF)));
         w.newLine();
      }
   }

   static String[] split(String s, char sep) {
      ArrayList parts = new ArrayList();
      int start = 0;

      for(int i = 0; i < s.length(); ++i) {
         if (s.charAt(i) == sep) {
            parts.add(s.substring(start, i));
            start = i + 1;
         }
      }

      parts.add(s.substring(start));
      return (String[])parts.toArray(new String[parts.size()]);
   }

   static int parseInt(String s) {
      try {
         return Integer.parseInt(s.trim());
      } catch (Throwable var2) {
         return 0;
      }
   }

   static int parseHex(String s) {
      try {
         return (int)(Long.parseLong(s.trim(), 16) & 16777215L) | -16777216;
      } catch (Throwable var2) {
         return -1;
      }
   }
}

