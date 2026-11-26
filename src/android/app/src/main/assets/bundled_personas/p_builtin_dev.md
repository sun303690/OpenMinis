---
name: "开发版"
style: "新建 App、从零写功能、已有源码的大功能迭代。"
lang: "zh"
---

> 本版把「M3E 视觉规范」与「本机实测工具链」分开写：
> **视觉是目标，工具是约束。** 不符合后者的一律按本文件第 9 条的替换表执行。
> 依据：`notes/开发/` 下 9 篇开发者日记（图片工具箱 v1→v54、英语点读器 v1→v25、
> 自然拼读 v4.3→v4.5、安装包管理器 v1→v4.0、FileTool Compose）。
> 旧版备份：`mounts/mcp/人格库_开发版_v1_20261103-旧版.md`。

**触发**：新建 App、从零写功能、已有源码的大功能迭代。
**退出**：只修构建/安装 → 排障版；只分析现有 APK → 分析版。

---

## 0. 开工前置（三件事，没做完不许写界面）

### 0.1 版本锚点

记录旧 `versionName/versionCode` → 目标新值；包名是否保持；`minSdk/targetSdk`；
功能验收点 3~5 条；明确**不做项**。旧 APK 与旧源码一律不覆盖。

### 0.2 路线选择（决定性，选错后面全白干）

| 路线 | 何时用 | 构建 | 已验证实例 |
|---|---|---|---|
| **A · Java 纯代码**（默认） | 轻量工具、内容浏览、播放器、单/少 Activity、无复杂状态 | `javac` + D8 + ARM64 AAPT2 + MT 签名 | 图片工具箱 54 版、英语点读器 25 版、自然拼读 v4.5、安装包管理器 v4.0 |
| **B · Gradle + Kotlin/Compose**（条件） | 需要 Compose、Room、KSP、WorkManager、多模块 | Gradle 8.11.1 + AGP 8.1.2 + JDK 17 | FileTool v1.1.0（唯一实测） |

**默认选 A。** 选 B 必须先回答：不用 Compose/Room 就做不了吗？
答不上来就回 A —— A 路线单包 30~420KB、构建 20~60 秒、失败面小得多。
B 路线一旦启用，`gradle` 单次构建 1~4 分钟，且 aapt2/AGP 版本敏感；先跑 `gradle help` 冒烟，
依赖不在缓存时不得假称离线可构建。

**已有项目不强迁移**：旧项目已经真机验证的配色、圆角、渐变、布局和 Java 架构保持不动；
只有用户明确要求 UI 重构，才按本卡令牌迁移，迁移前先截图留档。

### 0.3 环境自检（当轮实测，不照抄旧笔记）

```sh
for p in /usr/lib/jvm/java-11-openjdk/bin/javac /opt/asdk/r8.jar \
         /opt/asdk/android-lib.jar /opt/asdk/android-all.jar \
         /var/cache/minis-android/aapt2.arm64; do
  [ -e "$p" ] && echo "OK   $p" || echo "MISS $p"
done
/var/cache/minis-android/aapt2.arm64 version   # 期望 AAPT2 2.19
```
B 路线额外确认：`gradle --version`（8.11.1）、JDK 17、`/opt/android-sdk/platforms/android-34`、
`build-tools/33.0.1` 或 `34.0.0`、`~/.gradle/caches` 内 AGP 8.1.2 是否已缓存。

---

## 1. 设计令牌（写死，不许临场自选）

### 1.1 马卡龙配色（浅色为默认）

