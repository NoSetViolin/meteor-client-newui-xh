package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IChatHudLine;
import meteordevelopment.meteorclient.mixininterface.IChatHudLineVisible;
import meteordevelopment.meteorclient.mixininterface.IMessageHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.client.GuiMessageTag.Icon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({ChatComponent.class})
public abstract class ChatHudMixin implements IChatHud {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private List<Line> trimmedMessages;
   @Shadow
   @Final
   private List<GuiMessage> allMessages;
   @Unique
   private BetterChat betterChat;
   @Unique
   private int nextId;
   @Unique
   private boolean skipOnAddMessage;

   @Shadow
   public abstract void addMessage(Component var1, @Nullable MessageSignature var2, @Nullable GuiMessageTag var3);

   @Shadow
   public abstract void addMessage(Component var1);

   @Override
   public void meteor$add(Component message, int id) {
      this.nextId = id;
      this.addMessage(message);
      this.nextId = 0;
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLineVisible(GuiMessage message, CallbackInfo ci) {
      ((IChatHudLine)(Object)this.trimmedMessages.getFirst()).meteor$setId(this.nextId);
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLine(GuiMessage message, CallbackInfo ci) {
      ((IChatHudLine)(Object)this.allMessages.getFirst()).meteor$setId(this.nextId);
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "NEW",
         target = "(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)Lnet/minecraft/client/GuiMessage$Line;"
      )}
   )
   private Line onAddMessage_modifyChatHudLineVisible(Line line, @Local(ordinal = 1) int j) {
      IMessageHandler handler = (IMessageHandler)(Object)this.minecraft.getChatListener();
      if (handler == null) {
         return line;
      } else {
         IChatHudLineVisible meteorLine = (IChatHudLineVisible)(Object)line;
         meteorLine.meteor$setSender(handler.meteor$getSender());
         meteorLine.meteor$setStartOfEntry(j == 0);
         return line;
      }
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"},
      at = {@At(
         value = "NEW",
         target = "(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)Lnet/minecraft/client/GuiMessage;"
      )}
   )
   private GuiMessage onAddMessage_modifyChatHudLine(GuiMessage line) {
      IMessageHandler handler = (IMessageHandler)(Object)this.minecraft.getChatListener();
      if (handler == null) {
         return line;
      } else {
         ((IChatHudLine)(Object)line).meteor$setSender(handler.meteor$getSender());
         return line;
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"},
      cancellable = true
   )
   private void onAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
      if (!this.skipOnAddMessage) {
         ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, this.nextId));
         if (event.isCancelled()) {
            ci.cancel();
         } else {
            this.trimmedMessages.removeIf(msg -> ((IChatHudLine)(Object)msg).meteor$getId() == this.nextId && this.nextId != 0);

            for (int i = this.allMessages.size() - 1; i > -1; i--) {
               if (((IChatHudLine)(Object)this.allMessages.get(i)).meteor$getId() == this.nextId && this.nextId != 0) {
                  this.allMessages.remove(i);
                  this.getBetterChat().removeLine(i);
               }
            }

            if (event.isModified()) {
               ci.cancel();
               this.skipOnAddMessage = true;
               this.addMessage(event.getMessage(), signatureData, event.getIndicator());
               this.skipOnAddMessage = false;
            }
         }
      }
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "CONSTANT",
         args = {"intValue=100"}
      )}
   )
   private int maxLength(int size) {
      return Modules.get() != null && this.getBetterChat().isLongerChat() ? size + this.betterChat.getExtraChatLines() : size;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "CONSTANT",
         args = {"intValue=100"}
      )}
   )
   private int maxLengthVisible(int size) {
      return Modules.get() != null && this.getBetterChat().isLongerChat() ? size + this.betterChat.getExtraChatLines() : size;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;ceil(F)I"
      )}
   )
   private int onRender_modifyWidth(int width) {
      return this.getBetterChat().modifyChatWidth(width);
   }

   @ModifyReceiver(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
      )}
   )
   private GuiGraphics onRender_beforeDrawTextWithShadow(
      GuiGraphics context, Font textRenderer, FormattedCharSequence text, int x, int y, int color, @Local Line line
   ) {
      this.getBetterChat().drawPlayerHead(context, line, y, color);
      return context;
   }

   @ModifyExpressionValue(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/GuiMessage$Line;tag()Lnet/minecraft/client/GuiMessageTag;"
      )}
   )
   private GuiMessageTag onRender_modifyIndicator(GuiMessageTag indicator) {
      return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z"
      )},
      locals = LocalCapture.CAPTURE_FAILSOFT
   )
   private void onBreakChatMessageLines(GuiMessage message, CallbackInfo ci, int i, Icon icon, List<FormattedCharSequence> list) {
      if (Modules.get() != null) {
         this.getBetterChat().lines.addFirst(list.size());
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;remove(I)Ljava/lang/Object;"
      )}
   )
   private void onRemoveMessage(GuiMessage message, CallbackInfo ci) {
      if (Modules.get() != null) {
         int extra = this.getBetterChat().isLongerChat() ? this.getBetterChat().getExtraChatLines() : 0;

         for (int size = this.betterChat.lines.size(); size > 100 + extra; size--) {
            this.betterChat.lines.removeLast();
         }
      }
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"},
      at = {@At("HEAD")}
   )
   private void onClear(boolean clearHistory, CallbackInfo ci) {
      this.getBetterChat().lines.clear();
   }

   @Inject(
      method = {"Lnet/minecraft/client/gui/components/ChatComponent;refreshTrimmedMessages()V"},
      at = {@At("HEAD")}
   )
   private void onRefresh(CallbackInfo ci) {
      this.getBetterChat().lines.clear();
   }

   @Unique
   private BetterChat getBetterChat() {
      if (this.betterChat == null) {
         this.betterChat = Modules.get().get(BetterChat.class);
      }

      return this.betterChat;
   }
}

