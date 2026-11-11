# NeoForge 21.1.265 验证记录

本文件主体记录初始 `0.5.8-neoforge` 发行版的验证；`0.5.8-neoforge.1` 的新增修复和验证见 `PATCH-PETROLPARK.md`，证据在 `validation/petrolpark-fix/`。
`0.5.8-neoforge.2` 的 Sodium/Iris Xray 验证见 `PATCH-SODIUM-IRIS.md`，证据在 `validation/sodium-fix/`。

日期：2026-10-29。用户最终验收标准：在 NeoForge 实例中启动 Meteor，进入单人世界，开启 Xray 并确认正常使用。

## 实际测试环境

- Minecraft 1.21.1、NeoForge 21.1.265、Zulu JDK 21.0.8，Windows 11，RTX 4070 SUPER。
- 使用官方 `neoforge-21.1.265-installer.jar --installClient` 生成独立实例：`build/release-instance/`。
- 正式启动目标 `forgeclient`；从 `mods/meteor-client-neoforge.jar` 加载最终产物。
- 未使用开发输出目录、开发依赖注入、unprotect agent 或编译用 MC 修补 jar。
- 资源及普通游戏依赖可复用本机缓存；所有版本 JSON 声明 SHA-1 的依赖均校验后使用。
- 无 Baritone 和有 Baritone 两种组合分开测试。Baritone 文件 SHA-256 保持原值：
  `245707B3F4800CA85CBA9BE475E4C59438EF95EE107287086A64191018899130`。

## 测试内容与证据

| 检查 | 证据 |
|---|---|
| 正式实例启动及模块初始化 | 无 Baritone：169 个模块、1255 个设置；有 Baritone：171 个模块、1268 个设置 |
| ClickGUI | 开发实例通过真实 RightShift 打开并目视确认；正式实例自动打开同一模块界面并截图 |
| 单人世界 | 两种组合均进入专用超平坦存档，客户端收到矿石场景，正常退出并保存世界 |
| Xray 默认透明度 | opacity=25 连续运行 200 个游戏 tick，钻石、金、绿宝石矿可透过石墙看见 |
| Xray 完全透明 | opacity=0 重新构建区块后截图 |
| Xray 关闭恢复 | 关闭后重新构建区块，矿石再次被石墙完全挡住 |
| Tooltip | 原生工厂返回六类正确组件；文本、地图、实体、容器、书、旗帜均渲染截图 |
| FOV | 运行实际 GameRenderer.getFov；事件把基础值 70 改成 35，手部 FOV 保持 70 |
| AntiPacketKick | 关闭时拒绝 9 MiB 解压大小；开启时继续解压并输出完整 9 MiB；损坏压缩数据仍被拒绝 |
| Baritone | mod 存在性探测为 true，Path Manager 初始化，help 命令执行成功 |
| 配置 | 正常加载已有配置，测试退出前写出 modules.nbt、GUI 配置，随后正常关闭 |
| 静态产物 | 严格 mixin、元数据、AT、初始化索引与许可齐全；无 Fabric/intermediary/refmap、SLF4J/JSR-305 类副本 |
| 源码包独立重建 | 解压到新目录，七项构建任务全部执行成功；重建 jar 再次通过静态检查，1950 个 class 与实测 jar 逐字节一致。ZIP 时间戳等归档差异不保证相同整包哈希 |

最终证据目录：

- `validation/release-without-baritone/`：仅 Meteor 的完整日志、文本报告和截图。
- `validation/release-with-baritone/`：加装 Baritone 后的完整日志、文本报告和截图。
- `meteor-02-xray-off.png`：石墙完全遮住矿石。
- `meteor-03-xray-default-opacity.png`：石墙半透明，三列矿石可见。
- `meteor-04-xray-zero-opacity.png`：非白名单方块完全隐藏。
- `meteor-05-xray-disabled-again.png`：恢复原始石墙遮挡。
- `meteor-06-six-tooltips.png`：六类 tooltip 工厂输出的实际画面。

先前失败日志保留在工程目录用于定位，不纳入通过证据。进程退出码和自检报告均须通过，画面对照须人工检查。

## 验证边界

通过上述用户指定的单人 Xray 验收，不等同于 169/171 个模块逐个完整验收。
本轮没有测试第三方整合包、其他 NeoForge 版本、光影/替代渲染器、Fabric addon、
外部多人服务器握手、长期游戏稳定性或所有 Baritone 寻路行为。
FOV 测试验证基础事件路径与手部隔离，未穷举所有动态效果、其他视角 mod 或 Zoom 的全部设置。
地图 tooltip 测试使用专用空地图数据；真实地图内容以及 BetterTooltips 各物品输入组合未逐项覆盖。

自检默认关闭；只有显式 `-Dmeteor.selftest=true`（开发任务使用 `-PmeteorSelfTest`）才创建测试场景、切换 Xray、保存测试截图并自动退出。

交付 jar 和源码包的 SHA-256 见交付目录的 `SHA256SUMS.txt`；源码与构建步骤见 `README-NEOFORGE.md`。