| 令牌 | 色值 | 用途 |
|---|---|---|
| primary | `#A8C7FA` | 主按钮底、选中态、进度 |
| onPrimary | `#0B1D3A` | 主色上的文字（**禁止白字**，对比度不足） |
| primaryContainer | `#D9E6FF` | 卡片强调底、chip 选中底 |
| secondary | `#F7C8D8` | 次强调、标签 |
| tertiary | `#C9E7D0` | 成功、辅助 |
| accent | `#FFD9A0` | 高亮、提醒、徽标 |
| background | `#FBF8FF` | 默认页面底（**禁纯白作默认背景**；内容阅读区可按实际素材用纯白） |
| surface | `#F3F0FA` | 卡片底、面板底 |
| surfaceVariant | `#E7E2F0` | 输入框底、分隔区 |
| outline | `#C8C2D4` | 1dp 描边 |
| onBackground | `#1B1B21` | 主文字（**禁纯黑 #000000**） |
| onSurfaceVariant | `#6B6478` | 次文字、说明、页码 |
| error | `#E8A0A8` | 危险操作 |
| scrim | `#000000` @ 32% | 遮罩 |

深色主题只换 5 个：`background #141218`、`surface #1D1B20`、`surfaceVariant #2B2930`、
`onBackground #E6E1E9`、`onSurfaceVariant #CAC4D0`；其余不变。

规则：
- 全部进 `Ui.java`（A 路线）或 `theme/Color.kt`（B 路线），页面里**禁止出现 `0xFF...` 字面量**。
- 渐变默认禁用；例外见 1.6。
- 自定义色一律从这 14 个里取，不再发明第 15 个。

### 1.2 形状（四档，不许出现第五档）

| 元素 | 圆角 |
|---|---|
| 大卡 / hero 容器 / 弹窗 | 28dp |
| 普通卡片 / 面板 / 底部浮层 | 20dp |
| 按钮 / 输入框 / 图标容器 | 16dp |
| chip / 圆形徽标 | 999dp（全圆） |
| 分割线 | 0（用 1dp 线） |

A 路线实现：`GradientDrawable#setCornerRadius(dp)`；圆形用 `setShape(OVAL)`。
**禁止**：统一 8dp「万能圆角」、直角卡片、同类组件混用圆角；单页只能使用表内四档，不得临场新增第五档。

### 1.3 字体与字号（sp）

| 层级 | 字号/行高 | 字重 |
|---|---|---|
| Display | 36 / 44 | 400 |
| Headline L | 32 / 40 | 400 |
| Headline M | 28 / 36 | 400 |
| Title L | 22 / 28 | 500 |
| Title M | 16 / 24 | 500 |
| Body L | 16 / 24 | 400 |
| Body M | 14 / 20 | 400 |
| Label L | 14 / 20 | 500 |
| Label M | 12 / 16 | 500 |

规则：字体只用系统 `sans-serif`（**禁第三方字体**，A 路线引入字体文件会破坏体积与可复现性）；
页面标题用 Headline L/M；**每屏最多 3 个字号层级**。

### 1.4 间距（4dp 栅格，只允许 7 个数）

`4 / 8 / 12 / 16 / 24 / 32 / 48`

| 场景 | 值 |
|---|---|
| 页面左右内边距 | 16dp |
| 卡片内边距 | 16dp |
| 卡片间距 | 12dp |
| 分组间距 | 24dp |
| 图标与文字间距 | 8dp |
| 列表项最小高 | 56dp |
| 触控最小尺寸 | 48×48dp |
| 底部安全区 | 导航栏高 + 16dp |

出现上面 7 个数以外的 dp 值 = 跑偏。

### 1.5 动效

| 场景 | 参数 |
|---|---|
| 页面进入 | 位移 24dp + 淡入，280ms，`decelerate` |
| 按钮按压 | 缩放 0.96 → 1.0，120ms |
| 卡片展开 | 高度 320ms + 圆角联动 |
| 列表点击 | ripple，primary @ 12% |
| 转场 | 共享元素（A 路线用 `ActivityOptions`，B 路线用 `SharedTransitionLayout`） |

**关键实现差异（必看）**：

- **A 路线没有 `SpringAnimation`**（它在 `androidx.dynamicanimation`，不是 framework）。
  弹簧感用 `OvershootInterpolator(0.9f)` 或 `AnticipateOvershootInterpolator(0.6f)` 近似，
  或 `ValueAnimator` + 自定义 spring easing。**不要写不存在的类名。**
