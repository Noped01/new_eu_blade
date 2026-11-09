# 质子荧光 0.2.0（开发测试版）

Minecraft 1.21.1 / NeoForge 附属，新增 `proton_fluorescence:proton_blade`。直接继承重锋的原生拔刀剑，接入 SI 电池与工具模式接口。

## 前置与兼容范围

| 项目 | 本次核对版本 |
| --- | --- |
| Minecraft / Java | 1.21.1 / Java 21 |
| NeoForge | 构建使用 21.1.262；声明支持 21.1.228–21.1.x |
| SlashBlade: Resharpened | 2.1.9-1.21.1 |
| Singularity Iteration（mio_icif） | 0.1.11.1 |

另需这些模组自己声明的前置。本附属不要求安装 HF Blade、SI 拔刀剑联动、日系附属或 Legacy Compatibility；前三者只提供实现参考。没有 Mixin、覆盖补丁、旧版构造器注入或上游类打包。对全局伤害标签仅追加本附属的伤害类型，不覆盖原标签。

本附属不修复其他模组彼此之间的版本冲突。例如，本机 SI 联动 dev.10 与重锋 2.1.9 的旧构造器问题仍属于那两个版本自身的问题。

**当前完成编译、逻辑、原生碰撞几何与接口检查，尚未启动测试世界。不能据此保证完整整合包内没有冲突。**

## 专属 SE：荧光刃界

本附属独立注册 `proton_fluorescence:proton_edge`，同时实现剑气和额外真实伤害，不挂用日系附属的 SE。

| 玩家经验等级 | 关闭 | 开启 |
| --- | --- | --- |
| 低于 35 级 | 无附加剑气，无额外真实伤害 | 普通剑气，额外 50% 真实伤害 |
| 35 级及以上 | 普通剑气，额外 50% 真实伤害 | 有效锁定时将剑气移至目标附近；无锁定时普通发射。额外 100% 真实伤害 |

35 级计入高等级档。近战和来源可确认的重锋投射物都能触发额外伤害，包括本 SE 的剑气。效果要求本刀仍有 EU。经验等级与开关按命中时的来源刀状态判断。

额外比例按护甲减伤前、扣除盾牌已格挡部分的伤害计算。例如原伤害 100 点，额外 50% 就追加 50 点真实伤害；高等级开启时追加 100 点。真实伤害使用独立伤害类型，参考 SI 先进速子武器的绕甲标签，通过正常 hurt 流程结算，不直接改血，不绕过其他模组主动取消伤害的处理。

### 锁定剑气

沿用重锋的原生 Drive 剑气，不调用逐目标强制命中接口，也不取消原本的斩击。保留原生碰撞和伤害处理，不把原来的范围或穿透行为主动改成单体结算。

锁定传送后的虚空剑气速度为 **0.15 格/刻**（普通剑气 1.5 的十分之一），寿命参数为 **10 刻，正常 20 TPS 下约 0.5 秒**。只调整虚空剑气的运动与寿命，不增加“命中一次即消失”的限制；普通剑气保持原参数。

剑气放在锁定目标碰撞盒的边缘外侧，而非中心：原生射线检测从盒内起步可能判不到交点。已用 Minecraft 实际 AABB.clip 验证不同大小目标、正反向与斜向的第一步交点。方块阻挡、目标筛选与移动目标命中仍需进游戏核对。

投射物生成时记录来源刀 UUID 与攻击者 UUID，随实体数据保存。切换武器不会改绑到当前主手。找不到原刀、旧投射物没有来源记录时不追加伤害。当前能查找持有人和在线玩家随身物品、已加载的掉落物；放入容器或卸载区域的来源刀不授予追加效果。

## 开关、EU、外观

