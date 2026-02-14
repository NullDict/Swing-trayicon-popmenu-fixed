// 导入必要的 Java GUI 和 IO 类
import java.awt.*
import java.awt.event.*
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.exitProcess

class SwingSystemTray {
    // 系统托盘对象（操作系统右下角的托盘区域）
    private var systemTray: SystemTray? = null
    // 托盘图标对象（显示在托盘上的小图片）
    private var trayIcon: TrayIcon? = null
    // 右键弹出菜单 (Swing 组件，比 AWT 的 PopupMenu 更美观、功能更强)
    private var systemTrayPopupMenu: JPopupMenu? = null
    // 托盘图标的图片对象
    private var iconImage: Image? = null

    // 【重点技巧】一个隐藏的对话框窗口
    // 原因：Java 的 TrayIcon 原生只支持 AWT 的 PopupMenu。
    // 如果我们想用 Swing 的 JPopupMenu（更好看），必须借助一个隐藏的窗口作为“载体”来显示它。
    // 注意：在 Kotlin 中，如果变量在 init 块中赋值，通常建议加上 lateinit 关键字，或者像这里一样先声明。
    private var hiddenDialog: JDialog

    init {
        // 1. 获取托盘图标图片
        try {
            iconImage = getIcon()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 2. 检查操作系统是否支持系统托盘功能
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray()
            // 构建右键菜单
            systemTrayPopupMenu = buildSystemTrayJPopupMenu()

            // 3. 创建并配置托盘图标
            trayIcon = TrayIcon(iconImage, "Application Name", null).apply {
                // 自动调整图片大小以适应托盘尺寸
                isImageAutoSize = true

                // 添加鼠标监听器，用于处理右键点击弹出菜单
                addMouseListener(object : MouseAdapter() {
                    override fun mouseReleased(me: MouseEvent) {
                        // isPopupTrigger 用于判断是否为“弹出菜单”的触发动作（通常是右键单击）
                        if (me.isPopupTrigger) {
                            // 获取鼠标当前在屏幕上的坐标
                            val mouseLocation = MouseInfo.getPointerInfo().location

                            // 【核心技巧】设置隐藏窗口的位置和大小
                            // 将隐藏窗口移动到鼠标点击的位置，并让它极小（1x1像素）
                            hiddenDialog.location = mouseLocation
                            hiddenDialog.size = Dimension(1, 1)
                            // 显示窗口（只有显示了窗口，菜单才能附着在上面）
                            hiddenDialog.isVisible = true

                            // 计算菜单位置，防止菜单超出屏幕边界
                            val screenBounds: Rectangle = hiddenDialog.graphicsConfiguration.bounds
                            val screenWidth = screenBounds.width
                            val popup = systemTrayPopupMenu ?: return

                            popup.pack() // 计算菜单的最佳尺寸
                            val menuWidth = popup.preferredSize.width
                            val menuHeight = popup.preferredSize.height

                            // 默认显示在鼠标点击处的右上方偏移一点位置
                            var x = 7
                            var y = -menuHeight - 3

                            // 如果菜单宽度超出屏幕右侧，则调整到鼠标左侧显示
                            if (mouseLocation.x + menuWidth > screenWidth) {
                                x = -menuWidth
                            }

                            // 在隐藏窗口的 坐标处显示 Swing 菜单
                            popup.show(hiddenDialog, x, y)
                        }
                    }
                })

                // 添加动作监听器（双击托盘图标时触发）
                addActionListener {
                    println("actionPerformed (Double Clicked)")
                }
            }

            // 4. 将图标添加到系统托盘
            try {
                systemTray?.add(trayIcon)
            } catch (_: AWTException) {
                println("Could not place item at tray.  Exiting.")
            }
        }

        // 5. 初始化那个用于承载菜单的“隐藏对话框”
        hiddenDialog = JDialog().apply {
            size = Dimension(10, 10) // 设置一个小尺寸
            // 添加窗口焦点监听器
            addWindowFocusListener(object : WindowFocusListener {
                // 当窗口失去焦点时（例如用户点击了其他地方），隐藏窗口
                // 这样菜单就会消失，模拟正常菜单的行为
                override fun windowLostFocus(we: WindowEvent) {
                    isVisible = false
                }

                override fun windowGainedFocus(we: WindowEvent) {
                    // 获取焦点时无需操作
                }
            })
        }
    }

