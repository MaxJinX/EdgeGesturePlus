【Plus】
feat: 移除临时停用5秒手势 + 新增隐藏 MIUI 导航栏功能
1. 移除「临时停用5秒手势 / 防误触 antiTouchMode」
   - TouchBarView: 删除 onInValidGesture() / antiTouchMode() / antiTouchModeOn /
     setAntiTouchModeToggle() / voluntarilyCancelTime, 无效手势不再触发停用
   - SideGestureBar: 删除 setLeftView() / setRightView() 中注册该回调的整段代码
     (含 "边缘手势将停用5秒~" Toast 与 FLAG_NOT_TOUCHABLE 窗口改写)

2. 新增「隐藏 MIUI 导航栏」(需 ROOT), 设置 → 其它设置 中开关
   - 新增 util/MiuiNavBar.java: 通过 ROOT shell 写入
     force_fsg_nav_bar=1 + hide_gesture_line=1, 首次开启备份原值, 关闭时精确还原
   - 开机自动重施 (AccessibilityServiceGesture.onServiceConnected 中 3s/18s/58s 重试三次),
     绕过 MIUI 启动 launcher 时重置 hide_gesture_line 的行为
   - SystemProperty 增加 isMiui() 检测; SpfConfig 增加配置与备份项

3. 构建修复 (jcenter 已停服, 原配置无法编译)
   - 根 build.gradle: jcenter() → mavenCentral(), 前置阿里云镜像
   - app/build.gradle: 签名 keystore 由不存在的 D:/omoarea.jks 改为 ../omarea.jks,
     并为 release buildType 补上 signingConfig
