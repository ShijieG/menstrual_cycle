# 经期记录（Menstrual Tracker）

一款**原生 Android（Kotlin + Jetpack Compose）**的女性月经周期管理 App。完全本地运行、离线可用，**仅使用设备系统日期（无日历权限、无联网）**，隐私优先。

## 功能

- **日历即记录**：在「日历」页直接点击任意日期，即可查看当天记录状态，并标记**经期开始 / 结束**、勾选**经期症状**、记录**性生活**；支持回溯过去日期。
- **智能预测**：基于历史经期，自动预测**未来 36 个月**每一次月经的开始与结束，并以**置信等级 + 80% 概率区间**呈现；历史不足时回落医学默认周期（28 天 / 5 天）并标注「低置信」。
- **月历着色与百分比**：日历中按概率深浅着色（越深 = 来月经概率越高），预测日直接显示百分比；并标注已记录经期、排卵日（青色圆点）、易孕日（青色小点），今天高亮。
- **排卵期 / 易孕日**：由预测经期自动推算（排卵 ≈ 经期前 14 天，易孕窗 = 排卵日 −5 ~ +1）。
- **症状记录**：腹痛 / 腰酸 / 头痛 / 乳房胀痛 / 疲劳 / 情绪波动等多标签随手记（已移除心情、分泌物）。
- **提醒通知**：经期来临前 N 天、排卵日系统通知（WorkManager 每日调度，Android 13+ 需通知权限）。
- **统计趋势**：平均周期、平均经期、记录次数、规律性，以及近期周期长度柱状图。
- **数据导出**：将所有记录导出为 JSON 文件并通过系统分享备份。

## 技术栈与架构

- **Kotlin + Jetpack Compose**（Material 3，底部导航）
- **Room**（SQLite）持久化经期与症状；**DataStore** 保存设置
- **MVVM**：UI（Compose）↔ ViewModel ↔ Repository ↔ Room；预测逻辑在**纯 Kotlin 领域层**（`domain`），可单元测试
- **WorkManager** 提醒、**FileProvider** 导出分享、**java.time + 核心库脱糖**（minSdk 24）
- 无 Hilt：采用 Application 级手动依赖容器（`MenstrualApplication`）

## 目录

```
app/src/main/java/com/example/menstrual/
├── MenstrualApplication.kt          # 应用入口 + 依赖容器
├── MainActivity.kt
├── data/
│   ├── local/{entity,dao,AppDatabase.kt,Converters.kt}
│   ├── repository/PeriodRepository.kt
│   ├── preferences/SettingsDataStore.kt
│   └── export/ExportManager.kt
├── domain/
│   ├── model/{Cycle.kt,Prediction.kt}
│   ├── util/Statistics.kt           # 均值/标准差/高斯/置信区间
│   └── usecase/PredictCyclesUseCase.kt  # 核心预测算法
├── ui/
│   ├── MainApp.kt, AppViewModel.kt
│   ├── theme/{Color,Theme,Type}.kt
│   ├── calendar/CalendarScreen.kt   # 日历 + 记录（已合并）
│   ├── prediction/PredictionScreen.kt
│   ├── stats/StatsScreen.kt
│   └── settings/SettingsScreen.kt
└── reminder/{ReminderWorker.kt,ReminderScheduler.kt}
```

## 无需本地环境：GitHub Actions 自动出包（推荐）

你**不必在电脑上安装 Android Studio / SDK**。把代码推到 GitHub 后，GitHub 的免费云服务器会自动编译并产出可安装的 APK：

1. 在 GitHub 新建一个**公开（或私有）仓库**。
2. 在本工程目录执行（把 `<your-repo-url>` 换成你的仓库地址）：
   ```bash
   git init
   git add .
   git commit -m "init menstrual tracker"
   git branch -M main
   git remote add origin <your-repo-url>
   git push -u origin main
   ```
   > 若使用 SSH 且遇到 22 端口被拦：本机已配置 `github.com` 走 443（见 `~/.ssh/config`），`git@github.com:...` 形式地址可直接使用。
3. 进入仓库 **Actions** 标签页，等待名为 **Build Debug APK** 的工作流完成（首次约 5–15 分钟，需下载 JDK/SDK/依赖）。
4. 完成后在对应运行记录的 **Artifacts** 中下载 `menstrual-tracker-debug-apk`，即为可直接安装到手机的调试包。
5. 手机开启「允许安装未知来源应用」后，传输 APK 安装即可。

> 工作流定义在 `.github/workflows/build-apk.yml`：JDK 17 + Android SDK 34 + Gradle 8.6，`assembleDebug` 出包并上传产物。如需 release 签名包，按下方「发布签名」配置 GitHub Secrets 后改用 `assembleRelease`（并相应调整工作流里的构建命令）。

---

## 本地构建（备选，需开发者环境）

> 若你更倾向在本地编译，或想用 Android Studio 调试，请按本段操作。注意：当前机器无 Android SDK，未做真实编译/打包；以下步骤请在本地执行。

1. 安装 **Android Studio**（含 Android SDK 34、Kotlin 1.9）。
2. 用 Android Studio 打开本目录（或 `git clone` 后打开）。
3. 等待 Gradle 同步（会自动下载依赖）。
4. 连接设备或启动模拟器，点击 ▶ Run；或出包：
   - 调试：`gradle assembleDebug`
   - 发布：`gradle assembleRelease`（需先在 `app/build.gradle` 配置签名，详见下方）
5. 出包产物：`app/build/outputs/apk/release/`（APK）或 `bundle/release/`（AAB）。

### 发布签名（release）

在 `app/build.gradle` 的 `android { signingConfigs { ... } }` 中配置密钥库，**密钥与密码一律用环境变量引用，切勿明文写入**：

```gradle
signingConfigs {
    release {
        storeFile file(System.getenv("KEYSTORE_PATH"))
        storePassword System.getenv("KEYSTORE_PWD")
        keyAlias System.getenv("KEY_ALIAS")
        keyPassword System.getenv("KEY_PWD")
    }
}
buildTypes.release.signingConfig = signingConfigs.release
```

## 隐私与权限

- **无需日历权限、无需联网权限**：所有日期取自设备系统时钟，数据仅保存在本机 Room 数据库。
- 唯一权限：`POST_NOTIFICATIONS`（Android 13+，用于提醒，可拒绝）。
- 导出文件位于应用私有目录 `/Android/data/<pkg>/files/exports/`，通过 FileProvider 分享，不对外暴露。

## 预测算法简述

- **统计窗口**：仅取**过去 36 个月（today 及之前、且 ≥ today−36 月）**的经期记录求周期分布；更早的记录不计入，避免陈旧数据干扰均值/标准差。
- **预测窗口**：预测**未来 36 个月**内每一次月经（从最后一次记录的下一次月经起递推，**仅生成 today 之后的预测**，不混入已过去的时间点）。
1. 由窗口内相邻经期开始日求周期长度序列，统计均值 μ 与标准差 σ；
2. 由窗口内已结束经期求经期长度序列的均值与标准差；
3. 窗口内 ≥ 2 个完整区间用真实分布，否则回落默认 28 天 / 5 天（低置信）；
4. 按 μ 递推未来每次起点，套高斯分布给出 80% 置信区间与每日概率（供日历着色）。

详见 `domain/usecase/PredictCyclesUseCase.kt` 与 `domain/util/Statistics.kt`。