    // 构建右键弹出菜单的方法
    private fun buildSystemTrayJPopupMenu(): JPopupMenu {
        val menu = JPopupMenu()
        // 创建菜单项
        val showMenuItem = JMenuItem("显示")
        val hideMenuItem = JMenuItem("隐藏")
        val exitMenuItem = JMenuItem("退出")

        // 初始状态设置：“隐藏”按钮不可用
        hideMenuItem.isEnabled = false

        // 定义菜单项的点击事件监听器
        val listener = ActionListener { ae ->
            // 点击任何菜单项后，先把隐藏窗口关掉（菜单也就消失了）
            hiddenDialog.isVisible = false

            when (ae.source) {
                showMenuItem -> {
                    println("Shown")
                    showMenuItem.isEnabled = false
                    hideMenuItem.isEnabled = true
                }

                hideMenuItem -> {
                    println("Hidden")
                    hideMenuItem.isEnabled = false
                    showMenuItem.isEnabled = true
                }

                exitMenuItem -> {
                    // 退出前先移除托盘图标，然后关闭程序
                    systemTray?.remove(trayIcon)
                    exitProcess(0) // 退出 JVM
                }
            }
        }

        val items = arrayOf(showMenuItem, hideMenuItem, exitMenuItem)
        for (item in items) {
            // 在“退出”按钮前加一条分割线
            if (item == exitMenuItem) {
                menu.addSeparator()
            }
            item.addActionListener(listener)
            menu.add(item)
        }
        return menu
    }

    // 生成图标图片的方法
    @Throws(IOException::class)
    private fun getIcon(): Image {
        // 这里是通过二进制数据手动生成一个简单的 16x16 BMP 图片
        // 新手无需深入理解这里的二进制构造，只需知道它生成了一个默认图标即可
        val iconData = ByteArray(822)
        val header = byteArrayOf(
            0x42, 0x4d,             // BMP 文件头标识 'BM'
            0x36, 0x03, 0, 0,       // 文件大小
            0, 0, 0, 0,             // 保留字段
            0x36, 0, 0, 0,          // 像素数据偏移量
            0x28, 0, 0, 0,          // 位图信息头大小
            16, 0, 0, 0,            // 宽度: 16px
            16, 0, 0, 0,            // 高度: 16px
            16, 0,                  // 颜色平面数
            24, 0,                  // 每像素位数: 24位 (真彩色)
            0, 0, 0, 0,             // 压缩方式
            0, 0, 0, 0x03           // 图像数据大小
        )
        System.arraycopy(header, 0, iconData, 0, 36)

        // 填充像素数据，这里 -1 (0xFF) 代表白色
        for (i in 56..<822 step 3) {
            iconData[i] = -1
        }

        // 将字节数组转换为 Image 对象
        return ImageIO.read(ByteArrayInputStream(iconData))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // SwingUtilities.invokeLater 确保 UI 更新在事件分发线程 (EDT) 中执行
            // 这是 Swing GUI 编程的安全规范，防止界面卡死或报错
            SwingUtilities.invokeLater {
                try {
                    // 设置界面风格为当前操作系统风格
                    // 例如在 Windows 上看起来就像 Windows 程序，在 Mac 上像 Mac 程序
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

                    // 实例化并运行托盘程序
                    SwingSystemTray()
                } catch (e: Exception) {
                    println("Not using the System UI defeats the purpose...")
                    e.printStackTrace()
                }
            }
        }
    }
}
