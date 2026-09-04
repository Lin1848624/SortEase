package com.sortease.classify;

/** 槽位分类（客户端覆盖层与服务端整理共用）。 */
public enum SlotKind {
    /** 容器主体存储（可整理） */
    MAIN,
    /** 玩家主物品栏（可整理，默认随配置） */
    PLAYER_MAIN,
    /** 快捷栏 */
    PLAYER_HOTBAR,
    /** 特殊区域：升级槽/过滤槽/护甲/副手/合成格/结果格等（默认不操作） */
    SPECIAL;

    public String langKey() {
        return "sorted.slotkind." + name().toLowerCase();
    }
}
