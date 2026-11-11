package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Minecraft.class})
public interface MinecraftClientAccessor {
   @Accessor("fps:I")
   static int getFps() {
      return 0;
   }

   @Mutable
   @Accessor("user:Lnet/minecraft/client/User;")
   void setSession(User var1);

   @Accessor("proxy:Ljava/net/Proxy;")
   Proxy getProxy();

   @Accessor("reloadStateTracker:Lnet/minecraft/client/ResourceLoadStateTracker;")
   ResourceLoadStateTracker getResourceReloadLogger();

   @Invoker("startAttack()Z")
   boolean leftClick();

   @Mutable
   @Accessor("profileKeyPairManager:Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;")
   void setProfileKeys(ProfileKeyPairManager var1);

   @Accessor("authenticationService:Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;")
   YggdrasilAuthenticationService getAuthenticationService();

   @Mutable
   @Accessor
   void setUserApiService(UserApiService var1);

   @Mutable
   @Accessor("minecraftSessionService:Lcom/mojang/authlib/minecraft/MinecraftSessionService;")
   void setSessionService(MinecraftSessionService var1);

   @Mutable
   @Accessor("authenticationService:Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;")
   void setAuthenticationService(YggdrasilAuthenticationService var1);

   @Mutable
   @Accessor("skinManager:Lnet/minecraft/client/resources/SkinManager;")
   void setSkinProvider(SkinManager var1);

   @Mutable
   @Accessor("playerSocialManager:Lnet/minecraft/client/gui/screens/social/PlayerSocialManager;")
   void setSocialInteractionsManager(PlayerSocialManager var1);

   @Mutable
   @Accessor("reportingContext:Lnet/minecraft/client/multiplayer/chat/report/ReportingContext;")
   void setAbuseReportContext(ReportingContext var1);

   @Mutable
   @Accessor("profileFuture:Ljava/util/concurrent/CompletableFuture;")
   void setGameProfileFuture(CompletableFuture<ProfileResult> var1);
}
