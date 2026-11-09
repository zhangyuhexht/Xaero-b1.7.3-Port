import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import net.minecraft.client.Minecraft;

public final class XaeroWorlds {
   private static String cachedKey = null;
   private static Object cachedWorld = null;
   public static String lastMethod = "unknown";

   private XaeroWorlds() {
   }

   public static String worldKey(Minecraft mc) {
      return mc != null && mc.f != null ? worldKeyOf(mc.f) : "none";
   }

   public static synchronized String worldKeyOf(Object world) {
      if (world == null) {
         return "none";
      } else if (cachedKey != null && cachedWorld == world) {
         return cachedKey;
      } else {
         String key = resolve(world);
         cachedKey = key;
         cachedWorld = world;
         return key;
      }
   }

   public static void invalidate(Object world) {
      if (cachedWorld != world) {
         cachedKey = null;
         cachedWorld = null;
      }
   }

   public static int dimension(Object world) {
      try {
         Object info = field(world, "x");
         Method m = info.getClass().getMethod("i");
         return ((Number)m.invoke(info)).intValue();
      } catch (Throwable var3) {
         return 0;
      }
   }

   private static String resolve(Object world) {
      String base = findSaveName(world);
      lastMethod = base != null ? "save-folder" : "seed";
      if (base == null) {
         long seed = 0L;

         try {
            Method m = world.getClass().getMethod("s");
            seed = ((Number)m.invoke(world)).longValue();
         } catch (Throwable var5) {
            ;
         }

         base = "seed-" + Long.toHexString(seed);
      }

      int dim = dimension(world);
      return dim != 0 ? base + "_d" + dim : base;
   }

   private static String findSaveName(Object world) {
      try {
         Object saveHandler = field(world, "w");
         if (saveHandler == null) {
            return null;
         } else {
            IdentityHashMap seen = new IdentityHashMap();
            File f = findFile(saveHandler, seen, 0, 4);
            if (f == null) {
               return null;
            } else {
               File parent = f.getParentFile();
               File child = f;

               while(parent != null) {
                  if ("saves".equalsIgnoreCase(parent.getName())) {
                     return child.getName();
                  }

                  child = parent;
                  parent = parent.getParentFile();
               }

               return parent == null && child.getParentFile() != null ? sanitize(child.getName()) : null;
            }
         }
      } catch (Throwable var6) {
         return null;
      }
   }

   private static String sanitize(String s) {
      StringBuilder b = new StringBuilder();

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         b.append(Character.isLetterOrDigit(c) ? c : '_');
      }

      return b.length() == 0 ? "world" : b.toString();
   }

   private static Object field(Object obj, String name) throws IllegalAccessException {
      for(Class k = obj.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
         try {
            Field f = k.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
         } catch (NoSuchFieldException var5) {
            ;
         }
      }

      return null;
   }

   private static File findFile(Object obj, IdentityHashMap seen, int depth, int maxDepth) throws IllegalAccessException {
      if (obj != null && depth <= maxDepth && !obj.getClass().getName().startsWith("[")) {
         String cn = obj.getClass().getName();
         if (cn.startsWith("java.") || cn.startsWith("javax.") || cn.startsWith("sun.") || cn.startsWith("org.lwjgl")) {
            return null;
         } else if (seen.put(obj, Boolean.TRUE) != null) {
            return null;
         } else {
            Class c = obj.getClass();

            for(Class k = c; k != null && k != Object.class; k = k.getSuperclass()) {
               Field[] fs = k.getDeclaredFields();

               for(int i = 0; i < fs.length; ++i) {
                  Field f = fs[i];
                  if (f.getType() == File.class) {
                     f.setAccessible(true);
                     Object v = f.get(obj);
                     if (v instanceof File) {
                        return (File)v;
                     }
                  }
               }
            }

            for(Class k = c; k != null && k != Object.class; k = k.getSuperclass()) {
               Field[] fs = k.getDeclaredFields();

               for(int i = 0; i < fs.length; ++i) {
                  Field f = fs[i];
                  Class t = f.getType();
                  if (!t.isPrimitive() && !t.isArray() && !t.getName().startsWith("java.")) {
                     f.setAccessible(true);
                     Object v = f.get(obj);
                     File r = v != null ? findFile(v, seen, depth + 1, maxDepth) : null;
                     if (r != null) {
                        return r;
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }
}
