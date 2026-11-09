import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class XaeroSettingsScreen extends da {
   private boolean draggingSize = false;
   private int barX;
   private int barY;
   private static final int BAR_W = 220;
   private static final int BAR_H = 10;

   public XaeroSettingsScreen() {
   }

   @Override
   public void b() {
      XaeroCfg.ensureLoaded();
      this.rebuild();
   }

   private void rebuild() {
      this.e.clear();
      int xL = this.c / 2 - 152;
      int xR = this.c / 2 + 2;
      int y0 = this.d / 2 - 6;
      this.e.add(new ke(5, xL, y0, 150, 20, this.shapeLabel()));
      this.e.add(new ke(13, xR, y0, 150, 20, XaeroHud.zoomLabel()));
      this.e.add(new ke(9, xL, y0 + 24, 150, 20, this.enabledLabel()));
      this.e.add(new ke(6, xR, y0 + 24, 150, 20, this.entityLabel()));
      this.e.add(new ke(10, xL, y0 + 48, 150, 20, this.playerLabel()));
      this.e.add(new ke(7, xR, y0 + 48, 150, 20, this.wpLabel()));
      this.e.add(new ke(11, xL, y0 + 72, 150, 20, this.coordLabel()));
      this.e.add(new ke(12, xR, y0 + 72, 150, 20, this.biomeLabel()));
      this.e.add(new ke(14, xL, y0 + 96, 150, 20, this.caveLabel()));
      this.e.add(new ke(8, xL, this.d - 54, 150, 20, "\u6062\u590d\u9ed8\u8ba4"));
      this.e.add(new ke(4, xR, this.d - 54, 150, 20, "\u5b8c\u6210"));
      this.barX = this.c / 2 - BAR_W / 2;
      this.barY = this.d / 2 - 34;
   }

   private String shapeLabel() {
      return "\u5f62\u72b6: " + (XaeroCfg.circle ? "\u5706\u5f62" : "\u65b9\u5f62");
   }

   private String entityLabel() {
      return "\u663e\u793a\u751f\u7269: " + (XaeroCfg.showEntities ? "\u5f00" : "\u5173");
   }

   private String playerLabel() {
      return "\u663e\u793a\u73a9\u5bb6: " + (XaeroCfg.showPlayers ? "\u5f00" : "\u5173");
   }

   private String wpLabel() {
      return "\u663e\u793a\u8def\u5f84\u70b9: " + (XaeroCfg.showWaypoints ? "\u5f00" : "\u5173");
   }

   private String coordLabel() {
      return "\u663e\u793a\u5750\u6807: " + (XaeroCfg.showCoords ? "\u5f00" : "\u5173");
   }

   private String biomeLabel() {
      return "\u663e\u793a\u7fa4\u7cfb: " + (XaeroCfg.showBiome ? "\u5f00" : "\u5173");
   }

   private String enabledLabel() {
      return "\u5c0f\u5730\u56fe: " + (XaeroCfg.enabled ? "\u5f00" : "\u5173");
   }

   private String caveLabel() {
      return "\u6d1e\u7a74\u6a21\u5f0f(C): " + (XaeroCfg.caveMode ? "\u5f00" : "\u5173");
   }

   @Override
   public void a(int mx, int my, float pt) {
      Minecraft mc = this.b;
      this.i();
      sj fnt = this.g;

      try {
         if (Mouse.isCreated()) {
            int dw = Mouse.getDWheel();
            if (dw != 0 && this.overBar(mx, my)) {
               XaeroCfg.size = XaeroCfg.clamp(XaeroCfg.size + (dw > 0 ? 8 : -8), 32, 512);
            }

            if (Mouse.isButtonDown(0) && (this.draggingSize || this.overBar(mx, my))) {
               this.draggingSize = true;
               float k = (float)(mx - this.barX) / (float)BAR_W;
               XaeroCfg.size = XaeroCfg.clamp(Math.round(k * 480.0F) / 8 * 8 + 32, 32, 512);
            } else {
               this.draggingSize = false;
            }
         }
      } catch (Throwable var8) {
         ;
      }

      fnt.a("\u00a7lXaero \u5c0f\u5730\u56fe\u8bbe\u7f6e", this.c / 2 - fnt.a("Xaero ") / 2 - 20, 24, 16777215);
      String sizeLbl = "\u5730\u56fe\u5927\u5c0f: " + XaeroCfg.size + " px";
      fnt.a(sizeLbl, this.c / 2 - fnt.a(sizeLbl) / 2, this.barY - 14, 14737632);
      GL11.glDisable(3553);
      fillRect((float)this.barX, (float)this.barY, (float)BAR_W, (float)BAR_H, -12303292);
      float frac = (float)(XaeroCfg.size - 32) / 480.0F;
      fillRect((float)this.barX, (float)this.barY, frac * (float)BAR_W, (float)BAR_H, -16729153);
      fillRect((float)(this.barX + (int)(frac * (float)BAR_W) - 2), (float)(this.barY - 3), 5.0F, (float)(BAR_H + 6), -1);
      GL11.glEnable(3553);
      String hint = "\u62d6\u52a8\u6ed1\u6761\u81ea\u7531\u7f29\u653e\uff0c\u4e5f\u53ef\u5728\u6ed1\u6761\u4e0a\u6eda\u8f6e";
      fnt.a(hint, this.c / 2 - fnt.a(hint) / 2, this.barY + BAR_H + 5, 6710886);
      super.a(mx, my, pt);
   }

   private boolean overBar(int mx, int my) {
      return mx >= this.barX - 4 && mx <= this.barX + BAR_W + 4 && my >= this.barY - 6 && my <= this.barY + BAR_H + 6;
   }

   @Override
   protected void a(ke bn) {
      switch(bn.f) {
      case 4:
         XaeroCfg.save();
         this.b.a((da)null);
         break;
      case 5:
         XaeroCfg.circle = !XaeroCfg.circle;
         bn.e = this.shapeLabel();
         XaeroHud.markAllDirty();
         break;
      case 6:
         XaeroCfg.showEntities = !XaeroCfg.showEntities;
         bn.e = this.entityLabel();
         break;
      case 7:
         XaeroCfg.showWaypoints = !XaeroCfg.showWaypoints;
         bn.e = this.wpLabel();
         break;
      case 8:
         XaeroCfg.enabled = true;
         XaeroCfg.size = 96;
         XaeroCfg.circle = false;
         XaeroCfg.showEntities = true;
         XaeroCfg.showPlayers = true;
         XaeroCfg.showWaypoints = true;
         XaeroCfg.showCoords = true;
         XaeroCfg.showBiome = true;
         XaeroCfg.caveMode = false;
         XaeroHud.markAllDirty();
         this.rebuild();
         break;
      case 9:
         XaeroCfg.enabled = !XaeroCfg.enabled;
         bn.e = this.enabledLabel();
         break;
      case 10:
         XaeroCfg.showPlayers = !XaeroCfg.showPlayers;
         bn.e = this.playerLabel();
         break;
      case 11:
         XaeroCfg.showCoords = !XaeroCfg.showCoords;
         bn.e = this.coordLabel();
         break;
      case 12:
         XaeroCfg.showBiome = !XaeroCfg.showBiome;
         bn.e = this.biomeLabel();
         break;
      case 14:
         XaeroCfg.caveMode = !XaeroCfg.caveMode;
         bn.e = this.caveLabel();
         XaeroHud.forceResample();
         break;
      case 13:
         XaeroHud.cycleZoom();
         bn.e = XaeroHud.zoomLabel();
      }
   }

   @Override
   protected void a(char ch, int key) {
      if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_O) {
         XaeroCfg.save();
         this.b.a((da)null);
      } else {
         super.a(ch, key);
      }
   }

   @Override
   public void h() {
      XaeroCfg.save();
      super.h();
   }

   @Override
   public boolean c() {
      return false;
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
