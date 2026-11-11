# XiaohSense 菜单移植（fix3）

版本：Meteor 0.5.8-neoforge.3 / Minecraft 1.21.1 / NeoForge 21.1.265 / JDK 21。

来源：[NoSetViolin/meteor-client-newui-xh](https://github.com/NoSetViolin/meteor-client-newui-xh)，master 提交 `57f838875e881ae0212f264388f8ec6899d6f50a`。参考仓库面向 Minecraft 26.2；只移植菜单代码和 UI 着色器，按 1.21.1 的 GuiGraphics、鼠标/键盘、NBT 和 OpenGL 接口适配。保留原有 GPL 许可及上游文件头。

## 本轮改动

- 左侧分类、双列模块卡片、全局搜索、收藏、模块设置页、明暗模式、保存、拖动和滚动布局。
- 按用户补充要求：深色底为黑灰，左侧导航单独采样模糊背景，右侧正文完全不透明且使用纯色；分类 active 背景为均匀灰色，无渐变。
- 菜单圆角、阴影、模糊与字体使用独立 GUI 实现。没有切换全局字体，没有修改 Blur 模块及世界渲染器。此为游戏内模糊，不是 Windows Mica 系统材质。
- 默认菜单键从 RightShift 改成 Insert（Ins），命令键和模块绑定保持原值。
- 适配 1.21.1 的事件已使用标志；输入前更新鼠标命中状态，避免初次点击或界面切换后的旧 hover 状态。
- 不导入参考仓库里的任何模块功能、Fabric 加载器或渲染后端。

## 验证

自动测试默认关闭。独立实例从 mods 加载发布 jar，装有 Sodium 0.8.13、Iris 1.8.14-beta.1、Create、Petrolpark 和 Baritone，光影关闭。

| 项目 | 结果 |
| --- | --- |
| Java 21 shadowJar、严格 jar 静态检查 | 通过 |
| 模块/设置初始化 | 171 个模块、1268 项设置 |
| 新菜单布局、灰色分类高亮、左侧模糊/右侧纯色 | 实际截图检查通过 |
| Ctrl+F 搜索 Xray、右键设置、返回模块列表 | 通过自动屏幕输入路径验证 |
| Insert 呼出及关闭、RightShift 不再呼出 | 通过 Minecraft KeyboardHandler 回调验证 |
| 设置开关与明暗切换 | 在测试实例通过 computer-use 鼠标操作；测试值已恢复 |
| 单人世界、Xray 默认透明度/零透明度/关闭后重建 | 回归测试，见随包 smoke 报告与截图 |
| 非 GUI 功能范围 | 相比 fix2，1084 个非 GUI 模块/兼容/全局渲染类字节一致 |

构建、源码打包命令见 README-NEOFORGE.md。独立测试为 `tools/run-release-test.ps1 -WithBaritone`。`-InteractiveUi` 仅为默认关闭的开发预览，自动用 Insert 键盘回调打开菜单后停留，不进入用户存档。

本轮未重新扩大模块兼容范围，上一版已知限制仍见 PATCH-SODIUM-IRIS.md、VALIDATION.md。没有提交或推送 Git，没有修改 Baritone 工程。