- **B 路线**才有真弹簧：`spring(dampingRatio = 0.8f, stiffness = 380f)`。

禁止：线性缓动（`linear`）做界面动效、>400ms 的长动画、装饰性循环动画（加载态除外）、
同页超过 2 种动效。

### 1.6 渐变（原稿「一票否决」改为「有条件」）

**默认禁用。** 唯一例外：页面与内容区色差明显、出现可见分界线时，允许在**顶/底栏**做
短距离渐变过渡（≤40dp 高度），且必须截图放大确认无硬边。

依据：英语点读器 v20 做过整页渐变 → 书页（白）与留白（薄荷）之间出现**可见分界线**，
反而更不沉浸；v21 改纯白才是对的。**沉浸来自边界连续，不来自有没有渐变。**

---

## 2. 布局：矩阵 / 宫格 / 紧凑

### 2.1 栅格

| 宽度 | 列数 | 边距 / 间隔 |
|---|---|---|
| 竖屏（<600dp） | 4 栅格，宫格用 **2 列**（大卡）或 3 列（图标卡） | 16 / 12 |
| 横屏 / ≥600dp | 6 栅格，宫格 **3 列** | 16 / 12 |
| ≥840dp | 8 栅格，宫格 **4 列** | 24 / 16 |

**同屏只允许一种列数**；旋转后列数可换，但必须整体重排，不能参差。

实测算法（自然拼读 v4.5）：卡片宽 = `(屏宽 − 左16 − 右16 − gap×(cols−1)) / cols`，
**最后一列不加右边距**；卡片高固定 142dp。A 路线用 `GridLayout`，B 路线用
`LazyVerticalGrid(GridCells.Fixed(cols))`。

### 2.2 宫格规则

- 首页功能区：**≤12 项用宫格**；**>12 项必须分组**（每组带 Label L 标题）——
  依据：图片工具箱功能涨到 30+ 后，九宫格挤爆，改分组卡片才稳。
- 单元顺序固定：图标（24 或 32dp）→ 标题（Title M）→ 副标题（Body M，可省）。
- 单元宽高比 1:1（图标型）或 4:3（图文卡），同组内一致。
- 单元**等高**：A 路线用固定高 + `Ellipsize` 截断，不要让内容撑高参差。

### 2.3 紧凑约束

- 首屏**不滚动**即可见主操作入口。
- hero 图最大高度 = 屏高 30%；禁止装饰大图占满首屏。
- 列表项说明文字最多 2 行，超出省略号。
- 空状态用 96dp 图标 + 两行文字，禁止插画占半屏。

### 2.4 按钮

| 类型 | 规格 |
|---|---|
| 主按钮 | primary 底 + onPrimary 字，16dp 圆角，高 48dp，全宽或最小 120dp |
| 次按钮 | surface 底 + 1dp outline 描边 + onBackground 字 |
| 危险按钮 | error 底，**必须二次确认** |
| 图标按钮 | 48×48 触控区，图标 24dp |

- 同屏按钮 ≤3 个，超出收进「更多」面板。
- 按钮间距：横向 12dp、纵向 8dp；距屏边保持 16dp。
- **禁止**：无文字的纯图标按钮当主动作（返回/关闭除外）、胶囊按钮套胶囊、
  用 `elevation` 阴影当按钮主层次（部分 ROM 会裁阴影）。

A 路线按钮 = `TextView` + `GradientDrawable` + `StateListDrawable` 三态（常态/按下加深/禁用），
历史 `Ui.java` 已有现成实现，**直接复用，不要重新发明**。

---

## 3. 状态栏沉浸（两条路线写法不同，别混）

### 3.1 A 路线（无 androidx）

```java
// API 30+
getWindow().setDecorFitsSystemWindows(false);
// 低版本兜底
getWindow().getDecorView().setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
getWindow().setStatusBarColor(Color.TRANSPARENT);
getWindow().setNavigationBarColor(Color.TRANSPARENT);
// 图标明暗
getWindow().getDecorView().setSystemUiVisibility(
    flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);   // 浅底用深色图标
```

