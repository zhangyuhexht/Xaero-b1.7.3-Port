import dev.liber.api.ClientModInitializer;
import dev.liber.api.EnvType;
import dev.liber.api.ModInitializer;
import dev.liber.api.event.LiberEvents;
import dev.liber.api.event.Subscribe;
import dev.liber.api.event.lifecycle.TickEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import net.minecraft.client.Minecraft;

public final class XaeroAutoTestMod implements ModInitializer, ClientModInitializer {
   public void onInitialize() {
   }

   public void onInitializeClient() {
      LiberEvents.EVENT_BUS.register(new XaeroAutoTestDriver());
      System.out.println("[XaeroAutoTest] driver armed");
   }
}

final class XaeroAutoTestDriver {
   private boolean joining = false;
   private int totalTicks = 0;
   private int worldTicks = 0;
   private int state = 0;
   private Object screen = null;
   private boolean done = false;
   private String wpName = null;

   @Subscribe
   public void onTick(TickEvent e) {
      if (e.getEnvironment() == EnvType.CLIENT && e.getPhase() == TickEvent.Phase.END && !this.done) {
         try {
            this.step();
         } catch (Throwable var3) {
            this.fail(var3);
         }
      }
   }

   private void step() throws Exception {
      Minecraft mc = XaeroKeys.mc();
      if (mc != null) {
         ++this.totalTicks;
         if (this.totalTicks > 8000) {
            this.fail(new RuntimeException("global timeout at state " + this.state));
         } else {
            if (!this.joining) {
               this.joining = true;
               if (mc.c == null) {
                  mc.c = new os(mc);
                  System.out.println("[XaeroAutoTest] player controller installed");
               }

               System.out.println("[XaeroAutoTest] requesting world creation");
               mc.a("XaeroAutoTest", "XaeroAutoTest", 987654321L);
               mc.a((da)null);
               System.out.println("[XaeroAutoTest] world creation returned; f=" + (mc.f != null));
            }

            if (mc.f != null && mc.h != null) {
               ++this.worldTicks;
               if (this.worldTicks > 1200) {
                  this.fail(new RuntimeException("world timeout at state " + this.state));
               } else if (this.state == 0) {
                  if (mc.r instanceof ch) {
                     mc.h.p_();
                     mc.a((da)null);
                     System.out.println("[XaeroAutoTest] player died at spawn, auto-respawned");
                  }

                  if (this.worldTicks % 100 == 0) {
                     System.out.println("[dbg] f=" + (mc.f != null) + " h=" + (mc.h != null) + " wFlag=" + mc.w
                        + " r=" + mc.r + " p=" + (mc.p != null) + " q=" + (mc.q != null)
                        + " t=" + (mc.t != null) + " hudAlive=" + XaeroHud.isAlive());
                  }

                  if (this.worldTicks > 100) {
                     if (!XaeroHud.isAlive()) {
                        if (this.worldTicks % 100 == 0) {
                           System.out.println("[XaeroAutoTest] waiting for hud, tick " + this.worldTicks);
                        }
                     } else {
                        System.out.println("[XaeroAutoTest] HUD alive after " + this.worldTicks + " ticks");
                        if (this.worldTicks > 160) {
                           this.snap(mc, "a-hud-minimap");
                           int px = in.b(mc.h.aM);
                           int py = (int)mc.h.aN;
                           int pz = in.b(mc.h.aO);
                           this.wpName = XaeroWpts.add(mc.f, px, py, pz, "FF3B30");
                           File wf = new File(new File(new File(dev.liber.api.Liber.getGameDir().toFile(), "xaero"), XaeroWorlds.worldKeyOf(mc.f)), "waypoints.txt");
                           this.check(wf.isFile(), "waypoint file created: " + wf);
                           this.screen = new XaeroMapScreen();
                           mc.a((da)this.screen);
                           System.out.println("[XaeroAutoTest] world map opened");
                           this.state = 1;
                        }
                     }
                  }
               } else if (this.state == 1) {
                  if (this.worldTicks >= 220) {
                     this.snap(mc, "b-worldmap");
                  }

                  if (this.worldTicks >= 240 && !this.tpTested) {
                     this.tpTested = true;
                     int tx = in.b(mc.h.aM) + 120;
                     int tz = in.b(mc.h.aO) - 90;
                     java.lang.reflect.Method m = this.screen.getClass().getDeclaredMethod("teleportTo", new Class[]{Integer.TYPE, Integer.TYPE});
                     m.setAccessible(true);
                     m.invoke(this.screen, new Object[]{Integer.valueOf(tx), Integer.valueOf(tz)});
                     this.tpCheckX = tx;
                     this.tpCheckZ = tz;
                     System.out.println("[XaeroAutoTest] map menu teleport invoked to " + tx + "," + tz);
                  }

                  if (this.worldTicks >= 260) {
                     if (this.tpTested) {
                        double dxp = Math.abs(mc.h.aM - (double)(this.tpCheckX + 1));
                        double dzp = Math.abs(mc.h.aO - (double)(this.tpCheckZ + 1));
                        this.check(dxp < 8.0D && dzp < 8.0D, "map TP landed player at " + in.b(mc.h.aM) + "," + in.b(mc.h.aO) + " (want " + this.tpCheckX + "," + this.tpCheckZ + ")");
                     }

                     Object tiles = reflect(this.screen, "tiles");
                     this.check(tiles != null, "tiles manager exists");
                     int texId = reflectInt(tiles, "texId");
                     this.check(texId >= 0, "atlas texture uploaded (texId=" + texId + ")");
                     Object atlas = reflect(tiles, "atlas");
                     this.check(atlas != null, "atlas image exists");
                     int aw = reflectInt(tiles, "awBlocks");
                     this.check(aw >= 256, "atlas span " + aw);
                     mc.a((da)null);
                     System.out.println("[XaeroAutoTest] world map closed");
                     XaeroCfg.circle = true;
                     XaeroHud.markAllDirty();
                     this.state = 3;
                  }
               } else if (this.state == 3) {
                  if (this.worldTicks >= 330) {
                     this.snap(mc, "c-circle-minimap");
                     mc.a((da)(new XaeroSettingsScreen()));
                     System.out.println("[XaeroAutoTest] settings screen opened (circle mode)");
                     this.state = 2;
                  }
               } else if (this.state == 2) {
                  if (this.worldTicks >= 400) {
                     java.io.File cfgFile = XaeroCfg.file();
                     XaeroCfg.save();
                     this.check(cfgFile.isFile(), "config file saved: " + cfgFile);
                     this.check(XaeroHud.isAlive(), "hud still alive after circle rebake");
                     XaeroCfg.enabled = false;
                     XaeroHud.markAllDirty();
                     mc.a((da)null);
                     System.out.println("[XaeroAutoTest] minimap disabled via cfg");
                     this.state = 4;
                  }
               } else if (this.state == 4) {
                  if (this.worldTicks >= 460) {
                     XaeroCfg.enabled = true;
                     XaeroHud.markAllDirty();
                     this.check(XaeroHud.isAlive(), "hud alive after enable toggle");
                     int bx = in.b(mc.h.aM) + 60;
                     int bz = in.b(mc.h.aO) + 60;
                     int ty = XaeroMapData.topAt(mc.f.c(bx >> 4, bz >> 4), bx & 15, bz & 15);
                     this.check(ty > 0, "surface found at " + bx + "," + bz + " y=" + ty);
                     this.tp(mc, (double)bx + 0.5D, (double)ty + 2.0D, (double)bz + 0.5D, 0.0F);
                     System.out.println("[XaeroAutoTest] teleported to surface spot");
                     this.state = 5;
                  }
               } else if (this.state == 5) {
                  if (this.worldTicks >= 520) {
                     int bx = in.b(mc.h.aM) + 60;
                     int bz = in.b(mc.h.aO) + 60;
                     int ty = XaeroMapData.topAt(mc.f.c(bx >> 4, bz >> 4), bx & 15, bz & 15);
                     this.tp(mc, (double)bx + 0.5D, (double)ty + 2.0D, (double)bz + 0.5D, 0.0F);
                     this.tpX = (double)bx + 0.5D;
                     this.tpY = (double)ty + 2.0D;
                     this.tpZ = (double)bz + 0.5D;
                     System.out.println("[XaeroAutoTest] teleported to surface spot");
                     this.yawIdx = 0;
                     this.state = 6;
                  }
               } else if (this.state == 6) {
                  if (this.worldTicks >= 520 + this.yawIdx * 30 + 20) {
                     this.snap(mc, "d-yaw" + this.yawIdx * 90);
                     if (this.yawIdx >= 3) {
                        long[] cave = this.findCave(mc, in.b(mc.h.aM) + 8, in.b(mc.h.aO) + 8);
                        this.check(cave[2] > 0L, "cave pocket found at " + cave[0] + "," + cave[1] + " floorY=" + cave[2]);
                        this.tp(mc, (double)cave[0] + 0.5D, (double)cave[2] + 1.2D, (double)cave[1] + 0.5D, 45.0F);
                        XaeroCfg.caveMode = true;
                        XaeroHud.forceResample();
                        System.out.println("[XaeroAutoTest] teleported into cave, cave mode on");
                        this.state = 8;
                     } else {
                        this.tp(mc, this.tpX, this.tpY, this.tpZ, (float)((this.yawIdx + 1) * 90));
                        System.out.println("[XaeroAutoTest] yaw set to " + (this.yawIdx + 1) * 90);
                        ++this.yawIdx;
                     }
                  }
               } else if (this.state == 8) {
                  if (this.worldTicks >= 700) {
                     this.snap(mc, "e-tp-cave");
                     XaeroCfg.caveMode = false;
                     XaeroHud.forceResample();
                     this.check(mc.h != null && mc.f != null, "world intact after tp tour");
                     this.pass();
                  }
               }
            }
         }
      }
   }

