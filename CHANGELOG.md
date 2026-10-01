# Changelog

---

## 0.2.0

### English

- Ported SortEase to **Minecraft 1.21.1 / NeoForge 21.1.x** (Java 21).
- Build system moved from ForgeGradle to NeoForge's official **ModDevGradle** (2.0.148) with NeoForge 21.1.252; the reobfuscation step is gone because NeoForge 1.21 runs on Mojang mappings.
- Networking rewritten for the 1.21 payload system: `SimpleChannel` → `RegisterPayloadHandlersEvent` + `CustomPacketPayload` (`sortease:sort_request` / `sortease:sort_ack`), sent via `PacketDistributor`.
- Configs migrated from `ForgeConfigSpec` to `ModConfigSpec`; the mod-list "Config" button now comes from NeoForge's `IConfigScreenFactory` extension point.
- Events migrated to their NeoForge 1.21 equivalents (`ServerTickEvent.Post`, `ClientTickEvent.Post`, `ScreenEvent.*`, `EventBusSubscriber`); mod metadata is now `META-INF/neoforge.mods.toml` and declares the mixin config.
- Item equality and menu signatures now use the 1.21 data component API (`ItemStack.isSameItemSameComponents`, `ItemStack.hashItemAndComponents`) because `getTag()`/NBT was removed.
- Fixed the garbled mod description shown in the mod list (`gradle.properties` now uses `\uXXXX` escapes).

### 中文

- 将 SortEase 移植到 **Minecraft 1.21.1 / NeoForge 21.1.x**（Java 21）。
- 构建从 ForgeGradle 换成 NeoForge 官方 **ModDevGradle**（2.0.148）+ NeoForge 21.1.252；NeoForge 1.21 直接使用 Mojang 官方名，因此不再需要 reobf 重混淆步骤。
- 网络层改写为 1.21 的载荷机制：`SimpleChannel` → `RegisterPayloadHandlersEvent` + `CustomPacketPayload`（`sortease:sort_request` / `sortease:sort_ack`），发送改走 `PacketDistributor`。
- 配置由 `ForgeConfigSpec` 换成 `ModConfigSpec`；模组列表的「配置」按钮改由 NeoForge 的 `IConfigScreenFactory` 扩展点提供。
- 事件全部换成 NeoForge 1.21 对应类型（`ServerTickEvent.Post`、`ClientTickEvent.Post`、`ScreenEvent.*`、`EventBusSubscriber`）；元数据文件改为 `META-INF/neoforge.mods.toml` 并在其中声明混入配置。
- 物品等价判定与界面签名改用 1.21 的数据组件 API（`ItemStack.isSameItemSameComponents`、`ItemStack.hashItemAndComponents`），因为 `getTag()`/NBT 已被移除。
- 修正模组列表里乱码的描述文本（`gradle.properties` 改用 `\uXXXX` 转义）。

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
