import dev.liber.api.EnvType;
import dev.liber.api.Liber;
import dev.liber.api.event.Subscribe;
import dev.liber.api.event.lifecycle.TickEvent;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;

public final class XaeroKeys {
   private boolean prevM = false;
   private boolean prevB = false;
   private boolean prevO = false;
   private boolean prevK = false;
   private boolean prevC = false;
   private static Minecraft instance = null;

   @Subscribe
   public void onTick(TickEvent e) {
      if (e.getEnvironment() != EnvType.CLIENT || e.getPhase() != TickEvent.Phase.END) {
         return;
      }

      try {
         this.handleKeys();
      } catch (Throwable var3) {
         ;
      }
   }

   private void handleKeys() {
      Minecraft mc = mc();
      if (mc != null && mc.f != null && mc.h != null) {
         this.checkDeath(mc);
         if (mc.r != null) {
            this.prevM = false;
            this.prevB = false;
            this.prevO = false;
            this.prevK = false;
            this.prevC = false;
            return;
         }

         boolean mNow = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_M);
         boolean bNow = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_B);
         boolean oNow = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_O);
         boolean kNow = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_K);
         boolean cNow = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_C);
         if (mNow && !this.prevM) {
            this.openWorldMap(mc);
         }

         if (bNow && !this.prevB) {
            this.quickWaypoint(mc);
         }

         if (oNow && !this.prevO) {
            this.openSettings(mc);
         }

         if (kNow && !this.prevK) {
            this.openWaypointManager(mc, -1, -1, -1);
         }

         if (cNow && !this.prevC) {
            XaeroCfg.caveMode = !XaeroCfg.caveMode;
            XaeroHud.forceResample();
            chat(mc, "\u00a77[Xaero] \u6d1e\u7a74\u6a21\u5f0f: " + (XaeroCfg.caveMode ? "\u5f00" : "\u5173"));
         }

         this.prevM = mNow;
         this.prevB = bNow;
         this.prevO = oNow;
         this.prevK = kNow;
         this.prevC = cNow;
      }
   }

   private boolean wasDead = false;

   private void checkDeath(Minecraft mc) {
      boolean deadNow = mc.r instanceof ch;
      if (deadNow && !this.wasDead) {
         int x = in.b(mc.h.aM);
         int y = (int)mc.h.aN;
         int z = in.b(mc.h.aO);
         String nm = XaeroWpts.addNamed(mc.f, x, y, z, "FF3B30", "\u6b7b\u4ea1\u70b9");
         chat(mc, "\u00a7c[Xaero] \u6b7b\u4ea1\u70b9\u5df2\u8bb0\u5f55: " + nm + " (" + x + ", " + y + ", " + z + ")");
      }

      this.wasDead = deadNow;
   }

   private void openSettings(Minecraft mc) {
      try {
         XaeroCfg.ensureLoaded();
         mc.a((da)(new XaeroSettingsScreen()));
      } catch (Throwable var3) {
         chat(mc, "\u00a7e[Xaero] \u6253\u5f00\u8bbe\u7f6e\u5931\u8d25");
      }
   }

   static void openWaypointManager(Minecraft mc, int px, int py, int pz) {
      try {
         XaeroWptScreen s = new XaeroWptScreen(px, py, pz);
         mc.a((da)s);
      } catch (Throwable var5) {
         chat(mc, "\u00a7e[Xaero] \u6253\u5f00\u8def\u5f84\u70b9\u7ba1\u7406\u5931\u8d25");
      }
   }

   private void openWorldMap(Minecraft mc) {
      if (Liber.isModLoaded("xaero-world-map")) {
         try {
            mc.a((da)(new XaeroMapScreen()));
         } catch (NoClassDefFoundError var3) {
            chat(mc, "\u00a7e[Xaero] \u52a0\u8f7d\u4e16\u754c\u5730\u56fe\u5931\u8d25: " + var3.getMessage());
         } catch (Throwable var4) {
            chat(mc, "\u00a7e[Xaero] \u6253\u5f00\u4e16\u754c\u5730\u56fe\u5931\u8d25");
         }
      } else {
         chat(mc, "\u00a7e[Xaero] \u672a\u5b89\u88c5 xaero-world-map");
      }
   }

   private void quickWaypoint(Minecraft mc) {
      int x = in.b(mc.h.aM);
      int y = (int)mc.h.aN;
      int z = in.b(mc.h.aO);
      String color = XaeroWptScreen.nextColorHex();
      String name = XaeroWpts.add(mc.f, x, y, z, color);
      if (name != null) {
         chat(mc, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + name + " (" + x + ", " + y + ", " + z + ") \u5df2\u6dfb\u52a0");
      }
   }

   public static Minecraft mc() {
      if (instance == null) {
         try {
            Field f = Minecraft.class.getDeclaredField("a");
            f.setAccessible(true);
            instance = (Minecraft)f.get((Object)null);
         } catch (Throwable var1) {
            return null;
         }
      }

      return instance;
   }

   static void chat(Minecraft mc, String msg) {
      try {
         mc.v.a(msg);
      } catch (Throwable var3) {
         System.out.println("[Xaero] " + msg);
      }
   }
}