   private void tp(Minecraft mc, double x, double y, double z, float yaw) {
      mc.h.c(x, y, z, yaw, 0.0F);
      mc.h.aP = 0.0D;
      mc.h.aQ = 0.0D;
      mc.h.aR = 0.0D;
   }

   private double tpX;
   private double tpY;
   private double tpZ;
   private int yawIdx = 0;
   private int yawWait = 0;
   private boolean tpTested = false;
   private int tpCheckX = 0;
   private int tpCheckZ = 0;

   private long[] findCave(Minecraft mc, int bx, int bz) {
      fd w = mc.f;

      for(int x = bx; x < bx + 80; ++x) {
         for(int z = bz; z < bz + 80; ++z) {
            for(int y = 45; y >= 14; --y) {
               if (w.a(x, y, z) == 0 && w.a(x, y + 1, z) == 0 && w.a(x, y - 1, z) != 0 && w.a(x + 1, y, z) == 0 && w.a(x - 1, y, z) == 0) {
                  return new long[]{(long)x, (long)z, (long)(y - 1)};
               }
            }
         }
      }

      return new long[]{(long)bx, (long)bz, -1L};
   }

   private void snap(Minecraft mc, String tag) {
      try {
         File dir = dev.liber.api.Liber.getGameDir().toFile();
         String name = hj.a(dir, mc.d, mc.e);
         System.out.println("[XaeroAutoTest] screenshot " + tag + " -> " + name);
      } catch (Throwable var4) {
         System.out.println("[XaeroAutoTest] screenshot failed: " + var4);
      }
   }

