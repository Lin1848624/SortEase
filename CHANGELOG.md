# Changelog

---

## 0.1.3

### English

- Added compatibility for **Inventorio**'s **Deep Pockets** enchantment (Minecraft 1.20.1 · Forge · Inventorio v1.10.4): in Inventorio's own inventory screen, the Deep Pockets rows unlocked by the enchantment are sorted together with the player's main inventory.
- The utility belt, the tool belt, locked (not yet unlocked) Deep Pockets rows, and the hotbar inside that screen are now permanently protected: they are never moved, even when forced to participate from the classification overlay.
- The player main inventory is now sortable in Inventorio's inventory screen even when "Also sort player main inventory" is disabled, matching vanilla inventory-screen behaviour.
- The classification overlay no longer draws inactive slots (no more borders below the Inventorio panel) and refreshes itself when the enchantment level changes (e.g. swapping leggings).
- Inactive / disabled slots are now excluded from sorting in every menu, which also fixes Inventorio's blocked crafting slot being treated as sortable when its 2×2 crafting grid is disabled.

### 中文

- 适配 **Inventorio** 的**深口袋（Deep Pockets）附魔**（Minecraft 1.20.1 · Forge · Inventorio v1.10.4）：在 Inventorio 自己的物品栏界面整理时，附魔已解锁的深口袋行会与玩家主栏一起整理。
- 快捷腰带、工具腰带、未解锁的深口袋行，以及该界面内的快捷栏现在永久受保护：即使从分类视图强制参与也不会被移动。
- 在 Inventorio 物品栏界面里，即使关闭「同时整理玩家主物品栏」，玩家主栏仍可整理（与原版物品栏界面行为一致）。
- 分类覆盖层不再绘制未激活的槽位（不会在 Inventorio 面板下方画出不存在的格子），并在附魔等级变化（如更换护腿）时自动刷新。
- 所有界面中未激活 / 被禁用的槽位一律不参与整理；顺带修复 Inventorio 关闭 2×2 合成格时，其占位槽被当成可整理槽的问题。

---

## 0.1.2

### English

- Rewrote the player inventory partition logic: main inventory / hotbar are included per their own config toggles, while armor, off-hand, crafting and result slots are hard-isolated and can never be pulled into a sorting group (fixes sorting of the vanilla inventory in every view).
- Fixed own-inventory semantics: when the open menu is a player inventory screen (including Inventorio's), main slots are sortable even if "sort player main inventory" is disabled; the hotbar still honors its own toggle.
- Slot swaps are now atomic (two-phase write with rollback) and identical-stack swaps are skipped entirely, eliminating pointless slot updates and their accompanying movement animations.
- The reorder engine now compares stacks with `ItemStack.isSameItemSameTags` (vanilla `ItemStack.equals` is identity-based in 1.20.1).
- Added a preflight check: if nothing can be merged and every group is already in order, the job finishes with zero writes and zero broadcasts.
- Server-side deduplication: for each player, the content/layout signature plus the request parameters of the last finished sort are remembered; an identical repeat request on an unchanged container is silently skipped.
- Client-side visual deduplication: after a sort completes, the client remembers what the open screen shows; pressing the sort key again while nothing visible has changed no longer sends a request at all.

### 中文

- 重写了玩家背包的分区逻辑：主栏/快捷栏分别按各自开关参与整理；护甲、副手、合成区与结果槽一律硬隔离，绝不并入整理组（修复各种界面下原版物品栏无法整理的问题）。
- 修正“自身物品栏”判定：当打开的是玩家物品栏界面（含 Inventorio 的界面）时，即使关闭“整理玩家主栏”，主栏仍可整理；快捷栏依旧遵守自己的开关。
- 槽位交换改为原子操作（两阶段写入 + 失败回滚），且两槽内容完全相同时直接跳过写入，不再产生无意义的槽位更新与挪动动画。
- 重排引擎改用 `ItemStack.isSameItemSameTags` 判断堆叠等价（1.20.1 中 `ItemStack.equals` 是对象同一性比较）。
- 新增整理前预检：若已无任何可合并项且所有分组均已有序，任务零写入、零广播直接结束。
- 服务端去重：为每位玩家记录上次整理完成后的容器内容/布局签名与请求参数；相同容器、内容未变时的重复请求会被静默跳过。
- 客户端“视觉去重”：整理完成后记住当前界面内容；只要界面上没有可见变化，再次按键不会发出任何请求。

---

## 0.1.1

### English

- First public release under the new name **SortEase** (renamed because the original name was already taken on CurseForge) for Minecraft Forge 1.20.1.
- One-click dynamic, stack-aware inventory sorting: stack limits are never hard-coded, and very large custom stacks are handled safely with overflow-proof arithmetic.
- Automatically recognizes "special regions" (such as Create:Storage tool/upgrade/filter slots and Sophisticated Backpacks upgrade slots) and leaves them untouched by default.
- In-game visual overlay to view and switch each slot's sort classification on the fly, with dedicated hotkeys.
- Fixed a crash (`AbstractMethodError`) when the Inventorio inventory screen was open by capturing the screen anchor through a render-head mixin.
- Released under GPLv3 with a 512×512 icon and a bilingual (English/Chinese) README.

### 中文

- 以新名称 **SortEase** 首次公开发布（因原名在 CurseForge 上已被占用而改名），适配 Minecraft Forge 1.20.1。
- 一键动态“堆叠感知”整理：不硬编码堆叠上限，超大自定义堆叠（如 10 亿+）也能以防溢出算法安全处理。
- 自动识别“特殊区域”（如 Create:Storage 的工具/升级/过滤槽、精妙背包的升级槽等）并默认不触碰。
- 提供游戏内可视化覆盖层，可实时查看并一键切换每个槽位的整理分类，并配有专用快捷键。
- 修复打开 Inventorio 物品栏界面时的崩溃（`AbstractMethodError`）：改为在渲染头部通过 Mixin 捕获界面锚点坐标。
- 采用 GPLv3 许可证，附带 512×512 图标与中英双语 README。
