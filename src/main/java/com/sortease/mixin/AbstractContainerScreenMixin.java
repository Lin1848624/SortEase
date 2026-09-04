package com.sortease.mixin;

import com.sortease.client.OverlayGuiPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 AbstractContainerScreen.render 开头捕获 leftPos/topPos，
 * 供覆盖层在 ScreenEvent.Render.Post 中换算槽位坐标。
 * 相比“接口 + 强转”的方式，对 Inventorio 等自定义子类更稳健。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "render", at = @At("HEAD"))
    private void sortease$captureGuiPos(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        OverlayGuiPos.capture((AbstractContainerScreen<?>) (Object) this, this.leftPos, this.topPos);
    }
}
