package com.sorted.classify;

/** 一个已打开菜单的分类结果（槽位按菜单槽索引对齐，客户端/服务端结构一致）。 */
public final class MenuProfile {
    /** 持久化人工覆写用的菜单键（用类名，双端一致）。 */
    public final String menuKey;
    /** 是否为生存背包主界面（无主体容器的特殊屏幕也允许整理玩家栏）。 */
    public final boolean inventoryScreen;
    /** 是否存在 MAIN 主体存储（决定该菜单是否可整理）。 */
    public boolean hasMainContainer;
    /** 保留位：该界面被标记为不可整理。 */
    public final boolean unsupported;

    private final SlotKind[] kinds;

    public MenuProfile(String menuKey, int slotCount, boolean inventoryScreen, boolean hasMainContainer, boolean unsupported) {
        this.menuKey = menuKey;
        this.inventoryScreen = inventoryScreen;
        this.hasMainContainer = hasMainContainer;
        this.unsupported = unsupported;
        this.kinds = new SlotKind[slotCount];
        java.util.Arrays.fill(this.kinds, SlotKind.SPECIAL);
    }

    public void setKind(int slotIndex, SlotKind kind) {
        if (slotIndex >= 0 && slotIndex < kinds.length) kinds[slotIndex] = kind;
    }

    public int slotCount() {
        return kinds.length;
    }

    public SlotKind kind(int slotIndex) {
        return kinds[slotIndex];
    }

    /** 基础默认是否参与整理（不含手动覆写；玩家栏需另行按配置过滤）。 */
    public boolean sortableByDefault(int slotIndex) {
        SlotKind k = kinds[slotIndex];
        return k == SlotKind.MAIN || k == SlotKind.PLAYER_MAIN || k == SlotKind.PLAYER_HOTBAR;
    }

    public boolean canSort() {
        return !unsupported && (hasMainContainer || inventoryScreen);
    }
}
