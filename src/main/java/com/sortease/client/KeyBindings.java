package com.sortease.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.sorted";

    public static final KeyMapping SORT = new KeyMapping("key.sorted.sort", InputConstants.KEY_O, CATEGORY);
    public static final KeyMapping OVERLAY = new KeyMapping("key.sorted.overlay", InputConstants.KEY_P, CATEGORY);
    public static final KeyMapping CONFIG = new KeyMapping("key.sorted.config", InputConstants.KEY_K, CATEGORY);

    private KeyBindings() {}
}
