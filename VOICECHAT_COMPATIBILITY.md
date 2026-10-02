# 倒地听觉：Simple Voice Chat 与环境音

版本：Superficial Trauma **0.0.5**。基线：Minecraft 1.21.1、NeoForge 21.1.252、Java 21、SVC `1.21.1-2.6.24`，2026-10-03。

## 玩家体验

| 状态 | 自己说话 | 听见其他玩家 | SVC 菜单 |
|---|---|---|---|
| 清醒 | 保持原设置 | 原音 | 可以打开 |
| 倒地不足 3 秒 | 保留说完最后一句话的时间 | 清晰 | 禁止打开，已经打开的会关闭 |
| 倒地满 3 秒、不足 10 秒 | 禁言 | 清晰 | 禁止打开 |
| 倒地满 10 秒 | 禁言 | 音量减弱、闷声、短回音 | 禁止打开 |
| 心搏停止 / 室颤 / 脑死亡 | 沿用同一次倒地的 3 秒宽限，不重新计时 | 进一步变弱、变闷 | 禁止打开 |
| 苏醒期但尚未起身 | 仍禁言 | 按同一次倒地的累计时间处理 | 禁止打开 |
| 起身恢复行动 / 正常重生 | 恢复原设置 | 平滑恢复原音 | 恢复可用 |

- 时间依据游戏 tick：3 秒为 60 tick，10 秒为 200 tick。停搏听觉状态优先于普通倒地的十秒门槛。
- 服务器在 SVC 分发麦克风数据之前拒绝倒地者发言，客户端也停止提交语音；不依赖按键绑定，覆盖按键、常开、声控，以及轻声/群组发言。
- 不强行开启麦克风，不修改玩家原有静音、停用、输入方式、音量或音频设备设置。恢复行动不代表强行取消玩家自己的静音。
- 语音处理作用于倒地者本地收到的 SVC 声音，覆盖实体、位置、静态（如群组）通道；环境音另走 Minecraft 音频通道，不重复处理 SVC。健康玩家听到的声音不变。
- 禁用 SVC 自有菜单（设置、主菜单、群组、设备选择、引导等）；普通 Minecraft 暂停/设置界面不在这次限制范围内。
- 退出、重生、服务端关闭会清理语音状态；读取已有倒地存档时按保存的倒地时间计算，不重新赠送发言宽限。

## 加强后的语音参数

| 收听状态 | 基准增益 | 四级低通单级截止频率 | 短回音 |
|---|---:|---:|---|
| 正常 / 前十秒 | 1.00 | 不处理，原样通过 | 无 |
| 普通倒地十秒后 | 0.55 | 600 Hz | 160 ms × 0.30、290 ms × 0.12 |
| 停搏 / 室颤 / 脑死亡 | 0.22 | 400 Hz | 同上 |

这里的增益不是最终响度百分比：滤波、回音、SVC 距离和玩家音量设置共同影响听感。
相较首版的两级 1200/700 Hz 低通，现在更强地削弱高频。持续发言中跨越状态门槛时仍平滑调整增益和滤波，约 0.75 秒音频完成 95% 过渡；已经倒地十秒后的新语音片段直接按当前闷声强度开始，不再每句话都重新从原声渐变。
每个说话者/通道有独立缓冲；结束发言、长时间断流、断开语音连接会清理缓冲，避免把上一段声音带到下一段。
回音在连续语音流内产生，不额外延长 SVC 的结束发言标记或创建新的声音来源。

## 环境音

所有倒地阶段均在连续倒地满十秒后启用环境音处理；已在播放的持续声音和新声音都有效，苏醒起身后平滑恢复。

| 声音 | 是否处理 |
|---|---|
| 脚步、方块、环境、水声、天气、生物、战斗、爆炸、经 Minecraft 声音引擎播放的 CGM 枪声 | 是 |
| 唱片等位置相关的流式声音 | 是 |
| 本模组治疗、用药、除颤、CPR、辅助呼吸、尸检和 QTE 音效 | 否 |
| 本人心跳、沉重呼吸等体征提示 | 否 |
| GUI / 菜单音、非位置相关的 MASTER 提示、背景音乐与 VOICE 类提示 | 否 |

判定声音来源而非当前是否打开 GUI；打开医疗面板不会让外面的枪声恢复清晰。当前本模组所有已注册声音均属于医疗或体征提示，因此整个 `superficialtrauma` 声音命名空间排除处理；未来若新增外界声源，需要相应细分此规则。

环境音使用 Minecraft 自己的 OpenAL EFX 低通和回音通道，正常倒地增益 0.55、高频增益 0.015；停搏后增益 0.22、高频增益 0.003。回音延迟 160 ms，第二延迟相隔 130 ms，反馈 0.20，阻尼 0.80，回音通道增益 0.30。每 tick 向目标插值 20%，避免突然切换。保持原有距离、方位、用户音量和循环逻辑，不复制播放原声音。

