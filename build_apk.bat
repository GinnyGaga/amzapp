@echo off
chcp 65001 > nul
echo ========================================================
echo       Amazon ASIN 排名监控 - APK 一键打包助手
echo ========================================================
echo.

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [提示] 检测到本机未配置 Java 开发环境。
    echo.
    echo 打包 APK 推荐使用以下方式之一：
    echo.
    echo 1. 【强烈推荐】使用 Android Studio 打开本项目：
    echo    - 启动 Android Studio，点击 File -^> Open，选择当前目录: d:\project\amzapp
    echo    - 点击顶部菜单: Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
    echo    - 编译完成后即可直接在 app\build\outputs\apk\debug\ 目录下获取 APK 文件！
    echo.
    echo 2. 【自动化云端打包】推送到 GitHub：
    echo    - 本工程已内置 .github\workflows\build-apk.yml
    echo    - 将代码推送到您的 GitHub 仓库，GitHub Actions 会自动编译并提供 APK 直接下载！
    echo.
    echo 3. 【本机命令行编译】：
    echo    - 运行: winget install Microsoft.OpenJDK.17
    echo    - 重启终端后重新执行本脚本 build_apk.bat 即可自动编译。
    echo.
    pause
    exit /b 1
)

echo [1/2] 正在调用 Gradle 编译 Debug APK...
call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo [成功] APK 编译成功！
    echo 安装包路径位于：
    echo app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
    echo.
    explorer.exe app\build\outputs\apk\debug\
) else (
    echo.
    echo [失败] 编译遇到错误，请检查网络连接或在 Android Studio 中打开排查。
)

pause
