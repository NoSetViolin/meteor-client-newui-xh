# Freecam fix6

适用：Minecraft 1.21.1 / NeoForge 21.1.265。

## 原因与修复

NeoForge Camera.setup 普通视角通过 ComputeCameraAngles 事件调用 setRotation(FFF)。旧 CameraMixin 仅拦截 setRotation(FF)，该调用仍存在于睡眠分支，因此严格 mixin 检查能通过，但普通视角不采用 Freecam 角度。

修复在 NeoForge 游戏事件总线上注册 ComputeCameraAngles 监听器，仅在 Freecam 启用时设置插值后的 yaw/pitch。关闭时不改变事件，也不改变其他模组或菜单 UI。保留原鼠标灵敏度、反转和俯仰限制逻辑。

## 验证方式

默认关闭的 meteor.selftest 开关，在独立 release-instance 的专用单人存档中执行：通过 MouseHandler.turnPlayer 的实际输入处理链输入鼠标增量，检查 Freecam yaw/pitch、玩家身体角度保持不变、Camera.setup 的实际结果和关闭后的正常转向。测试实例复制当前用户实例的模组集合，未复制用户存档或改写用户配置。

旧版本测试已复现：Freecam 内部 yaw +12°、pitch +6°，但 Camera 结果不匹配（before-selftest.txt）。最终运行结果见 after-selftest.txt。

附带执行菜单、Xray、FOV、tooltip 和 Baritone 原有冒烟测试；不宣称所有模组组合与功能均已验证。

## 构建

JDK 21，工作目录 port/meteor-neoforge：

    .\gradlew.bat --no-daemon shadowJar sourceBundle
    .	oolserify-jar.ps1 -Jar .uild\libs\meteor-client-neoforge-0.5.8-neoforge.6.jar
    .	oolsun-release-test.ps1 -WithBaritone

未提交或推送 Git。
