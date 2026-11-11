# Petrolpark 状态效果查询兼容修复（fix1）

Meteor mod 版本：`0.5.8-neoforge.1`。Minecraft 1.21.1、NeoForge 21.1.265。

## 原因

原实例崩溃报告：`crash-2026-10-29_17.47.33-client.txt`。
Petrolpark 1.4.36 的 `GoldHelper.onEntityTick` 调用 `LivingEntity.hasEffect` 查询
`petrolpark:midas_touch`。该效果通过 `SharedFeatureFlag.GOLD_CONVERSION` 的共享功能注册路径创建；
运行时其引用可能未绑定。原版查询只需要用 Holder 作活动效果表的键，不必取出实际 MobEffect。

Meteor 的 PotionSpoof mixin 却无条件执行 `effect.value()`，即使模块关闭也如此，
因此使原本能返回 false 的查询变成 `DeferredHolder.value()` 空指针异常。
此次崩溃时只有 Discord Presence 激活，与 Xray 开关无关。

## 修复

只修改 Meteor 的 `LivingEntityMixin.hasStatusEffect`：

1. 原查询为 false 时直接保留 false。
2. 模块系统未就绪或 PotionSpoof 未启用时保留原查询结果。
3. Holder 未绑定时保留原查询结果。
4. 仅对已绑定、且确实需要被 PotionSpoof 检查的效果调用 `value()`。

没有使用捕获全部异常的方式，也没有禁用 PotionSpoof；正常的阻止/允许效果行为保留。
此防护适用于其他模组传入的未绑定状态效果 Holder，不依赖硬编码的 Petrolpark 类。

## 回归验证

回归场景直接调用实际的 `LivingEntity.hasEffect`（经过正式 mixin）：

- 修复前：使用 NeoForge `DeferredHolder` 构造同名未绑定效果，在单人世界中重现同一异常。
- 修复后：覆盖 PotionSpoof 开/关、已绑定/未绑定、原查询 true/false、正常阻止/允许效果。
- 正式独立实例加装用户版本的 Petrolpark 1.4.36、Create 6.0.10、Baritone 后验证单人世界、
  GUI、Xray 默认透明度/完全透明/关闭恢复、FOV、六类 tooltip、配置保存。
- 实际服务器和完整整合包的重新连接，仍需在原实例上复测；本地验证不宣称覆盖全部整合包行为。

证据位于 `validation/petrolpark-fix/before/` 和 `validation/petrolpark-fix/after/`。

## 安装与备份

修复版只需替换 Meteor 原 jar，保留 Petrolpark、Create、Baritone 及其余模组。
不要在同一实例中同时安装两份 Meteor。
若已由本次修复部署到 PCL 实例，旧 Meteor 备份位于该实例的
`meteor-mod-backups/petrolpark-fix1/`，不在 mods 扫描目录内。