   private static int reflectInt(Object o, String f) throws Exception {
      Class k = o.getClass();

      while(k != null && k != Object.class) {
         try {
            java.lang.reflect.Field fd = k.getDeclaredField(f);
            fd.setAccessible(true);
            return ((Number)fd.get(o)).intValue();
         } catch (NoSuchFieldException var4) {
            k = k.getSuperclass();
         }
      }

      throw new NoSuchFieldException(f);
   }

   private static Object reflect(Object o, String f) throws Exception {
      Class k = o.getClass();

      while(k != null && k != Object.class) {
         try {
            java.lang.reflect.Field fd = k.getDeclaredField(f);
            fd.setAccessible(true);
            return fd.get(o);
         } catch (NoSuchFieldException var4) {
            k = k.getSuperclass();
         }
      }

      throw new NoSuchFieldException(f);
   }

   private void check(boolean ok, String what) {
      System.out.println((ok ? "[OK]   " : "[FAIL] ") + what);
      if (!ok) {
         throw new RuntimeException("check failed: " + what);
      }
   }

   private void pass() {
      this.done = true;
      writeResult("PASS", "all checks ok; waypoint=" + this.wpName);
      System.out.println("== E2E TEST PASS ==");
      System.exit(0);
   }

   private void fail(Throwable t) {
      if (!this.done) {
         this.done = true;
         System.out.println("== E2E TEST FAIL ==");
         t.printStackTrace();
         writeResult("FAIL", String.valueOf(t));
         System.exit(1);
      }
   }

   private static void writeResult(String status, String detail) {
      try {
         String p = System.getProperty("xaero.test.out");
         PrintStream ps = new PrintStream(new FileOutputStream(p != null ? p : "xaero-test-result.txt"));
         ps.println(status);
         ps.println(detail);
         ps.close();
      } catch (Throwable var4) {
         var4.printStackTrace();
      }
   }
}
