# XiaohSense — NeoForge 1.21.1

制作人：NoSetViolin。基于 Meteor Client 0.5.8，保留 GPL-3.0 与上游、第三方许可。

此分支为原生 NeoForge 移植版，适用于 Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21。
当前版本：0.5.8-neoforge.8（fix8）。

## 安装

将 `artifacts/xiaohsense-0.5.8-mc1.21.1-neoforge21.1.265-fix8.jar` 放入实例 mods 目录，避免同时安装旧版。
使用 Insert 呼出菜单。Baritone 为可选集成，根目录的 Baritone jar 同时作为构建依赖。

## 功能

- XiaohSense UI、圆角品牌图标、侧栏模糊、中文字体回退、配置页位置继承。
- Sodium / Iris Xray 兼容修复、NeoForge Freecam 转向修复。
- Thirdperson：原版前后视角、可调距离、高度和基础 FOV，保留墙体碰撞。

## 构建

在仓库根目录执行（JDK 21）：

```powershell
.\port\meteor-neoforge\gradlew.bat -p port/meteor-neoforge --no-daemon shadowJar
```

产物：`port/meteor-neoforge/build/libs/meteor-client-neoforge-0.5.8-neoforge.8.jar`。
保留目录布局：构建引用根目录的 Baritone jar。Baritone 对应源码在 `port/baritone`。

## 验证

`validation/thirdperson-fix8.txt` 保存发布前独立 NeoForge 实例的单人世界测试记录，涵盖菜单、Freecam、Thirdperson、Xray、FOV 与 tooltip。未宣称所有模块及组合均已验证。
`port/meteor-neoforge/tools/verify-jar.ps1` 可执行打包静态检查。
`run-release-test.ps1` 为本地开发验证脚本，需要预先配置独立测试实例及游戏资源。

详细移植和修复说明见 `port/meteor-neoforge/*.md`。原项目许可见 LICENSE 与资源中的许可文件。
