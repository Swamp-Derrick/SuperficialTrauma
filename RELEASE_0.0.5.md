# Superficial Trauma 0.0.5

Minecraft **1.21.1** · NeoForge **21.1.252** · Java **21**

这是开发中的测试版本，面向新的 NeoForge 1.21.1 游戏环境；不支持将 Forge 1.20.1 旧世界直接升级到本版。

## 本次更新

- 加入 Simple Voice Chat 倒地联动：倒地后保留 3 秒发言时间，随后禁言，并禁止打开 SVC 菜单；恢复行动后恢复原有设置。
- 倒地前 10 秒保持清晰收听，之后其他玩家语音变弱、变闷并带短回音；停搏、室颤与脑死亡时进一步减弱。
- 倒地满 10 秒后，外界脚步、环境声、战斗声等也有闷声与回音；医疗、治疗、体征提示和 GUI 音效保持清晰。环境音处理不依赖 SVC。
- 加强闷声效果，平滑处理恢复过程，修复音频设备重载时的声源释放问题。
- 加入新的 ST 方形资源包 / 仓库图标，保留游戏模组页面的原横幅；更新兼容性基线和语音 / 环境音说明。

## 安装

下载 `superficialtrauma-neoforge-1.21.1-0.0.5.jar`，在服务器及参与游戏的客户端安装同一版本，并移出旧版 Superficial Trauma JAR，避免重复加载。更新前备份存档。

SVC 与枪械模组均为可选依赖，不包含在本下载中。语音功能的已验证基线为 **Simple Voice Chat 1.21.1-2.6.24**，需照常配置其网络连接；枪械基线为 **CGM 1.4.4、Framework 0.13.11、NineZero 1.5.0-port.1+1.21.1**。

## 检查范围与已知限制

- 17 项 JUnit 回归、独立服务器以及客户端音频 / 菜单检查；同时覆盖不安装可选模组的运行路径。
- 用户已确认最终语音和环境音的实际游戏听感良好；自动合成音频检查不替代所有麦克风模式、群组及高延迟情况下的多人测试。
- 其他音频滤镜模组尚未专门验证；不支持 OpenAL EFX 的设备会退回仅音量减弱。
- 斜向拖拽尸体上岸等既有边缘问题仍待进一步多人验证。

完整说明：[兼容性基线](https://github.com/Swamp-Derrick/SuperficialTrauma/blob/v0.0.5/COMPATIBILITY.md) · [倒地听觉说明](https://github.com/Swamp-Derrick/SuperficialTrauma/blob/v0.0.5/VOICECHAT_COMPATIBILITY.md)
