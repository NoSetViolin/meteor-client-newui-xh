# Sodium / Iris Xray 兼容修复（fix2）

Meteor mod 版本：`0.5.8-neoforge.2`，包含此前 Petrolpark 修复。

## 对应实例与原因

目标实例：`D:/PCL2CE/.minecraft/versions/1.21.1-jixiedongli`。
实测使用该实例的 Sodium **0.8.13+mc1.21.1**、Iris **1.8.14-beta.1+mc1.21.1**
以及 **ComplementaryUnbound_r5.9.3.zip**。检查时用户实例的光影开关为关闭。

此前的移植仅接入原版/NeoForge 的模型渲染器，而 Sodium 使用自己的区块模型渲染、
方块面遮挡缓存及照明数据。因此 Xray 模块即使开启，Sodium 仍绘制完整石墙并剔除内部矿石面。
复现截图在 `validation/sodium-fix/before/`：开启 Xray 后石墙仍完整不透明。

## 修改

- 接入 Sodium `BlockRenderer.renderModel`，跳过需要隐藏的非白名单方块。
- 接入 `BlockOcclusionCache.shouldDrawSide`，显示白名单矿石被普通方块挡住的面。
- 接入 `LightDataAccess.compute`，保留几何/AO 标志并为矿石提供可见的照明；保留 Fullbright 的亮度设置。
- 接入 NeoForge Sodium 的流体渲染器，隐藏被 Xray 排除的液体。
- 使用 Iris 的可选公共 API 获取真实光影状态，替换此前恒 false 的占位实现。
- 兼容 mixin 仅在检测到 Sodium 时启用，不打包或硬依赖 Sodium/Iris 类，不包含 Fabric Loader/API。

Sodium 模式沿用 Meteor 的零透明度策略：**非白名单方块完全隐藏，opacity 按 0 处理**。
无 Sodium 且未开启光影时继续使用此前原版渲染路径和用户设置的 opacity。
不承诺此修复恢复所有历史 Sodium/Iris 兼容功能或适配其他版本的内部 API。

## 验证

在官方 NeoForge 21.1.265 独立安装实例中加载实际修复 jar，保留 Petrolpark 1.4.36、
Create 6.0.10、Baritone，使用隔离单人测试存档。自检默认关闭。

| 组合 | 验证内容 |
|---|---|
| Sodium + Iris，关闭光影 | 关闭时石墙遮住矿石；开启后钻石、金、绿宝石矿可见；关闭后石墙恢复 |
| Sodium + Iris，启用上述 Complementary 光影 | Iris API 确认 shaders=true；同样进行 Xray 开启/关闭画面对照 |
| 不装 Sodium / Iris | 确认兼容 mixin 被跳过，原版 Xray 的 opacity=25、opacity=0、关闭恢复仍工作 |

三种组合同时复测 GUI、Petrolpark 未绑定效果引用、FOV、六类 tooltip、Baritone help、配置保存。
必须结合截图检查；仅自检输出模块已开启不等于画面透视生效。
光影包自身会输出部分跨版本 uniform/方块属性警告，实际光影管线和 Xray 画面正常。

最终证据目录：`validation/sodium-fix/shaders-off/`、`shaders-on/`、`vanilla-backend/`。
尚未在用户完整整合包上重新连接原多人服务器；不覆盖服务器对矿石数据的隐藏措施或所有模组方块/机械结构渲染器。

## 安装

替换旧 Meteor，保留实例内的 Sodium、Iris、Petrolpark、Create 及其他模组。
版本标识为 `0.5.8-neoforge.2`。不要同时安装多个 Meteor jar。
若本次已自动替换，原 Meteor 备份位于实例目录的 `meteor-mod-backups/sodium-iris-fix2/`。
部署结果及 SHA-256 记录在交付目录 `deployment.json` 和 `SHA256SUMS.txt`。
