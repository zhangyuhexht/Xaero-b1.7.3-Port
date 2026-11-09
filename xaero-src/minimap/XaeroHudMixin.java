import dev.liber.api.mixin.At;
import dev.liber.api.mixin.CallbackInfo;
import dev.liber.api.mixin.Inject;
import dev.liber.api.mixin.LiberMixin;

@LiberMixin({"px"})
public final class XaeroHudMixin {
   private XaeroHudMixin() {
   }

   @Inject(
      method = {"b(F)V"},
      at = @At("RETURN")
   )
   public static void xaero$afterFrame(Object renderer, float partialTicks, CallbackInfo ci) {
      long now = System.nanoTime();
      if (now - lastRun == 0L || now - lastRun < 750000L) {
         return;
      }

      lastRun = now;
      try {
         XaeroHud.onFrame();
      } catch (Throwable var6) {
         XaeroHud.disable(var6);
      }
   }

   private static long lastRun = 0L;
}
