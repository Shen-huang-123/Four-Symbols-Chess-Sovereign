# 四象棋尊 v2.9 · 模式选择页独立重构

- 本次仅修改模式选择页，手机竖屏、横屏和平板分别布局，三张卡片正常高度比原版减少 30%，增加原生图标、轻按回弹和确认反馈。
- 恢复左上返回，沿用原同屏、创建、加入和返回流程；其他 13 个生产文件哈希一致。
- 九组模拟尺寸各 109 项检查通过，含真实开局、返回保留棋局、创建入口和加入地址弹窗。未替代荣耀 WIN 真机及多机网络验收。
- [实际文件、布局公式与验收](https://github.com/Shen-huang-123/Four-Symbols-Chess-Sovereign/blob/main/%E6%BA%90%E7%A0%81/%E5%9B%9B%E8%B1%A1%E6%A3%8B%E5%B0%8A/%E9%AA%8C%E8%AF%81/v2.9-%E6%A8%A1%E5%BC%8F%E9%A1%B5%E9%87%8D%E6%9E%84%E8%AF%B4%E6%98%8E.md)；[缺少的透明立绘规格](https://github.com/Shen-huang-123/Four-Symbols-Chess-Sovereign/blob/main/%E6%BA%90%E7%A0%81/%E5%9B%9B%E8%B1%A1%E6%A3%8B%E5%B0%8A/%E9%AA%8C%E8%AF%81/v2.9-%E7%B4%A0%E6%9D%90%E9%9C%80%E6%B1%82.md)。立绘生成遇网络错误，目前使用原分辨率头像圆章，未实现参考图精度的半身插画。

## 安装与校验

- APK：`四象棋尊-v2.9.apk`
- 包名：`com.dorm.fourchess`
- APK 内版本：`2.9.0`（versionCode `29`）
- SHA256：`ed331cc0390046143a0dadcbf2ecaaafcf28eca57aba45b95f620d0d9e531fca`
- 本版为历史版本归档；推荐使用 v4.3。

## 历史源码说明

本地保留了该版本 APK，但没有可验证的完整源码快照。本标签仅保存发布说明与校验文件；GitHub 自动生成的 Source code 压缩包是归档元数据，不包含该版本游戏源码。完整源码请查看 v3.3 或最新版 v4.3。
