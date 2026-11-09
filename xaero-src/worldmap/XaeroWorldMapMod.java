import dev.liber.api.ClientModInitializer;
import dev.liber.api.Liber;
import dev.liber.api.ModInitializer;

public final class XaeroWorldMapMod implements ModInitializer, ClientModInitializer {
   public void onInitialize() {
      System.out.println("[XaeroWorldMap] main init");
   }

   public void onInitializeClient() {
      if (!Liber.isModLoaded("xaero-minimap")) {
         System.out.println("[XaeroWorldMap] WARNING: xaero-minimap 未安装，按 M 无法打开地图");
      } else {
         System.out.println("[XaeroWorldMap] client ready (配合 xaero-minimap)");
      }
   }
}
