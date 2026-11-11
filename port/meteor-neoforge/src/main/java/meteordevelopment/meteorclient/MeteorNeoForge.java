package meteordevelopment.meteorclient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import meteordevelopment.meteorclient.utils.tooltip.*;

/**
 * NeoForge 入口。
 *
 * <p>原 Fabric 版通过 {@code fabric.mod.json} 的 client entrypoint 调用
 * 这里由 FML 构造器注册客户端事件；Minecraft 构造完成后再初始化 Meteor。</p>
 */
@Mod(value = Platform.MOD_ID, dist = Dist.CLIENT)
public final class MeteorNeoForge {
   public MeteorNeoForge(IEventBus modBus) {
      modBus.addListener(MeteorNeoForge::registerTooltips);
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
         net.neoforged.bus.api.EventPriority.LOWEST, MeteorNeoForge::freecamCameraAngles);
      // MinecraftClientMixin initializes Meteor after Minecraft's constructor completes.
   }

   private static void freecamCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
      if (MeteorClient.mc == null || MeteorClient.mc.player == null) return;
      var modules = meteordevelopment.meteorclient.systems.modules.Modules.get();
      if (modules == null) return;
      var freecam = modules.get(meteordevelopment.meteorclient.systems.modules.render.Freecam.class);
      if (freecam == null || !freecam.isActive()) return;
      // NeoForge's normal camera path uses setRotation(FFF), not the vanilla
      // setRotation(FF) call still used by the sleeping-camera branch.
      float partialTick = (float) event.getPartialTick();
      event.setYaw((float) freecam.getYaw(partialTick));
      event.setPitch((float) freecam.getPitch(partialTick));
   }

   private static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
      event.register(TextTooltipComponent.class, MeteorTooltipData::getComponent);
      event.register(MapTooltipComponent.class, MeteorTooltipData::getComponent);
      event.register(EntityTooltipComponent.class, MeteorTooltipData::getComponent);
      event.register(ContainerTooltipComponent.class, MeteorTooltipData::getComponent);
      event.register(BookTooltipComponent.class, MeteorTooltipData::getComponent);
      event.register(BannerTooltipComponent.class, MeteorTooltipData::getComponent);
   }
}

