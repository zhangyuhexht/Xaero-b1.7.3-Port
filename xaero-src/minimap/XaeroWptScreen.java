import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class XaeroWptScreen extends da {
   private final int pendingX;
   private final int pendingY;
   private final int pendingZ;
   private int scroll = 0;
   private static final int ROWS = 6;
   private static final int ROW_H = 26;

   public XaeroWptScreen(int px, int py, int pz) {
      this.pendingX = px;
      this.pendingY = py;
      this.pendingZ = pz;
   }

   @Override
   public void b() {
      Minecraft mc = this.b;
      this.scroll = 0;
      if (mc != null && mc.f != null && mc.h != null && (this.pendingX != -1 || this.pendingY != -1 || this.pendingZ != -1)) {
         String name = XaeroWpts.add(mc.f, this.pendingX, this.pendingY, this.pendingZ, nextColorHex());
         XaeroKeys.chat(mc, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + name + " (" + this.pendingX + ", " + this.pendingY + ", " + this.pendingZ + ") \u5df2\u6dfb\u52a0");
      }

      this.rebuildButtons();
   }

   private void rebuildButtons() {
      this.e.clear();
      List list = XaeroWpts.all(this.b.f);
      int x0 = this.c / 2 - 150;

      for(int row = 0; row < ROWS; ++row) {
         int idx = this.scroll + row;
         if (idx < list.size()) {
            int y = 52 + row * ROW_H;
            this.e.add(new ke(100 + idx, x0 + 236, y, 32, 20, "\u4f20"));
            this.e.add(new ke(200 + idx, x0 + 272, y, 28, 20, "\u5220"));
         }
      }

      this.e.add(new ke(1, this.c / 2 - 150, this.d - 56, 98, 20, "\u4e0a\u7ffb"));
      this.e.add(new ke(2, this.c / 2 - 48, this.d - 56, 98, 20, "\u4e0b\u7ffb"));
      this.e.add(new ke(3, this.c / 2 + 54, this.d - 56, 96, 20, "\u65b0\u5efa(\u73a9\u5bb6\u5904)"));
      this.e.add(new ke(4, this.c / 2 - 100, this.d - 28, 200, 20, "\u5b8c\u6210"));
   }

   @Override
   public void a(int mx, int my, float pt) {
      this.i();
      sj fnt = this.g;
      fnt.a("\u00a7lXaero \u8def\u5f84\u70b9\u7ba1\u7406", this.c / 2 - 149, 14, 16777215);
      List list = XaeroWpts.all(this.b.f);
      String cnt = "\u5171 " + list.size() + " \u4e2a";
      fnt.a(cnt, this.c / 2 + 150 - fnt.a(cnt), 14, 11776947);
      GL11.glDisable(3553);
      fillRect((float)(this.c / 2 - 152), 34.0F, 304.0F, (float)(ROWS * ROW_H + 6), 1610612736);
      GL11.glEnable(3553);

      for(int row = 0; row < ROWS; ++row) {
         int idx = this.scroll + row;
         if (idx >= list.size()) {
            break;
         }

         XaeroWpts wp = (XaeroWpts)list.get(idx);
         int y = 58 + row * ROW_H;
         GL11.glDisable(3553);
         fillRect((float)(this.c / 2 - 148), (float)y, 16.0F, 8.0F, wp.color);
         GL11.glEnable(3553);
         String line = idx + 1 + ". " + wp.name;
         fnt.b(line, this.c / 2 - 128, y - 2, 16777215);
         String pos = "(" + wp.x + ", " + wp.y + ", " + wp.z + ")";
         fnt.b(pos, this.c / 2 - 148 + 16, y + 7, 12632256);
      }

      super.a(mx, my, pt);
   }

   @Override
   protected void a(ke bn) {
      int id = bn.f;
      if (id == 4) {
         this.close();
      } else if (id == 1) {
         this.scroll = Math.max(0, this.scroll - 1);
         this.rebuildButtons();
      } else if (id == 2) {
         int max = Math.max(0, XaeroWpts.count() - ROWS);
         this.scroll = Math.min(max, this.scroll + 1);
         this.rebuildButtons();
      } else if (id == 3) {
         Minecraft mc = this.b;
         if (mc != null && mc.h != null) {
            String nm = XaeroWpts.add(mc.f, in.b(mc.h.aM), (int)mc.h.aN, in.b(mc.h.aO), nextColorHex());
            XaeroKeys.chat(mc, "\u00a7a[Xaero] \u8def\u5f84\u70b9 " + nm + " \u5df2\u6dfb\u52a0");
            this.rebuildButtons();
         }
      } else if (id >= 200) {
         int idx = id - 200;
         if (XaeroWpts.remove(idx)) {
            XaeroKeys.chat(this.b, "\u00a77[Xaero] \u5df2\u5220\u9664\u8def\u5f84\u70b9 #" + (idx + 1));
         }

         this.clampScroll();
         this.rebuildButtons();
      } else if (id >= 100) {
         int idx = id - 100;
         XaeroWpts wp = XaeroWpts.get(idx);
         Minecraft mc = this.b;
         if (wp != null && mc != null && mc.h != null && mc.f != null) {
            mc.h.c((double)((float)wp.x + 0.5F), (double)((float)wp.y + 1.0F), (double)((float)wp.z + 0.5F), mc.h.bj, mc.h.bi);
            XaeroKeys.chat(mc, "\u00a7a[Xaero] \u5df2\u4f20\u9001\u5230 " + wp.name);
            this.close();
         }
      }
   }

   private void clampScroll() {
      int max = Math.max(0, XaeroWpts.count() - ROWS);
      if (this.scroll > max) {
         this.scroll = max;
      }

      if (this.scroll < 0) {
         this.scroll = 0;
      }
   }

   private void close() {
      XaeroWpts.flushIfDirty(this.b.f);
      this.b.a((da)null);
   }

   @Override
   protected void a(char c, int key) {
      if (key == Keyboard.KEY_K || key == Keyboard.KEY_ESCAPE) {
         this.close();
      } else {
         super.a(c, key);
      }
   }

   @Override
   public boolean c() {
      return false;
   }

   @Override
   public void h() {
      XaeroWpts.flushIfDirty(this.b.f);
      super.h();
   }

   private static int colorIdx = 0;

   static String nextColorHex() {
      String[] palette = new String[]{"FF3B30", "34C759", "007AFF", "FFCC00", "AF52DE", "FF9500"};
      return palette[colorIdx++ % palette.length];
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
}