状态栏高度取法（实测可用）：
```java
int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
int h = id > 0 ? getResources().getDimensionPixelSize(id) : 0;
```

### 3.2 B 路线（Compose）

`enableEdgeToEdge()` + `WindowInsets.statusBars` + `Modifier.windowInsetsPadding()`，
主题内必须包 `Surface(color = background, contentColor = onBackground)`，
否则深色主题下正文回落 `Color.Black`（FileTool 实测全黑）。

### 3.3 铁律

- 顶部内容留白 = 状态栏高 + 8dp；底部 = 导航栏高 + 16dp。
- **禁止双重留白**：`setStatusBarColor` 已预留 + 又加同高 padding → 出现「额头」。
  （DJ 播放器实测；判据：顶部空白明显大于状态栏。）
- 浅背景用深色状态栏图标，深背景用浅色图标；**禁止写死白色状态栏**。
- 横屏/分屏必须复测留白，`inset` 变化后要重新取。

---

## 4. 图标与资源

### 4.1 启动图标（用户定稿，别加戏）

- **只出一份 `mipmap-xxxhdpi/ic_launcher.png`，192×192**，不要 mdpi/hdpi/xhdpi。
- 成品 APK 内路径是 `res/mipmap-xxxhdpi-v4/ic_launcher.png`（aapt2 自动加 `-v4`）。
- 系统按设备密度自动缩放；省体积（实测 4 档 131KB → 1 档 86KB）。
- 改图标：改源文件重编（推荐）；或改成品 APK 后**必须重签名**。

### 4.2 应用内图标

- 只用两档尺寸：**24dp（行内）/ 32dp（宫格）**，48dp 仅用于空状态/大按钮。
- 风格统一：全线性或全填充，**不许混**。
- A 路线：优先 `Canvas` 手绘或 Unicode 符号（图片工具箱、英语点读器做法），
  避免为几个图标引 `androidx.vectordrawable`（零依赖工程里它编译不过）。
- B 路线：`Icons.Outlined.*` 或 `Icons.Filled.*`，同一屏不混两套。

**禁止**：多密度目录并存、位图图标、图标加投影、图标尺寸超出 24/32/48 三档。

---

## 5. 组件对照表（原稿写 Material 组件，A 路线要换原生实现）

| 需求 | A 路线（零依赖）实现 | 注意 |
|---|---|---|
| 卡片 | `LinearLayout/FrameLayout` + `GradientDrawable` 圆角 + 可选 1dp 描边 | 二选一：要么描边，要么极淡 elevation |
| 按钮 | `TextView` + 三态 `StateListDrawable` | 触点区 ≥48dp，用 `padding` 撑 |
| Chip | `TextView` 全圆背景 + 选中换 primaryContainer | 高 32dp |
| 底部导航 | `LinearLayout` 等分 + 图标/文字 + 顶部 1dp 线 | ≤5 项 |
| 输入框 | `EditText` + `shape` 背景 + `selector` 聚焦态 | 聚焦换 2dp primary 描边 |
| 弹窗 | `AlertDialog` + 自定义 View；`window.setBackgroundDrawable` 才能圆角 | 最大宽 560dp |
| 提示 | `Toast` 或自绘底部浮层（历史 `showSheet`） | 4s 自动消失 |
| 进度 | `ProgressBar`（圆形/水平）用 primary 着色 | 二选一，不混 |
| 顶部栏 | 自绘标题栏 + 滚动时缩小字号 | 不加整块底色 |

**一票否决**：同屏出现两种卡片样式、Drawer 与底部导航共存、Tab >4、
同屏按钮 >3、用 `Theme.MaterialComponents` 却零依赖（编译期直接找不到资源）。

---

## 6. 已知崩溃坑（本项目踩过，必须提前规避）

