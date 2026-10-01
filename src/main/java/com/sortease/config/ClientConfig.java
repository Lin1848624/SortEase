package com.sortease.config;

import com.sortease.SortedMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 客户端配置 sortease-client.toml（界面/键位/排序偏好等纯客户端项）。 */
@EventBusSubscriber(modid = SortedMod.MODID)
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 0=仅合并 1=按名称 2=按注册名 3=按所属模组 4=按数量（见 SortMode） */
    public static final ModConfigSpec.IntValue SORT_MODE = BUILDER
            .comment("默认排序模式: 0=仅合并, 1=按名称, 2=按注册名, 3=按所属模组, 4=按数量")
            .defineInRange("sortMode", 1, 0, 4);
    public static final ModConfigSpec.BooleanValue SORT_ASCENDING = BUILDER
            .comment("排序方向: true=升序, false=降序（仅对 1~4 模式生效）")
            .define("sortAscending", true);
    public static final ModConfigSpec.BooleanValue INCLUDE_PLAYER_MAIN = BUILDER
            .comment("整理时是否包含玩家主物品栏（9~35 格）")
            .define("includePlayerMain", true);
    public static final ModConfigSpec.BooleanValue INCLUDE_HOTBAR = BUILDER
            .comment("整理时是否包含快捷栏（0~8 格）")
            .define("includeHotbar", true);
    public static final ModConfigSpec.BooleanValue OVERLAY_DEFAULT_ON = BUILDER
            .comment("打开容器时默认显示槽位分类覆盖层")
            .define("overlayDefaultOn", false);
    public static final ModConfigSpec.IntValue OVERLAY_OPACITY = BUILDER
            .comment("覆盖层描边透明度（0~100，100=不透明）")
            .defineInRange("overlayOpacity", 55, 0, 100);
    public static final ModConfigSpec.BooleanValue UNKNOWN_AS_SORTABLE = BUILDER
            .comment("未能被任何规则识别的未知槽位是否默认参与整理（引擎仍会做 mayPlace 硬校验，失败自动跳过）")
            .define("unknownAsSortable", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int sortMode;
    public static boolean sortAscending;
    public static boolean includePlayerMain;
    public static boolean includeHotbar;
    public static boolean overlayDefaultOn;
    public static int overlayOpacity;
    public static boolean unknownAsSortable;

    private ClientConfig() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // ModConfigEvent 会对所有模组的配置触发，这里只处理本模组的配置
        if (event.getConfig().getSpec() != SPEC) return;
        refresh();
    }

    public static void refresh() {
        sortMode = SORT_MODE.get();
        sortAscending = SORT_ASCENDING.get();
        includePlayerMain = INCLUDE_PLAYER_MAIN.get();
        includeHotbar = INCLUDE_HOTBAR.get();
        overlayDefaultOn = OVERLAY_DEFAULT_ON.get();
        overlayOpacity = OVERLAY_OPACITY.get();
        unknownAsSortable = UNKNOWN_AS_SORTABLE.get();
    }
}