- 使用 SI 量子剑同款 `IToolModeProvider` / `ToggleableElectricTool` 接口，由 SI 工具开关键切换（默认 **G**，以个人键位为准），也支持 SI 装备控制台。不新增抢占 HF 按键的监听。
- 开启并主手持有时，通过重锋公开接口维持 SSS，并启用力量效果对应的原生强化剑技；不额外挂力量药水。
- 默认参数暂沿用此前草稿：**1 亿 EU、T5、32768 EU/次传输、1280 耐久、基础攻击参数 1024**。这组数值是占位平衡，不是用户已定稿的数值。
- SI EU 电池是唯一储能。沿用 SI 联动刀的电量与磨损映射、不可对外供电策略；使用重锋自身的断刀处理。SI 自己提供 FE 充电视图，本附属不重复注册 FE 能力。
- 默认启动费 1000 EU；开启且选中时 10 EU/t，原生命中事件另扣 10 EU。重锋原有耐久磨损会折算 EU。切出主手暂停待机费与 SSS 维持。
- SI 能量背包充电使用 SI 现成接口。量子头盔太阳能仍取决于 SI 的太阳能核心、随身装备充电开关及发电条件；本刀通过 IBatteryItem 接收，不额外创造能量。
- 提供 SI 武器库 `ArmoryLink("mainhand")`；玩家配对、权限、距离等仍按 SI 的规则，支持情况待游戏实测。
- 血腥革新的模型和贴图已按用户要求复制、重命名到本附属命名空间，仅作占位。原资产来源及条款见 THIRD-PARTY-NOTICES.md / LICENSE-METS.txt。默认开启和关闭的刀刃斩击颜色均为 **红色 #FF0000**。
- 物品稀有度为 **史诗（EPIC）**，默认名称为红色 **「质子荧光」**。红名通过本物品的名称样式实现，不新增或修改全局稀有度。

## 获取与测试

安装 `proton-fluorescence-0.2.0-neoforge-1.21.1.jar` 到测试实例；多人游戏两端均需安装。本次没有改动实际游戏目录。

```mcfunction
/give @s proton_fluorescence:proton_blade
```

本版暂不提供生存配方：先前“血腥革新 + 下界之星 + 铜锭”的配方是草稿自定，且会引入 SI 联动前置，已移除。

请在测试世界用 `/experience set @s 34 levels` 和 `/experience set @s 35 levels` 分别检查关闭/开启四种状态，锁定与不锁定各测一次。锁定测试应同时放置多个目标，确认范围、碰撞和额外伤害没有变化。再核对 SI 充电、G 开关、SSS、保存重进和切换刀后旧剑气的来源。

不要沿用 0.1.x 的测试结论或配置。0.1.x 的 off/on SA、SE 和外观路径引用了其他附属；0.2.0 使用自己的路径。已有测试世界请先另存旧配置，再让新版本生成默认配置。

## 后续 SA / SE 接口

本版没有新写 SA。开启和关闭暂均为本体 `slashblade:judgement_cut`；错误 SA ID 也回退到它。两个模式可分别在 `serverconfig/proton_fluorescence-server.toml` 的 `offSA` / `onSA` 指定已注册剑技。

Java 接口位于 `dev.protonblade.api`：

```java
ProtonBladeApi.setSlashArt(BladeMode.OFF, ResourceLocation.parse("yourmod:off_art"));
ProtonBladeApi.setSlashArt(BladeMode.ON, ResourceLocation.parse("yourmod:on_art"));
```

还提供 `setSpecialEffects`、`mode`、`edgeRules`、`isEffectActive`，以及服务端 `BladeModeEvents.Change` / `Tick` / `Hit`。`setPowered` 是服务端免费管理接口，玩家切换走 SI 的 `toggleActive` 扣启动费。SE 列表只改本刀，其他游戏内附加 SE 保留；自定义替换列表时需保留希望使用的 `proton_edge` 与开启模式下的 `proton_resonance`。

## 离线构建

需要 Python 3.11+、JDK 21，以及已安装的命名 NeoForge 1.21.1 运行库和上述两个模组。

```powershell
python configure_runtime.py --minecraft-dir 'D:/游戏/.minecraft' --version '实例目录名' --mods-dir 'D:/游戏/.minecraft/versions/实例/mods' --jdk 'D:/工具/jdk-21'
python build.py
python check.py
python release.py
```

`local-runtime.json` 只记录本机路径，不打入源码包。构建与检查拒绝 HF、SI 联动、日系附属和 Legacy Compatibility 出现在类路径中。检查记录在 `build/verification.json`，明确标记游戏启动和游戏内测试尚未完成。
