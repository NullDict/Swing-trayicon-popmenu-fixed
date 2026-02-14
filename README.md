# Kotlin Swing 系统托盘工具

一个基于 Kotlin 和 Java Swing 的系统托盘 demo。旨在解决原生 AWT 菜单样式丑陋、字体渲染差的问题，特别优化了对**中文**显示的支持。

## ✨ 特性

*   **🇨🇳 完美中文支持**：使用 Swing `JPopupMenu` 替代原生 AWT `PopupMenu`，解决了传统 Java 托盘菜单中文字体发虚、难看或乱码的问题。
*   **🎨 自定义图标**：内置二进制图标生成逻辑，无需外部图片文件即可运行。同时也支持轻松替换为自定义图片。
*   **🚀 现代化 UI**：自动适配操作系统外观，菜单样式美观，支持右键菜单和双击事件。

## 🛠️ 技术原理

Java 原生的 `SystemTray` API 只能添加 AWT 组件，而 AWT 组件对高 DPI 和中文字体的支持较差。

本项目采用了一种经典的 **"隐形窗口" (Hidden Dialog)** 技巧：
1.  监听托盘图标的鼠标点击事件。
2.  创建一个不可见的 `JDialog` 窗口，位置跟随鼠标。
3.  在该窗口上显示 Swing 风格的 `JPopupMenu`。

这样既保留了系统托盘的功能，又享受了 Swing 组件美观、灵活的优势。

## 📦 如何运行

1.  确保安装了 JDK (Java Development Kit) 8 或更高版本。
2.  克隆或下载本项目。
3.  直接运行 `SwingSystemTray.kt` 中的 `main` 函数。

```kotlin
fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            SwingSystemTray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
