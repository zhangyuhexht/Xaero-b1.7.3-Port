import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;

public final class XaeroColors {
   private static int[] GRASS = null;
   private static int[] FOLIAGE = null;
   private static int[] WATER = null;
   private static Object lastPack = null;
   private static final int[] TABLE = new int[256];

   private XaeroColors() {
   }

   public static void ensureColorizers(Minecraft mc) {
      Object pack = null;
      try {
         pack = mc.D.a;
      } catch (Throwable var5) {
         ;
      }

      if (GRASS == null || lastPack != pack) {
         lastPack = pack;
         GRASS = loadColorizer(mc, "/misc/grasscolor.png");
         FOLIAGE = loadColorizer(mc, "/misc/foliagecolor.png");
         WATER = loadColorizer(mc, "/misc/watercolor.png");
         System.out.println("[Xaero] colorizers loaded: grass=" + (GRASS != null) + " foliage=" + (FOLIAGE != null) + " water=" + (WATER != null));
      }
   }

   private static int[] loadColorizer(Minecraft mc, String path) {
      InputStream in = null;
      if (mc != null && mc.D != null && mc.D.a != null) {
         try {
            in = mc.D.a.a(path);
         } catch (Throwable var6) {
            ;
         }
      }

      if (in == null) {
         try {
            in = Minecraft.class.getResourceAsStream(path);
         } catch (Throwable var7) {
            ;
         }
      }

      if (in == null) {
         return null;
      } else {
         try {
            BufferedImage im = ImageIO.read(in);
            in.close();
            if (im != null && im.getWidth() == 256 && im.getHeight() == 256) {
               int[] px = new int[65536];
               im.getRGB(0, 0, 256, 256, px, 0, 256);

               for(int i = 0; i < px.length; ++i) {
                  px[i] = (px[i] & 16777215) | -16777216;
               }

               return px;
            }
         } catch (Throwable var8) {
            ;
         }

         return null;
      }
   }

   private static int lookup(int[] table, fd w, int bx, int bz, int fallback) {
      try {
         xv mgr = w.a();
         mgr.a(bx, bz, 1, 1);
         double t = mgr.a[0];
         double h = mgr.b[0];
         return lookupAt(table, t, h, w, bx, bz, fallback);
      } catch (Throwable var10) {
         return fallback;
      }
   }

   private static int lookupAt(int[] table, double t, double h, fd w, int bx, int bz, int fallback) {
      try {
         if (table != null) {
            h *= t;
            int col = (int)((1.0D - t) * 255.0D);
            int row = (int)((1.0D - h) * 255.0D);
            return table[(row << 8 | col) & 65535];
         }

         kd b = w.a().a(bx, bz);
         if (b != null) {
            return b.o & 16777215 | -16777216;
         }
      } catch (Throwable var10) {
         ;
      }

      return fallback;
   }

   public static boolean hasGrassColorizer() {
      return GRASS != null;
   }

   public static int grassTintAt(double t, double h) {
      return lookupAt(GRASS, t, h, (fd)null, 0, 0, -14503604);
   }

   public static int foliageTintAt(double t, double h) {
      return lookupAt(FOLIAGE, t, h, (fd)null, 0, 0, -14503604);
   }

   public static int waterTintAt(double t, double h) {
      int c = lookupAt(WATER, t, h, (fd)null, 0, 0, -1);
      return c == -1 ? -15066598 : c;
   }

   public static float detailNoise(int wx, int wz) {
      int n = wx * -1640531527 + wz * 1013904223 + (wx ^ wz << 7);
      n ^= n >> 13;
      n *= 1274126177;
      n ^= n >> 16;
      return 1.0F + (float)((n & 15) - 7) * 0.009F;
   }

   public static int grassTint(fd w, int bx, int bz) {
      return lookup(GRASS, w, bx, bz, -14503604);
   }

   public static int foliageTint(fd w, int bx, int bz) {
      return lookup(FOLIAGE, w, bx, bz, -14503604);
   }

   public static int waterTint(fd w, int bx, int bz) {
      int c = lookup(WATER, w, bx, bz, 0);
      return c == -1 ? -15066598 : c;
   }

   public static int biomeColor(fd w, int bx, int bz) {
      try {
         kd b = w.a().a(bx, bz);
         if (b != null) {
            return (b.o & 16777215) | -16777216;
         }
      } catch (Throwable var4) {
         ;
      }

      return -1;
   }

   public static String biomeName(fd w, int bx, int bz) {
      try {
         kd b = w.a().a(bx, bz);
         return b == null ? "?" : b.n;
      } catch (Throwable var4) {
         return "?";
      }
   }

   public static int colorFor(fd w, int id, int bx, int bz) {
      int gT = grassTint(w, bx, bz);
      int fT = foliageTint(w, bx, bz);
      int bio = biomeColor(w, bx, bz);
      return colorForTinted(id, gT, fT, bio);
   }