| 现象 | 根因 | 规避 |
|---|---|---|
| `IllegalStateException: specified child already has a parent` | 工厂方法返回了已 `addView` 的子 View | **工厂只返回容器**（踩过 2 次：英语点读器 v19、自然拼读 v4.4） |
| 列表整片空白，有数据 | `ListView` 用 `height=0 + weight=1` 挂在根 `LinearLayout` 下 → 实测 `listH=0` | 根改 `FrameLayout` + `ListView` MATCH_PARENT + 底部悬浮面板 |
| `IllegalArgumentException: Cannot add a null child view` | 动态控件未初始化就 `addView` | 先判空再 add，或工厂内建默认视图 |
| 面板撑满全屏 | `ScrollView` 用 `weight=1` | 改固定高度 `min(内容高, 屏高×0.62)` |
| 深色主题文字全黑（Compose） | 主题未包 `Surface` | 见 3.2 |
| 扫描选不到文件 | 依赖 `File.canRead()`（`/storage/emulated/0` 对 other 无 r-x 返回 false） | 去掉 canRead，`list()` + try/catch |
| 首屏滚动状态错乱 | 旋转后未重建布局 | `onConfigurationChanged` 里按当前模式重排 |

诊断口诀：布局类问题先打一行日志——
`root.postDelayed(() -> Log.i(TAG, "rootH="+root.getHeight()+" listH="+list.getHeight()+" n="+adapter.getCount()), 1500);`
真机抓 `logcat -d -s <TAG>:*`，一次定位，比逐行读代码快。

---

## 7. 工程结构与构建

### 7.1 A 路线（默认）

```
<App>/
├─ AndroidManifest.xml        # 必须自带 <uses-sdk>，否则装完弹权限页
├─ res/mipmap-xxxhdpi/        # 只有启动图标
├─ assets/                    # 数据/图片，代码 getAssets().open()
├─ apk_src/<包路径>/*.java
└─ build.sh                   # javac → d8 → aapt2 → 骨架 → MT 签名
```

- `minSdk` 默认 24（要用 `setDecorFitsSystemWindows` 时升 30 的代码分支要判版本）。
- `targetSdk` 默认 34（**AAPT2 2.19 只认 Platform 34 资源表**，35 报
  `illegal map type 'string' (22)`）。
- `targetSdk ≥30` 时 `resources.arsc` 必须 **STORED + 4 字节对齐**，否则安装报 `-124`。
- aapt2 `-I` 用 `/opt/asdk/android-all.jar`（不是 2MB 的 `android.jar`）。
- 需要 `R.java` 时用 `aapt2 link --java gen` 自动生成，**不要手写 ID**（曾因 ID 不一致
  `findViewById` 返回 null 崩溃）。
- 产物签名只用 MT MCP（V1+V2+V3），沙箱内没有第二套可行途径。

### 7.2 B 路线（条件启用）

```properties
# gradle.properties
android.aapt2FromMavenOverride=/var/cache/minis-android/aapt2     # FileTool 日记实测；ARM64；文件名必须严格叫 aapt2 且 chmod 755
# 若该别名不存在，可用已实测等价副本：/var/cache/minis-aapt2/aapt2
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m
android.useAndroidX=true
```

- `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk`（AGP 8.1.2 要求 ≥17，JDK 11 直接报错）。
- `local.properties`: `sdk.dir=/opt/android-sdk`。
- 依赖只用缓存中已有版本：AGP **8.1.2**、Kotlin **2.0.20**、Compose BOM **1.7.x**、
  material3 **1.3.0**、Material Components **1.9.0**。
  **原稿写的 "Material Components 1.12+" 本机不存在**，用 1.9.0；需要 1.12 必须先联网验证可得性。
- `gradle --no-daemon`，不要默认 `clean`。
- 签名同 A 路线（MT MCP）。

### 7.3 版本迭代

`versionCode +1`；`versionName`：不兼容主版本 +1 / 新功能次版本 +1 / 修复修订号 +1。
交付必须写**旧值 → 新值**。产物 `<应用名>_v<n>_<yyyyMMdd-HHmm>.apk`，旧版保留。

---

## 8. 反跑偏验收（逐条过，不过即改）

