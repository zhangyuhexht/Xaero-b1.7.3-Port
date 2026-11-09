import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class XaeroMapScreen extends da {
   private static final double[] PPB = new double[]{0.125D, 0.25D, 0.5D, 1.0D, 2.0D, 4.0D};
   private int ppbIdx = 3;
   private double camX;
   private double camZ;
   private int infoX = Integer.MIN_VALUE;
   private int infoZ = Integer.MIN_VALUE;
   private String infoLine = "";
   private boolean dragging = false;
   private int lastMx;
   private int lastMy;
   private int curMx;
   private int curMy;
   private XaeroTiles tiles;
   private double animFrom = 2.0D;
   private double animTo = 2.0D;
   private double animT = 1.0D;
   private long zoomToastUntil = 0L;
   private boolean prevRMB = false;
   private long lastFrame = 0L;
   private boolean menuOpen = false;
   private int menuPX = 0;
   private int menuPY = 0;
   private int menuBX = 0;
   private int menuBZ = 0;

   private void openMenu(int mx, int my) {
      Minecraft mc = this.b;
      if (mc == null || mc.f == null) {
         return;
      }

      this.menuOpen = true;
      this.menuBX = (int)Math.floor(this.camX + (double)(mx - this.c / 2) / this.ppb());
      this.menuBZ = (int)Math.floor(this.camZ + (double)(my - this.d / 2) / this.ppb());
      int w = 120;
      int h = 66;
      int px = mx;
      int py = my;
      if (px + w > this.c - 4) {
         px = this.c - 4 - w;
      }

      if (py + h > this.d - 40) {
         py = this.d - 40 - h;
      }

      this.menuPX = px;
      this.menuPY = py;

      this.e.clear();
      this.e.add(new ke(10, px, py, w, 20, "\u4f20\u9001\u5230\u6b64\u5904"));
      this.e.add(new ke(11, px, py + 23, w, 20, "\u6dfb\u52a0\u8def\u5f84\u70b9"));
      this.e.add(new ke(12, px, py + 46, w, 20, "\u53d6\u6d88"));
   }

   private void closeMenu() {
      this.menuOpen = false;
      this.e.clear();
      this.e.add(new ke(0, this.c / 2 - 100, this.d - 26, 200, 20, "\u5b8c\u6210"));
   }

   void teleportTo(int bx, int bz) {
      Minecraft mc = this.b;
      if (mc != null && mc.h != null && mc.f != null) {
         int by = guessSurfaceY(mc, bx, bz);
         mc.h.c((double)bx + 0.5D, (double)by + 1.2D, (double)bz + 0.5D, mc.h.bj, mc.h.bi);
         mc.h.aP = 0.0D;
         mc.h.aQ = 0.0D;
         mc.h.aR = 0.0D;
         XaeroKeys.chat(mc, "\u00a7a[Xaero] \u5df2\u4f20\u9001\u5230 (" + bx + ", " + by + ", " + bz + ")");
      }
   }

   public XaeroMapScreen() {
   }

   private double ppb() {
      return this.animFrom + (this.animTo - this.animFrom) * this.animT;
   }

   @Override
   public void b() {
      Minecraft mc = this.b;
      if (mc != null && mc.f != null && mc.h != null) {
         this.camX = mc.h.aM;
         this.camZ = mc.h.aO;
         this.tiles = new XaeroTiles(mc);
      }

      if (this.menuOpen) {
         this.e.clear();
         this.e.add(new ke(10, this.menuPX, this.menuPY, 120, 20, "\u4f20\u9001\u5230\u6b64\u5904"));
         this.e.add(new ke(11, this.menuPX, this.menuPY + 23, 120, 20, "\u6dfb\u52a0\u8def\u5f84\u70b9"));
         this.e.add(new ke(12, this.menuPX, this.menuPY + 46, 120, 20, "\u53d6\u6d88"));
      } else {
         this.e.clear();
         this.e.add(new ke(0, this.c / 2 - 100, this.d - 26, 200, 20, "\u5b8c\u6210"));
      }
   }

   @Override
   public void a(int mx, int my, float pt) {
      Minecraft mc = this.b;
      if (mc != null && mc.f != null && mc.h != null && this.tiles != null) {
         fd w = mc.f;
         long now = System.currentTimeMillis();
         if (this.animT < 1.0D) {
            long dt = this.lastFrame == 0L ? 16L : Math.max(1L, now - this.lastFrame);
            this.lastFrame = now;
            this.animT = Math.min(1.0D, this.animT + (double)dt / 130.0D);
            if (this.animT >= 1.0D) {
               this.animFrom = this.animTo;
            }
         } else {
            this.lastFrame = now;
         }

         this.handleMouse(mx, my);
         double halfW = (double)this.c / 2.0D / this.ppb();
         double halfH = ((double)this.d - 40.0D) / 2.0D / this.ppb();

         try {
            this.tiles.ensure(w, this.camX - halfW - 16.0D, this.camZ - halfH - 16.0D, this.camX + halfW + 16.0D, this.camZ + halfH + 16.0D);
            this.tiles.refreshAround(w, mc.h.aM, mc.h.aO);
            this.tiles.rebuildIfNeeded(this.camX, this.camZ, halfW, halfH);
            this.tiles.flush(false);
         } catch (Throwable var18) {
            var18.printStackTrace();
         }

         this.i();
         GL11.glDisable(2884);
         this.drawAtlas(mc);
         this.drawChunkGrid(halfW, halfH);
         this.drawCursorHighlight(mx, my);
         this.drawWaypoints(mc);
         this.drawPlayer(mc, mx, my);
         this.drawInfo(mc, w, mx, my);
         GL11.glEnable(2884);
         super.a(mx, my, pt);
      }
   }

   private void handleMouse(int mx, int my) {
      try {
         this.curMx = mx;
         this.curMy = my;
         if (Mouse.isCreated()) {
            int dw = Mouse.getDWheel();
            if (dw != 0) {
               this.zoom(dw > 0 ? -1 : 1, mx, my);
               this.zoomToastUntil = System.currentTimeMillis() + 1200L;
            }

            boolean rmb = Mouse.isButtonDown(1);
            if (rmb && !this.prevRMB) {
               if (this.menuOpen) {
                  this.closeMenu();
               } else {
                  this.openMenu(mx, my);
               }
            }

            this.prevRMB = rmb;
            if (Mouse.isButtonDown(0)) {
               if (this.menuOpen) {
                  if (!this.overMenu(mx, my)) {
                     this.closeMenu();
                  }
               } else {
                  if (this.dragging) {
                     this.camX -= (double)(mx - this.lastMx) / this.ppb();
                     this.camZ -= (double)(my - this.lastMy) / this.ppb();
                  }

                  this.dragging = true;
                  this.lastMx = mx;
                  this.lastMy = my;
               }
            } else {
               this.dragging = false;
            }
         }
      } catch (Throwable var4) {
         ;
      }
   }

   private boolean overMenu(int mx, int my) {
      for(int i = 0; i < this.e.size(); ++i) {
         ke bn = (ke)this.e.get(i);
         if (mx >= bn.c && mx <= bn.c + bn.a && my >= bn.d && my <= bn.d + bn.b) {
            return true;
         }
      }

      return false;
   }

   private void waypointAtCursor() {
      Minecraft mc = this.b;
      if (mc != null && mc.f != null) {
         int bx = (int)Math.floor(this.camX + (double)(this.curMx - this.c / 2) / this.ppb());
         int bz = (int)Math.floor(this.camZ + (double)(this.curMy - this.d / 2) / this.ppb());
         int by = guessSurfaceY(mc, bx, bz);
         String nm = XaeroWpts.add(mc.f, bx, by, bz, XaeroWptScreen.nextColorHex());
         XaeroKeys.chat(mc, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + nm + " (" + bx + ", " + by + ", " + bz + ") \u5df2\u6dfb\u52a0");
      }
   }

   private void zoom(int dir, int mx, int my) {
      double oldPpb = this.animTo;
      double bx = this.camX + (double)(mx - this.c / 2) / this.ppb();
      double bz = this.camZ + (double)(my - this.d / 2) / this.ppb();
      this.ppbIdx += dir;
      if (this.ppbIdx < 0) {
         this.ppbIdx = 0;
      }

      if (this.ppbIdx >= PPB.length) {
         this.ppbIdx = PPB.length - 1;
      }

      this.animFrom = oldPpb;
      this.animTo = PPB[this.ppbIdx];
      this.animT = 0.0D;
      double newPpb = this.animTo;
      this.camX = bx - (double)(mx - this.c / 2) / newPpb;
      this.camZ = bz - (double)(my - this.d / 2) / newPpb;
   }

   private void drawAtlas(Minecraft mc) {
      if (this.tiles.atlas != null && this.tiles.texId >= 0 && this.tiles.awBlocks > 0) {
         double sx0d = ((double)this.tiles.ax0 - this.camX) * this.ppb() + (double)this.c / 2.0D;
         double sy0d = ((double)this.tiles.az0 - this.camZ) * this.ppb() + (double)((double)this.d / 2.0D);
         double spanPx = (double)this.tiles.awBlocks * this.ppb();
         GL11.glEnable(3553);
         mc.p.b(this.tiles.texId);
         int filter = this.ppb() < 0.75D ? 9729 : 9728;
         GL11.glTexParameteri(3553, 10241, filter);
         GL11.glTexParameteri(3553, 10240, filter);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         float sx0 = (float)sx0d;
         float sy0 = (float)sy0d;
         float spanPx2 = (float)spanPx;
         GL11.glBegin(7);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex2f(sx0, sy0);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex2f(sx0, sy0 + spanPx2);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex2f(sx0 + spanPx2, sy0 + spanPx2);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex2f(sx0 + spanPx2, sy0);
         GL11.glEnd();
         GL11.glDisable(3553);
      }
   }

   private void drawChunkGrid(double halfW, double halfH) {
      if (this.ppbIdx >= 2) {
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(3553);
         GL11.glBegin(1);
         GL11.glColor4f(0.2F, 0.25F, 0.35F, 0.3F);

         for(double bx = Math.floor((this.camX - halfW) / 16.0D) * 16.0D; bx <= this.camX + halfW; bx += 16.0D) {
            float sx = worldToScreenX(bx);
            GL11.glVertex2f(sx, 0.0F);
            GL11.glVertex2f(sx, (float)this.d);
         }

         for(double bz = Math.floor((this.camZ - halfH) / 16.0D) * 16.0D; bz <= this.camZ + halfH; bz += 16.0D) {
            float sy = worldToScreenY(bz);
            GL11.glVertex2f(0.0F, sy);
            GL11.glVertex2f((float)this.c, sy);
         }

         GL11.glEnd();
         GL11.glEnable(3553);
         GL11.glDisable(3042);
      }
   }

   private float worldToScreenX(double wx) {
      return (float)((wx - this.camX) * this.ppb() + (double)this.c / 2.0D);
   }

   private float worldToScreenY(double wz) {
      return (float)((wz - this.camZ) * this.ppb() + (double)this.d / 2.0D);
   }

   private void drawCursorHighlight(int mx, int my) {
      if (this.ppbIdx >= 2 && my < this.d - 34) {
         double bx = Math.floor(this.camX + (double)(mx - this.c / 2) / this.ppb());
         double bz = Math.floor(this.camZ + (double)(my - this.d / 2) / this.ppb());
         float sx = worldToScreenX(bx);
         float sy = worldToScreenY(bz);
         float s = (float)this.ppb();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(3553);
         fillRectOutline(sx, sy, s, s, -2130706433);
         GL11.glEnable(3553);
         GL11.glDisable(3042);
      }
   }

   private void drawWaypoints(Minecraft mc) {
      java.util.List list = XaeroWpts.allDirect(mc.f);
      sj fnt = this.g;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);

      for(int i = 0; i < list.size(); ++i) {
         XaeroWpts p = (XaeroWpts)list.get(i);
         float sx = worldToScreenX((double)p.x + 0.5D);
         float sy = worldToScreenY((double)p.z + 0.5D);
         if (sx >= -60.0F && sx <= (float)this.c + 60.0F && sy >= -80.0F && sy <= (float)this.d - 30.0F) {
            beamUp(sx, sy, p.color);
            float r = Math.max(2.5F, (float)this.ppb() * 0.6F);
            fillRect(sx - r, sy - r, r * 2.0F, r * 2.0F, mixDark(p.color));
            fillRect(sx - r * 0.55F, sy - r * 0.55F, r * 1.1F, r * 1.1F, p.color);
            fnt.a(p.name, (int)(sx + r + 2.0F), (int)(sy - 4.0F), 16777215);
         }
      }

      GL11.glEnable(3553);
      GL11.glDisable(3042);
   }

   private static void beamUp(float x, float y, int color) {
      float r = (float)(color >>> 16 & 255) / 255.0F;
      float g = (float)(color >>> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      float top = y - Math.max(24.0F, y * 0.15F);

      for(int seg = 0; seg < 4; ++seg) {
         float a0 = 1.0F - (float)seg / 4.0F;
         float a1 = 1.0F - (float)(seg + 1) / 4.0F;
         float w0 = 1.6F * a0;
         float w1 = 1.6F * a1;
         float y0 = y + (top - y) * ((float)seg / 4.0F);
         float y1 = y + (top - y) * ((float)(seg + 1) / 4.0F);
         GL11.glBegin(7);
         GL11.glColor4f(r, g, b, a0 * 0.85F);
         GL11.glVertex2f(x - w0, y0);
         GL11.glColor4f(r, g, b, a1 * 0.85F);
         GL11.glVertex2f(x - w1, y1);
         GL11.glVertex2f(x + w1, y1);
         GL11.glColor4f(r, g, b, a0 * 0.85F);
         GL11.glVertex2f(x + w0, y0);
         GL11.glEnd();
      }
   }

   private void drawPlayer(Minecraft mc, int mx, int my) {
      dc p = mc.h;
      float px = (float)p.aM;
      float pz = (float)p.aO;
      float sx = worldToScreenX((double)px);
      float sy = worldToScreenY((double)pz);
      double yawRad = Math.toRadians((double)p.bj);
      float fx = -(float)Math.sin(yawRad);
      float fz = (float)Math.cos(yawRad);
      float perX = -fz;
      float perY = fx;
      float len = Math.max(7.0F, (float)this.ppb() * 3.0F);
      boolean offscreen = sx < 8.0F || sx > (float)this.c - 8.0F || sy < 8.0F || sy > (float)this.d - 42.0F;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      if (!offscreen) {
         GL11.glBegin(9);
         GL11.glColor4f(0.08F, 0.08F, 0.08F, 1.0F);
         GL11.glVertex2f(sx + fx * (len + 2.0F), sy + fz * (len + 2.0F));
         GL11.glVertex2f(sx - fx * len * 0.7F + perX * len * 0.75F, sy - fz * len * 0.7F + perY * len * 0.75F);
         GL11.glVertex2f(sx - fx * len * 0.7F - perX * len * 0.75F, sy - fz * len * 0.7F - perY * len * 0.75F);
         GL11.glEnd();
         GL11.glBegin(9);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glVertex2f(sx + fx * len, sy + fz * len);
         GL11.glVertex2f(sx - fx * len * 0.7F + perX * len * 0.6F, sy - fz * len * 0.7F + perY * len * 0.6F);
         GL11.glVertex2f(sx - fx * len * 0.7F - perX * len * 0.6F, sy - fz * len * 0.7F - perY * len * 0.6F);
         GL11.glEnd();
      } else {
         float ex = clampRange(sx, 12.0F, (float)this.c - 12.0F);
         float ey = clampRange(sy, 12.0F, (float)this.d - 46.0F);
         double ang = Math.atan2((double)(sy - ey), (double)(sx - ex));
         float ax = (float)Math.cos(ang);
         float ay = (float)Math.sin(ang);
         GL11.glBegin(9);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.95F);
         GL11.glVertex2f(ex + ax * 6.0F, ey + ay * 6.0F);
         GL11.glVertex2f(ex - ax * 3.0F + ay * 3.5F, ey - ay * 3.0F - ax * 3.5F);
         GL11.glVertex2f(ex - ax * 3.0F - ay * 3.5F, ey - ay * 3.0F + ax * 3.5F);
         GL11.glEnd();
      }

      GL11.glEnable(3553);
      GL11.glDisable(3042);
   }

   private static float clampRange(float v, float min, float max) {
      return v < min ? min : (v > max ? max : v);
   }

   private void drawInfo(Minecraft mc, fd w, int mx, int my) {
      sj fnt = this.g;
      int cbx = (int)Math.floor(this.camX + (double)(mx - this.c / 2) / this.ppb());
      int cbz = (int)Math.floor(this.camZ + (double)(my - this.d / 2) / this.ppb());
      String title = "\u00a7lXaero\u4e16\u754c\u5730\u56fe";
      fillRect(4.0F, 2.0F, (float)(fnt.a(title) + 6), 13.0F, -1073741824);
      fnt.a(title, 8, 6, 16777215);
      if (cbx != this.infoX || cbz != this.infoZ) {
         this.infoX = cbx;
         this.infoZ = cbz;
         this.infoLine = "X: " + cbx + "  Z: " + cbz + "  \u751f\u7269\u7fa4\u7cfb: " + XaeroColors.biomeName(w, cbx, cbz);
      }

      fillRect(4.0F, 15.0F, (float)(fnt.a(this.infoLine) + 6), 13.0F, -1073741824);
      fnt.a(this.infoLine, 8, 18, 12632256);
      drawScaleBar(fnt);
      String line3 = "\u62d6\u52a8\u79fb\u52a8 \u00b7 \u6eda\u8f6e\u7f29\u653e \u00b7 \u53f3\u952e\u83dc\u5355(\u4f20\u9001/\u8def\u5f84\u70b9) \u00b7 M/Esc \u5173\u95ed \u00b7 K \u7ba1\u7406";
      fnt.a(line3, 8, this.d - 44, 8947848);
      if (System.currentTimeMillis() < this.zoomToastUntil) {
         String zt = "\u7f29\u653e " + fmtNum(1.0D / this.ppb()) + ":1";
         fillRect((float)(this.c / 2 - fnt.a(zt) / 2 - 4), 34.0F, (float)(fnt.a(zt) + 8), 13.0F, -1073741824);
         fnt.a(zt, this.c / 2 - fnt.a(zt) / 2, 37, 15658596);
      }
   }

   private void drawScaleBar(sj fnt) {
      double[] candidates = new double[]{8.0D, 16.0D, 32.0D, 64.0D, 128.0D};
      double best = 8.0D;
      double bestdiff = Double.MAX_VALUE;

      for(int i = 0; i < candidates.length; ++i) {
         double px = candidates[i] * this.ppb();
         double diff = Math.abs(px - 80.0D);
         if (diff < bestdiff) {
            bestdiff = diff;
            best = candidates[i];
         }
      }

      float pxLen = (float)((double)best * this.ppb());
      int xEnd = this.c - 10;
      int y = this.d - 60;
      GL11.glDisable(3553);
      fillRect((float)(xEnd - (int)pxLen), (float)y, pxLen, 2.0F, -2130706433);
      fillRect((float)xEnd, (float)y, 1.0F, 5.0F, -2130706433);
      fillRect((float)(xEnd - (int)pxLen), (float)y, 1.0F, 5.0F, -2130706433);
      GL11.glEnable(3553);
      String label = fmtNum(best) + "\u683c";
      fnt.a(label, xEnd - (int)pxLen / 2 - fnt.a(label) / 2, y + 6, 15656320);
   }

   private static String fmtNum(double v) {
      return v == Math.floor(v) ? String.valueOf((long)v) : String.valueOf(v);
   }

   @Override
   protected void a(char c, int key) {
      Minecraft mc = this.b;
      if (key == Keyboard.KEY_M || key == Keyboard.KEY_ESCAPE) {
         if (this.menuOpen) {
            this.closeMenu();
         } else {
            this.b.a((da)null);
         }
      } else if (key == Keyboard.KEY_B && !this.menuOpen) {
         int bx = (int)Math.floor(this.camX + (double)(this.curMx - this.c / 2) / this.ppb());
         int bz = (int)Math.floor(this.camZ + (double)(this.curMy - this.d / 2) / this.ppb());
         int by = guessSurfaceY(mc, bx, bz);
         String nm = XaeroWpts.add(mc.f, bx, by, bz, XaeroWptScreen.nextColorHex());
         XaeroKeys.chat(mc, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + nm + " (" + bx + ", " + by + ", " + bz + ") \u5df2\u6dfb\u52a0");
      } else {
         super.a(c, key);
      }
   }

   private static int guessSurfaceY(Minecraft mc, int bx, int bz) {
      try {
         lm ch = mc.f.c(bx >> 4, bz >> 4);
         int y = XaeroMapData.topAt(ch, bx & 15, bz & 15);
         return y >= 0 ? y : 64;
      } catch (Throwable var4) {
         return 64;
      }
   }

   @Override
   protected void a(ke bn) {
      if (bn.f == 0) {
         this.b.a((da)null);
      } else if (bn.f == 10) {
         this.teleportTo(this.menuBX, this.menuBZ);
         this.closeMenu();
      } else if (bn.f == 11) {
         int by = guessSurfaceY(this.b, this.menuBX, this.menuBZ);
         String nm = XaeroWpts.add(this.b.f, this.menuBX, by, this.menuBZ, XaeroWptScreen.nextColorHex());
         XaeroKeys.chat(this.b, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + nm + " (" + this.menuBX + ", " + by + ", " + this.menuBZ + ") \u5df2\u6dfb\u52a0");
         this.closeMenu();
      } else if (bn.f == 12) {
         this.closeMenu();
      }
   }

   @Override
   public boolean c() {
      return false;
   }

   @Override
   public void h() {
      if (this.tiles != null) {
         this.tiles.flush(true);
      }

      XaeroWpts.flushIfDirty(this.b.f);
      super.h();
   }

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

   private static void fillRectOutline(float x, float y, float w, float h, int argb) {
      fillRect(x, y, w, 1.0F, argb);
      fillRect(x, y + h - 1.0F, w, 1.0F, argb);
      fillRect(x, y, 1.0F, h, argb);
      fillRect(x + w - 1.0F, y, 1.0F, h, argb);
   }

   private static int mixDark(int argb) {
      int r = (argb >>> 16 & 255) * 2 / 3 << 16;
      int g = (argb >>> 8 & 255) * 2 / 3 << 8;
      int b = (argb & 255) * 2 / 3;
      return r | g | b | -16777216;
   }
}