   public static int colorForTinted(int id, int gTint, int fTint, int bioColor) {
      switch(id) {
      case 2:
         return gTint;
      case 6:
         return mul(gTint, 0.85F);
      case 18:
         return mul(fTint, 0.95F);
      case 31:
         return mul(gTint, 0.8F);
      default: {
         int base = TABLE[id];
         if (base != 0) {
            return base;
         } else if (id > 0 && id < 256) {
            return mix(bioColor, -12566464, 0.35F);
         } else {
            return -1;
         }
      }
      }
   }

   public static int waterColorTinted(int wTint) {
      return mix(0xFF2E52CE, (wTint & 16777215) | -16777216, 0.45F);
   }

   public static int waterColor(fd w, int bx, int bz) {
      return waterColorTinted(waterTint(w, bx, bz));
   }

   public static int mul(int argb, float f) {
      int a = argb >>> 24 & 255;
      int r = clamp((float)(argb >>> 16 & 255) * f);
      int g = clamp((float)(argb >>> 8 & 255) * f);
      int b = clamp((float)(argb & 255) * f);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int shade(int argb, float f) {
      return mul(argb, f);
   }

   public static int mix(int c1, int c2, float k) {
      int r = (int)((float)(c1 >>> 16 & 255) * k + (float)(c2 >>> 16 & 255) * (1.0F - k));
      int g = (int)((float)(c1 >>> 8 & 255) * k + (float)(c2 >>> 8 & 255) * (1.0F - k));
      int b = (int)((float)(c1 & 255) * k + (float)(c2 & 255) * (1.0F - k));
      return -16777216 | r << 16 | g << 8 | b;
   }

   private static int clamp(float v) {
      return v < 0.0F?0:(v > 255.0F?255:(int)v);
   }

   public static int rgb(String hex) {
      return (int)(Long.parseLong(hex.trim(), 16) & 16777215L) | -16777216;
   }

   static void set(int id, int rgb) {
      if (id >= 0 && id < 256) {
         TABLE[id] = rgb | -16777216;
      }
   }

   static {
      set(1, 0x7F7F7F);
      set(3, 0x866043);
      set(4, 0x828282);
      set(5, 0x9C7F4E);
      set(7, 0x565656);
      set(10, 0xD45A12);
      set(11, 0xD45A12);
      set(12, 0xD6CC96);
      set(13, 0x84888A);
      set(14, 0x8C7D53);
      set(15, 0x84878E);
      set(16, 0x606060);
      set(17, 0x685133);
      set(19, 0xC3C34B);
      set(20, 0xB8D2DA);
      set(21, 0x596199);
      set(22, 0x1954A2);
      set(23, 0x757575);
      set(24, 0xD8D0A4);
      set(25, 0x646464);
      set(26, 0xB03030);
      set(27, 0x777777);
      set(28, 0x6E6E6E);
      set(29, 0x7C836B);
      set(30, 0xE9E9E9);
      set(32, 0x966C4A);
      set(33, 0x827A66);
      set(34, 0x9C9061);
      set(35, 0xE8E8E8);
      set(37, 0xEDDD30);
      set(38, 0xD0342B);
      set(39, 0x9C7F72);
      set(40, 0xD31818);
      set(41, 0xF8EC4E);
      set(42, 0xE6E6E6);
      set(43, 0x9C9C94);
      set(44, 0x9C9C94);
      set(45, 0x985542);
      set(46, 0xDC4433);
      set(47, 0x836C46);
      set(48, 0x707C68);
      set(49, 0x150C22);
      set(50, 0xFFCC66);
      set(51, 0xE07820);
      set(52, 0x2A3441);
      set(53, 0x9C7F4E);
      set(54, 0x9A7938);
      set(55, 0x8B0000);
      set(56, 0x6FA8A8);
      set(57, 0x64DCD2);
      set(58, 0x79553A);
      set(59, 0xB5A83A);
      set(60, 0x5F3F21);
      set(61, 0x767676);
      set(62, 0x767676);
      set(63, 0x9C7F4E);
      set(64, 0x9C7F4E);
      set(65, 0x9C7F4E);
      set(66, 0x777777);
      set(67, 0x7F7F7F);
      set(68, 0x9C7F4E);
      set(69, 0x777777);
      set(70, 0x7F7F7F);
      set(71, 0xC0C0C0);
      set(72, 0x9C7F4E);
      set(73, 0x7D5050);
      set(74, 0x7D5050);
      set(75, 0x6B0000);
      set(76, 0xFF3300);
      set(77, 0x7F7F7F);
      set(78, 0xF0FBFB);
      set(79, 0x7DAFF7);
      set(80, 0xF0FBFB);
      set(81, 0x35793B);
      set(82, 0xA4A8B8);
      set(83, 0x93C993);
      set(84, 0x575757);
      set(85, 0x9C7F4E);
      set(86, 0xC07615);
      set(87, 0x7A3030);
      set(88, 0x584A3F);
      set(89, 0xFFDA80);
      set(90, 0x721E8E);
      set(91, 0xDD9523);
      set(92, 0xF0EDE5);
      set(93, 0x9C7F4E);
      set(94, 0x9C7F4E);
      set(95, 0x9A7938);
   }
}
