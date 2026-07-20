package com.nsfs.aireply

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*

/**
 * 悬浮窗服务：常驻一个可拖拽、可折叠的小窗，贴在你用微信时的最上层。
 * 不依赖 curl / Termux / root —— 请求由系统网络栈直接发出。
 */
class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var rootView: View? = null
    private var expanded = true

    companion object {
        const val CHANNEL_ID = "ai_reply_float"
        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notif = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AI 回复建议浮窗")
                .setContentText("浮窗运行中，点按候选即可复制")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .build()
            // API 34+ 必须声明前台服务类型，否则抛 MissingForegroundServiceTypeException
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notif)
            }
        }
        showFloating()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "AI 回复浮窗", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun showFloating() {
        val view = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        rootView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 120
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN

        wm.addView(view, params)
        bindView(view)
    }

    private fun bindView(view: View) {
        val header = view.findViewById<View>(R.id.float_header)
        val toggle = view.findViewById<TextView>(R.id.btn_toggle)
        val close = view.findViewById<TextView>(R.id.btn_close)
        val body = view.findViewById<View>(R.id.float_body)
        val etCtx = view.findViewById<EditText>(R.id.et_context)
        val btnGen = view.findViewById<Button>(R.id.btn_generate)
        val btnPaste = view.findViewById<Button>(R.id.btn_paste)
        val status = view.findViewById<TextView>(R.id.tv_status)
        val list = view.findViewById<LinearLayout>(R.id.candidate_list)

        // 顶部标题栏拖拽
        header.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var paramX = 0
            private var paramY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.rawX
                        startY = e.rawY
                        paramX = params.x
                        paramY = params.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = paramX + (e.rawX - startX).toInt()
                        params.y = paramY + (e.rawY - startY).toInt()
                        wm.updateViewLayout(rootView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })

        toggle.setOnClickListener {
            expanded = !expanded
            body.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.text = if (expanded) "▾" else "▴"
        }

        close.setOnClickListener { stopSelf() }

        btnPaste.setOnClickListener {
            val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            if (clip != null && clip.itemCount > 0) {
                etCtx.setText(clip.getItemAt(0).text)
            } else {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        }

        btnGen.setOnClickListener {
            val ctx = etCtx.text.toString().trim()
            if (ctx.isEmpty()) {
                status.text = "请先点「粘贴」取对方消息"
                return@setOnClickListener
            }
            status.text = "生成中…"
            list.removeAllViews()
            ApiClient.fetch(ctx, Prefs.getUrl(this), Prefs.getToken(this), Prefs.getN(this)) { cands, err ->
                if (err != null) {
                    status.text = err
                    return@fetch
                }
                status.text = "共 ${cands?.size ?: 0} 条 · 点按复制"
                list.removeAllViews()
                cands?.forEach { (style, text) -> list.addView(makeCandidate(style, text)) }
            }
        }
    }

    private fun makeCandidate(style: String, text: String): View {
        val ctx = this
        val item = LayoutInflater.from(this).inflate(R.layout.candidate_item, null) as LinearLayout
        val chip = item.findViewById<TextView>(R.id.cand_style)
        val body = item.findViewById<TextView>(R.id.cand_text)
        chip.text = style
        body.text = text
        val color = when (style) {
            "幽默" -> Color.parseColor("#E74C3C")
            "高冷" -> Color.parseColor("#2980B9")
            "角色" -> Color.parseColor("#8E44AD")
            else -> Color.parseColor("#27AE60")
        }
        chip.setTextColor(color)
        item.setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("reply", text))
            Toast.makeText(ctx, "已复制，去微信粘贴", Toast.LENGTH_SHORT).show()
        }
        return item
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView?.let { wm.removeView(it) }
        rootView = null
        isRunning = false
    }
}
