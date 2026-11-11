# 品牌图与双行品牌文字（fix4）

Meteor 0.5.8-neoforge.4，Minecraft 1.21.1 / NeoForge 21.1.265。

- 左上角改为用户提供的 XH 图片，原始 JPG 逐字节复制至 `assets/meteor-client/textures/icons/gui/xiaohsense.jpg`，无重绘或裁剪；沿用 30 × 30 的设计尺寸和位置。
- 右侧第一行 `XiaohSense` 使用独立粗体字体并略微压缩宽度；第二行 `KENNYZ.CN` 使用同字体家族的常规字重，字号为第一行 50%，颜色使用较淡的副文字色。
- 两行排版高度为 18 + 2 + 9 = 29 个设计单位，置于 logo 的 30 个单位高度中；上下各保留 0.5 单位边距。
- 删除品牌区原有的单独 CLIENT WORKSPACE 标签。模块列表及模块设置页共用同一品牌组件。
- 不改变导航模糊、正文纯色、灰色分类高亮、Ins 快捷键及任何模块功能。

运行检查：独立 NeoForge 实例打开菜单和设置页，检查截图中的 logo 和双行品牌位置；菜单搜索、返回和 Insert 键路径通过。`tools/run-release-test.ps1 -WithBaritone -UiOnly` 可复测，开发自检默认关闭。

图片为用户提供素材；正文及原有移植文件的 GPL 与第三方许可保持原样。字体优先使用系统 Segoe UI / Inter / Noto Sans / Arial 的粗体和常规字重，不分发系统字体文件；缺少粗体时仅品牌名称使用双绘制回退。