这部分不依赖 SVC。音频设备重载时释放并重建滤镜资源；通道释放与停止声音按同一音频线程的队列顺序执行，避免重载时停止已释放声源。不支持 EFX 时保留音量减弱并记录警告，不因此关闭整个声音系统。实现参照 [LWJGL OpenAL EFX API](https://javadoc.lwjgl.org/org/lwjgl/openal/EXTEfx.html)，只处理 Minecraft 的声音上下文，不修改 SVC 的独立上下文。其他改写原生音频滤镜的声学模组尚未专门验证。

## 安装与可选依赖

服务器和参与测试的客户端都安装新版 Superficial Trauma 与 SVC，照常配置 SVC 网络连接。无需更改 SVC 配置文件来启用本功能。
SVC 不存在时，Superficial Trauma 仍可独立加载。构建只引用 `voicechat-api:2.6.24`，不将 SVC 或其 API 打进模组 JAR。

使用官方 [插件注册 API](https://modrepo.de/minecraft/voicechat/api/getting_started)、[服务端麦克风事件](https://voicechat.modrepo.de/de/maxhenkel/voicechat/api/events/MicrophonePacketEvent.html) 和 [客户端接收音频事件](https://voicechat.modrepo.de/de/maxhenkel/voicechat/api/events/ClientReceiveSoundEvent.html)。音频线程只读取不可变状态快照，不直接访问 Minecraft 世界或可变的玩家健康数据。

## 自动检查与手动复测

自动检查涵盖：

- 3 秒、10 秒精确边界；停搏、室颤、脑死亡、苏醒期、起身和再次倒地；连续倒地中刷新姿态不会重置宽限。
- 原音直通、音量递减、高频抑制、跨数据包回音、渐变恢复、不同玩家/通道隔离、缓冲数量上限与清理。
- 使用实际 SVC 插件发现和事件分发：服务端拒绝模拟麦克风事件，正常玩家和恢复后的玩家不受影响。
- 使用实际 SVC 客户端回调处理合成 PCM，验证普通/轻声麦克风阻断与实体/位置/静态收听处理。
- 隔离客户端世界中的实际倒地计时、菜单拦截、已打开菜单关闭、停搏降音、恢复行动解锁；同时加载提供的 CGM、Framework、NineZero。
- 原生音频检查：现有/新开始的普通音效和流式音效、治疗/GUI 排除、十秒门槛、停搏、反复更新音量不叠乘、音频上下文重载、恢复与 OpenAL 错误检查。

这些自动检查不是两名真实玩家的语音对话测试。测试客户端关闭真实语音输入输出，向回调注入合成音频。用户已对实际游戏中的语音回音、加强后的闷声及环境音效果反馈“效果很好”；该主观验收单独记录，不等同于已穷举不同麦克风模式、群组、网络延迟下的多人测试。

首版通过 12 项 JUnit、带/不带 SVC 的独立服务器和 SVC 客户端检查。本次扩展通过 17 项 JUnit、独立服务器回归、带 SVC / 枪械基线的环境音客户端检查，以及不装可选模组的客户端连续三次音频重载检查；最终复测未出现 OpenAL 错误。功能检查日志为 `build/world-audio-final-build.log`、`build/world-audio-client.log`、`build/world-audio-standalone-client.log`。这些是自动检查证据，实际听感反馈见上一段。
CGM 自带的 `cgm:sounds/SOUND-LICENSE.txt` 非法资源路径警告仍存在，与本次语音功能无关。

发布产物：`build/libs/superficialtrauma-neoforge-1.21.1-0.0.5.jar`，校验值随 GitHub Release 附件提供。分发包不包含第三方 API/JAR 或开发测试类。

0.0.5 发布复核在独立开发目录重新执行构建、17 项 JUnit、无可选模组 / 完整兼容组合的 GameTest 服务器、SVC 客户端及环境音客户端检查。日志为 `build/release-0.0.5-build.log`、`build/release-0.0.5-compat-server.log`、`build/release-0.0.5-svc-client.log`、`build/release-0.0.5-world-client.log`；未改动用户实际测试实例或存档。

建议双人复测顺序：正常通话 → 击倒并继续说话 → 3 秒后旁人听不到倒地者 → 倒地者十秒后听见闷声/回音 → 停搏更弱 → 救起后恢复。分别测试按键、声控、常开、群组，并在倒地前后尝试打开 SVC 菜单。

### 开发复现

PowerShell，Java 21，仓库目录中执行：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true -Penable_voicechat_mod=true '-Pvoicechat_mod_jar=E:\.minecraft\versions\1.21.1 neoforge test\mods\[简单的语音聊天] voicechat-neoforge-1.21.1-2.6.24.jar'
.\gradlew.bat runClient -Penable_migration_gametests=true -Penable_voicechat_client_smoke=true -Penable_voicechat_mod=true '-Pvoicechat_mod_jar=E:\.minecraft\versions\1.21.1 neoforge test\mods\[简单的语音聊天] voicechat-neoforge-1.21.1-2.6.24.jar'
.\gradlew.bat runClient -Penable_migration_gametests=true -Penable_world_audio_client_smoke=true
```

可追加 `-Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'` 一起加载枪械兼容基线。
客户端 smoke 使用 `run-1.21.1` 内的新测试世界，运行前在该开发目录的 `config/voicechat/voicechat-client.properties` 中设置 `disabled=true`，避免使用真实麦克风。测试代码、测试世界与外部模组都不进入分发 JAR。
