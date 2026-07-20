package com.nsfs.aireply

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.nsfs.aireply.databinding.ActivityMainBinding
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvHint.text = "云端：novelgrok.nsfs.cn\n\n使用流程：\n" +
                "1. 点「启动浮窗」→ 允许「在其他应用上层显示」；\n" +
                "2. 切到微信，长按对方消息 → 复制；\n" +
                "3. 点屏幕左上角浮窗的「粘贴」→「生成候选」；\n" +
                "4. 点任意一条，自动复制到剪贴板，回微信粘贴发送。\n\n" +
                "⚠️ 如提示「屡次停止运行」，请先到设置里给本应用「悬浮窗」权限。"

        binding.btnStart.setOnClickListener { ensurePermissionThenStart() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun ensurePermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            binding.tvStatus.text = "请在弹出的设置里允许「在其他应用上层显示」，然后返回重新点启动。"
            return
        }
        startFloating()
    }

    private fun startFloating() {
        if (FloatingService.isRunning) {
            Toast.makeText(this, "浮窗已在运行", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, FloatingService::class.java)
        // API 26+ 启动前台服务必须用 startForegroundService，否则崩溃
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.tvStatus.text = "浮窗已启动，回到桌面 / 微信即可看到左上角的绿色浮窗。"
    }
}
