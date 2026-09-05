# 亚马逊 ASIN 每日排名监控与折线图 App (AmzRankTracker)

一款专为亚马逊卖家与竞品追踪者打造的原生 Android 应用程序。支持纯手机端自动化运行、每日定时静默抓取、落库持久化，并生成直观美观的**每日-排名走势折线图**。

---

## ✨ 核心特性

1. **商品与 ASIN 智能管理**：
   - 支持直接输入 10 位 ASIN 码（如 `B08N5WRWNW`）或直接粘贴包含 ASIN 的亚马逊商品分享链接，系统自动正则识别提取。
   - 支持自定义商品别名备注（如“主力降噪耳机”、“竞品爆款”）。
2. **纯手机端静默定时任务**：
   - 基于 Android 官方 **WorkManager** 架构，系统每 24 小时自动触发一次后台静默抓取。
   - 抓取请求之间加入 3~6 秒随机抖动间隔，模拟真人浏览。
   - 支持主界面随时点击“**全部刷新**”或单项“**立即刷新**”，秒级更新当前实时排名。
3. **本地数据库存储（落库）**：
   - 基于 **Jetpack Room (SQLite)** 构建，所有历史抓取数据安全保存在手机本地，永久留存。
   - 自动计算并展示今日排名相比昨日的升降变动（`↑ 上升 15 名` / `↓ 下降 8 名` / `持平`）。
4. **专业的倒序排名走势折线图**：
   - **电商专属视觉设计**：符合亚马逊排名直觉，**名次越好（数值越小）曲线越靠上**，最高点为 `#1`。
   - **手势交互**：手指在折线图上拖拽滑动，实时显示垂直引导线与悬浮卡片，清晰查看每日精准名次。
   - **多维度筛选**：一键切换【近 7 天】、【近 30 天】、【全部历史】，支持在大类 BSR 排名与细分子类目排名之间切换。
   - **每日明细表**：折线图下方同步展示每日抓取快照。
5. **反爬虫穿透与人机验证助手**：
   - 若遇到亚马逊 503 或 Robot Check 人机验证码，App 顶部会及时提醒并支持一键进入**内置会话助手**。
   - 在内置安全 WebView 中通过一次滑块验证后，自动同步最新 Cookies 给后台爬虫，保障长期稳定运行。

---

## 🏗️ 项目工程结构

```
d:\project\amzapp\
├── app\
│   ├── src\main\
│   │   ├── AndroidManifest.xml          # 权限（网络、通知、后台唤醒）
│   │   ├── java\com\amzrank\tracker\
│   │   │   ├── MainActivity.kt          # 单 Activity 声明式导航
│   │   │   ├── AmzApplication.kt        # WorkManager 与系统初始化
│   │   │   ├── data\
│   │   │   │   ├── local\               # Room 数据库与 DAO
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao\
│   │   │   │   │   │   ├── AsinDao.kt
│   │   │   │   │   │   └── RankRecordDao.kt
│   │   │   │   │   └── entity\
│   │   │   │   │       ├── AsinItem.kt
│   │   │   │   │       └── RankRecord.kt
│   │   │   │   ├── scraper\             # 爬虫引擎与反爬会话
│   │   │   │   │   ├── AmazonScraper.kt
│   │   │   │   │   ├── CookieManagerHelper.kt
│   │   │   │   │   └── ScrapeResult.kt
│   │   │   │   └── repository\          # 业务仓库层
│   │   │   │       └── RankRepository.kt
│   │   │   ├── worker\                  # 每日后台调度
│   │   │   │   ├── DailySyncWorker.kt
│   │   │   │   └── WorkScheduler.kt
│   │   │   └── ui\                      # Jetpack Compose UI
│   │   │       ├── components\
│   │   │       │   ├── AddAsinDialog.kt
│   │   │       │   └── RankLineChart.kt # 自定义倒序折线图
│   │   │       ├── screens\
│   │   │       │   ├── HomeScreen.kt    # ASIN 列表主界面
│   │   │       │   ├── DetailScreen.kt  # 走势图与历史记录
│   │   │       │   └── WebVerifyScreen.kt # 验证码穿透助手
│   │   │       ├── theme\               # 亚马逊经典主题
│   │   │       └── viewmodel\
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── .github\workflows\build-apk.yml      # GitHub Actions 云端自动打包工作流
├── build_apk.bat                        # Windows 本机一键打包脚本
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📦 打包生成 APK 安装文件

您可以通过以下三种方式生成 `.apk` 文件并安装到安卓手机：

### 方式 1：使用 Android Studio 打开（最推荐）
1. 下载安装 [Android Studio](https://developer.android.com/studio)；
2. 打开 Android Studio，点击 `Open`，选择本项目目录 `d:\project\amzapp`；
3. 等待 Gradle 自动完成依赖同步；
4. 点击顶部菜单：`Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`；
5. 打包完成后，点击弹窗提示的 `locate`，即可在 `app\build\outputs\apk\debug\app-debug.apk` 找到安装包。

### 方式 2：使用 GitHub Actions 云端一键打包（无需配置本地环境）
1. 本项目已预置 `.github/workflows/build-apk.yml`；
2. 将本项目代码推送到您的 GitHub 仓库；
3. 进入仓库页面的 **Actions** 标签，工作流将自动运行；
4. 构建完成后，在 **Artifacts** 中直接下载生成的 `AmzRankTracker-Debug-APK.zip`，解压即是安装包！

### 方式 3：本机命令行打包
双击运行根目录下的 `build_apk.bat`。如果本机已安装 JDK 17，脚本将自动拉取 Gradle 并编译输出 APK。
