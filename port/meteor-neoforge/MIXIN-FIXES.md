# NeoForge 21.1.265 修复记录

所有最终 mixin 配置保持 `defaultRequire=1`。未以禁用模块或静默放宽注入代替修复。

| 位置 | 原因 | 修复及验证方式 |
|---|---|---|
| DrawContextMixin | NeoForge tooltip 方法不再调用 Optional.ifPresent | 移除旧注入，改为六个原生 tooltip 工厂；运行时检查工厂并渲染 |
| TitleScreenMixin | NeoForge 改写标题文字渲染 | 一次性检查移到 render TAIL；主菜单验证 |
| EntityMixin 水流 | 实际计算移到无参数重载 | 目标改为 updateFluidHeightAndDoFluidPushing()V；静态指令检查及单人加载 |
| ClientPlayerInteractionManagerMixin | 挖掘 lambda 增加 LeftClickBlock 参数 | 对齐实际 descriptor；静态检查及单人加载 |
| 五个 Accessor | 隐式字段名仍按 Yarn 推断 | 显式 validBlocks、items、root、ownerUUID、entityTarget；静态字段检查及运行加载 |
| LivingEntityMixin（fix1） | PotionSpoof 关闭时仍解引用未绑定的 NeoForge 效果 Holder，触发 Petrolpark midas_touch 崩溃 | 原结果为 false、模块未开启或 Holder 未绑定时保留原查询结果；真实 hasEffect 路径回归及 Petrolpark/Create 同装测试 |
| Sodium 兼容（fix2） | 原版模型钩子被独立区块渲染器绕过 | 补齐模型取消、矿石面遮挡、照明与流体四个可选 mixin；Sodium/Iris 关闭和开启光影，以及无 Sodium 三种组合实测 |
| Iris 状态（fix2） | 原占位实现恒 false | 惰性解析公共 IrisApi，实际查询光影管线状态；不打包 Iris 类 |
| BlockModelRendererMixin | 区块渲染实际调用 NeoForge 带 ModelData/RenderType 的重载 | 将透明度/取消渲染钩子移到实际 AO 和非 AO 重载，ThreadLocal 默认 -1；Xray 三状态画面对照 |
| SectionCompilerMixin（新增） | NeoForge 使用 BakedModel.getRenderTypes，绕过原版渲染层选择 | 将半透明的非矿石方块放入 translucent 层；Xray 默认 opacity=25 实测 |
| CompressionDecoderMixin（新增） | 原 Fabric ASM 已删除 | 激活时只提升上限比较值，继续正常解压；保留阈值和损坏数据异常 |
| GameRendererMixin / FOV Invoker | 原基础 FOV ASM 已删除 | 在基础配置 FOV 存储点及 panoramic 常量投递事件，保留后续动态计算和手部 FOV |
| MixinPlugin / Platform | 早期 ModList 未就绪，可选 Baritone 错误探测 | 早期使用 LoadingModList，Baritone mixin 按实际 mod 存在性选择 |
| BaritonePathManager / ClientChunkMapAccessor | 正式实例无开发 unprotect，私有 Rotation 字段不可访问；区块 getter 与 Baritone 重名 | 使用目标类 privateLookupIn；为 Meteor 区块 getter 加前缀；不修改 Baritone 工程或 jar |
| 初始化 | 入口与构造尾部重复调用，Reflections 不能可靠扫描 union URL | 单一构造完成入口；构建生成 27 个 PreInit/PostInit 方法索引，仍按注解依赖顺序执行 |
| 打包 | 重复打包 SLF4J/JSR-305 与 NeoForge 模块冲突 | 从 shaded 配置排除；正式安装实例检测 |
| 编译 | 旧版手工 mc-at-fixed.jar 不可复现 | 从 265 的实际 AT jar 自动生成 InnerClasses 修补副本，仅编译时使用 |

辅助静态扫描器：`../meteor-remap/AuditMixins.java`。其结果只检查选择器/字段/直接指令，
不代表局部变量捕获、间接调用路径或画面效果正确；最终仍以正式实例的日志及截图为准。
