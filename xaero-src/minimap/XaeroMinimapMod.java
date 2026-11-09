import dev.liber.api.ClientModInitializer;
import dev.liber.api.Liber;
import dev.liber.api.ModInitializer;
import dev.liber.api.event.LiberEvents;

public final class XaeroMinimapMod implements ModInitializer, ClientModInitializer {
   public static final String VERSION = "1.4.6";

   public void onInitialize() {
      System.out.println("[XaeroMinimap] main init, liber " + Liber.getVersion());
   }

   public void onInitializeClient() {
      XaeroCfg.load();
      LiberEvents.EVENT_BUS.register(new XaeroKeys());
      System.out.println("[XaeroMinimap] client ready v" + VERSION + " size=" + XaeroCfg.size
         + " shape=" + (XaeroCfg.circle ? "circle" : "square"));
   }
}