- [ ] 截图取色：全部命中 1.1 表，无纯黑纯白
- [ ] 圆角只有 28/20/16/999 四档
- [ ] 字号只出现在 1.3 表内，页面标题为 Headline 级，每屏 ≤3 个层级
- [ ] 间距只出现 4/8/12/16/24/32/48（`rg 'dp' 源码` 抽查字面量）
- [ ] 宫格同屏单一列数，单元等高，比例 1:1 或 4:3；>12 项已分组
- [ ] 首屏不滚动可见主操作
- [ ] 同屏按钮 ≤3，主动作是填充式，危险操作有二次确认
- [ ] 状态栏透明、内容延伸、留白正确、图标明暗正确；无「额头」双倍留白
- [ ] 启动图标只有 `mipmap-xxxhdpi` 一份；应用内图标尺寸仅 24/32/48
- [ ] 动效命中 1.5，无 `linear` 界面动效，无 >400ms 长动画
- [ ] 底部导航 ≤5 项，无 Drawer 共存
- [ ] 无硬编码色值（`rg '0xFF[0-9A-Fa-f]{6}' apk_src`）
- [ ] 依赖清单与选路（A/B）在交付说明中写明
- [ ] versionCode/versionName 已递增，给出旧→新
- [ ] 签名 + zipalign 通过，真机安装可启动
- [ ] 旋转/横竖屏、返回、后台恢复各过一次

---

## 9. 与原分享稿的差异（改的是不能用/会跑偏的地方）

| 原稿 | 本机实测 | 本卡写法 |
|---|---|---|
| 语言 Kotlin | A 路线全程 Java（9 个已验证项目） | **默认 Java**；Kotlin 仅 B 路线 |
| Material Components 1.12+ | 缓存只有 1.9.0；零依赖工程根本没 androidx | 写 **1.9.0**；A 路线用原生控件对照表（第 5 节） |
| `WindowCompat.setDecorFitsSystemWindows` | 该 API 在 androidx.core；A 路线无此依赖 | 改 `Window#setDecorFitsSystemWindows`（API 30+）+ 旧版 flag 兜底 |
| 弹簧动效 `stiffness/damping` | `SpringAnimation` 属 androidx.dynamicanimation，A 路线没有 | A 路线用 `OvershootInterpolator` 近似；真弹簧仅 B 路线 |
| 禁止一切渐变 | 顶/底栏短渐变曾用于消除分界线 | **有条件允许**（≤40dp，截图验证无硬边），见 1.6 |
| 启动图标「自适应两层」 | 零依赖工程只需单张 192×192 | 保持单份 xxxhdpi；自适应图标仅在 B 路线或明确要求时做 |
| 圆角 28 万能值 | 28 用在大卡/弹窗，按钮 16 更好用 | 四档表，禁止万能值 |
| 最低 SDK 26 | 历史项目 21/24/29/30 都用过 | 默认 **24**，用到 API 30 特性时分支判断 |
| 每次构建 `versionCode+1` | 一致 | 保留，并强制写旧→新 |
| 未提零依赖工程的布局坑 | 踩过 7 次崩溃（第 6 节） | 新增「已知崩溃坑」硬清单 |

---

## 10. 交付

- APK 双路径：`/var/minis/mounts/mcp/<名>.apk`（历史产物，现已不在该路径） + `/storage/emulated/0/MT2/mcp/<名>.apk`
- 源码路径、构建命令（A 路线给 `build.sh`，B 路线给 `gradle` 命令）
- 版本旧→新；选路 A/B 与理由；新增依赖（B 路线必列）
- 修改清单表：文件 / 改动 / 原因
- 验收清单勾选结果 + 截图（首屏 / 列表 / 详情 / 弹窗各一）
- **主动给验证点**：装哪个包、点哪里、应该看到什么、异常时抓哪行日志

必读：`apk-cli-build`、`android-dev`；A 路线参考源码
`shared/projects/phonics-collection/src/`（含可复用 `build.sh` 与纯代码 UI 工厂）。
