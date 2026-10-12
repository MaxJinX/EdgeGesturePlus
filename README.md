# EdgeGesturePlus

> **本仓库是 [helloklf/EdgeGesture](https://github.com/helloklf/EdgeGesture) 的修改版（fork），不是原项目。**
>
> - 原作者：**kuanglinfei**（GitHub [@helloklf](https://github.com/helloklf)）
> - 基线：上游 `master` 分支提交 `fca8d3a`
> - 修改日期：**2026-10-12**
> - 许可证：**GNU GPL-3.0**（沿用上游，见 [LICENSE](LICENSE)）

一个轻量的 Android 边缘手势应用（基于无障碍服务实现）。
本仓库在原版基础上做了功能裁剪、功能新增与构建修复，**具体改动见下方「相对原版的修改」**。

---

## 相对原版的修改

### 1. 移除「临时停用 5 秒手势」（防误触机制）

原版在左右两侧边缘手势上**连续两次**做出无效的垂直滑动后，会把该侧手势窗口设为
`FLAG_NOT_TOUCHABLE` 并弹出「边缘手势将停用5秒~」，5 秒内完全不响应触摸，之后靠
`ACTION_OUTSIDE` 恢复。

本版本移除了这套机制：无效手势本身照旧被丢弃，但**不再触发停用**。

- `ui/TouchBarView.java`：删除 `onInValidGesture()`、`antiTouchMode()`、`antiTouchModeOn`、
  `setAntiTouchModeToggle()`、`voluntarilyCancelTime`；两处调用点改为 `clearEffect()`。
- `ui/SideGestureBar.java`：删除 `setLeftView()` / `setRightView()` 中注册该回调的整段代码
  （含那句 Toast 与窗口 flag 改写）。

### 2. 新增「隐藏 MIUI 导航栏」（**需要 ROOT**）

设置 → 其它设置 中新增开关，通过 ROOT 常驻 shell 写入 MIUI 的两个 global 设置项：

| 设置项 | 值 | 作用 |
| --- | --- | --- |
| `force_fsg_nav_bar` | `1` | 切到全面屏手势模式，隐藏三键导航栏 |
| `hide_gesture_line` | `1` | 隐藏底部手势提示线（小白条） |

- 首次开启时会**备份**两个设置项的原始值，关闭开关时精确还原（原本不存在的项用
  `settings delete` 删除，不会污染原有导航方式）。
- **开机自动重施**：MIUI 会在开机启动桌面时把 `hide_gesture_line` 重置回默认值，
  因此在无障碍服务的 `onServiceConnected()` 中延迟 3s / 18s / 58s 重试三次。
- 核心实现见 `util/MiuiNavBar.java`。

> ⚠️ `force_fsg_nav_bar=1` 会让 MIUI 自带的侧边返回手势同时生效，可能与边缘手势热区冲突。
> 若觉得别扭，可改用沉浸式隐藏（`policy_control immersive.navigation=*`）。

### 3. 构建修复

- 根 `build.gradle`：`jcenter()` 已停止服务，改为 `mavenCentral()` 并前置阿里云镜像。
- `app/build.gradle`：原签名配置指向一个并不存在的文件（`D:/omoarea.jks`），且 `release`
  buildType 并未引用它；现改为从 `keystore.properties` 读取（见「构建」一节）。
- 版本号 `versionCode` 提升到 `105`、`versionName` 为 `1.6.4-plus`，便于与原版区分。

---

## 安装须知

⚠️ **本仓库产出的 APK 使用独立签名，与原版不同，无法覆盖安装。**

安装前请先**卸载已安装的 EdgeGesture**（会清除应用设置与手势配置），再安装本版本。

---

## 构建

环境要求：

- **JDK 11**（本项目使用 Gradle 6.5，只支持 Java 8~14，用 JDK 17 会直接构建失败）
- Android SDK：`platforms;android-29`、`build-tools;30.0.2`、`platform-tools`

```bash
# 1. 指定 Android SDK 路径
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 2. 编译（未签名包）
./gradlew assembleRelease

# 3. 如需签名，先创建 keystore.properties
cp keystore.properties.example keystore.properties
# 填入密钥路径与口令后，再执行 build
```

**签名是可选的**：若 `keystore.properties` 不存在，构建会自动跳过签名并产出未签名的
APK，不会失败。该文件已被 `.gitignore` 忽略，密钥与口令不会进入仓库。

---

## 许可证

本项目遵循 **GNU General Public License v3.0**，完整条款见 [LICENSE](LICENSE)。

- 原始项目版权归原作者 **kuanglinfei** 所有。
- 本仓库作为衍生作品，同样以 GPL-3.0 授权；对本仓库的修改部分版权归 **MaxJinX** 所有。
- 依据 GPL-3.0，任何人都可以自由获取、使用、修改和再分发本项目的源代码。

---

## 原版更新日志（上游 README 内容，保留以存史）

【1.2.0】
- 增加横竖屏使用小白条的开关，以及动作和外观自定义选项
- emmm，越来越臃肿了~

【1.1.10】
- 增加横屏时手势区域更换成小白条选项

【1.1.9】
- 横屏时缩小底部手势区域降低游戏误触几率

【1.1.8】
- 增加三星OneUI独有的优化提示

【1.1.7 更新日志】
- 修复了发现的问题
【1.1.6更新日志】

- 大幅（35%~90%）降低内存占用



【1.1.5更新日志】

- 震动反馈调节加入，根据自己的喜好调节震动强度，把马达耍起来吧！



【1.1.4 更新日志】

- 修复 底部热区高度调为0会遮挡全屏的问题

- 修复 手势动作设置项错位

- 增加悬停手势触发时长选项



【1.1.3 更新日志】

- 诈尸的一天，增加热区灵敏度

- 允许调整底部热区宽度

- 进入设置界面时热区高亮显示



【1.1.2 更新日志】

- 修复了一些动画细节问题。emmm，似乎对于一款追求轻量的软件来说，差不多可以结束更新了，如果没有重大bug出现的话



【1.1.0 更新日志】

- 终于开始做自定义选项了，时间仓促 界面和细节略粗糙，见谅~

- 为了方便部分用户锁定后台，改为默认显示桌面图标





【1.0.6 极致精简版 更新日志】

- 修复在部分设备上偶现的底部手势热区变大

- 优化悬停手势震动反馈



【1.0.5 更新日志】

- 修复在5.0系统上启动失败


【1.0.4 更新日志】

- 大幅压缩软件体积



【1.0.3 更新日志】

- 为了方便大家锁定后台，增加一个切换启动图标的选项（默认还是不显示启动图标，从辅助服务界面的设置进去，不懂的可以看截图）



【1.0.2 更新日志】

- 修复手势动画顶部被截断的问题



【1.0.1 更新日志】

- 降低系统版本要求到 Android 5.0

- 优化手势震动为 仅滑动悬停才震动
