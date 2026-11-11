# Meteor Client 0.5.8 — 原生 NeoForge 1.21.1

目标及验证版本：Minecraft **1.21.1**、NeoForge **21.1.265**、JDK **21**。
当前 Meteor mod 版本为 **0.5.8-neoforge.5**；包含 Petrolpark 修复与 Sodium/Iris Xray 兼容，详见 `PATCH-PETROLPARK.md`、`PATCH-SODIUM-IRIS.md`。
菜单移植见 `PATCH-UI.md`；品牌图及文字见 `PATCH-BRAND.md`；圆角、位置继承和中文修复见 `PATCH-UI-FIX5.md`。不改变模块和兼容功能。
不需要 Fabric Loader、Fabric API 或 Connector。资源命名空间为 `meteor-client`，NeoForge modId 为 `meteor_client`。

## 安装

将交付的 Meteor jar 放入 Minecraft 1.21.1 + NeoForge 21.1.265 实例的 `mods` 目录。
Insert（Ins）打开/关闭模块列表界面；Render 分类中左键 Xray 开关、右键调整设置。
Xray 默认保留非矿石方块约 10% 的不透明度；将 opacity 调为 0 可完全隐藏它们。
配置保存在实例的 `meteor-client/` 目录。

Baritone 为可选独立 mod，使用本项目根目录的 `baritone-neoforge-1.21.1.jar`。
本移植支持上述实测版本的 Sodium/Iris Xray；不包含 Fabric addon 加载及 Lithium/Indigo/ViaFabricPlus 兼容层，亦不代表完整移植了所有 Sodium/Iris 兼容功能。
请勿通过原版 Meteor 网站的更新提示换装 Fabric 发行包。

## 构建

在本目录运行 PowerShell：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon shadowJar
.\tools\verify-jar.ps1 -Jar build/libs/meteor-client-neoforge-0.5.8-neoforge.5.jar
.\gradlew.bat --no-daemon sourceBundle
```

源码包保留 `port/meteor-neoforge` 布局及编译用的根目录 Baritone jar，附带对应 Baritone 源码。
Gradle 自动生成编译期 InnerClasses 可见性修补 jar 和内置初始化索引，无需分发 Minecraft jar 或复跑反编译流程。
运行期使用 NeoForge 安装器正常生成的游戏文件；`build/mc-at-fix/minecraft.jar` 只用于 javac。
构建不使用 refmap，不打包 NeoForge 已提供的 SLF4J、JSR-305。

## 复测

```powershell
.\gradlew.bat --no-daemon runClient -PmeteorSelfTest
```

`-PmeteorSelfTest` 默认关闭。启用后只在该开发实例中创建/复用专用的
`meteor-neoforge-selftest` 超平坦单人世界，构建石墙及隐藏矿石，测试 Xray 三种状态、GUI、
压缩包限制、FOV、tooltip 工厂及保存配置。测试结束自动退出。
截图在 `run/client/screenshots/`，文本报告在 `run/client/meteor-selftest.txt`。
必须检查报告和截图；仅 Java 进程退出码为 0 不代表测试通过。

独立安装实例使用官方 NeoForge 21.1.265 安装器安装到 `build/release-instance` 后，运行：

```powershell
.\tools\run-release-test.ps1
.\tools\run-release-test.ps1 -WithBaritone
```

该脚本读取安装器及原版版本 JSON、校验依赖 SHA-1，并只从 `mods` 加载最终 Meteor jar。
不使用开发类目录、编译期 Minecraft 修补 jar、开发依赖注入或 unprotect agent。
脚本内的离线测试身份仅用于本地单人世界。
完整验证范围和限制见 `VALIDATION.md`。

## 源码及许可

Meteor 上游基准：`MeteorDevelopment/meteor-client` v0.5.8，
`37c5c5c640ba9c66721e824c62e84bf1223c9cd6`。保留 GPL-3.0，正文在 `src/main/resources/LICENSE-Meteor`。
第三方许可、NOTICE、来源记录及依赖 POM 一并放入最终 jar 的 `META-INF/licenses/`。
移植修复记录见 `MIXIN-FIXES.md`。未执行提交或推送。
