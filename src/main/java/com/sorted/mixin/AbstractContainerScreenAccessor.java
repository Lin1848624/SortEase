package com.sorted.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 访问 AbstractContainerScreen 的受保护坐标字段（覆盖层换算槽位屏幕坐标用）。 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int sortedLeftPos();

    @Accessor("topPos")
    int sortedTopPos();
}
