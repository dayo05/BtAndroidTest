package com.dayo.BtAndroidTest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.MalformedURLException
import java.net.URL

class AutoDataReceiver : Service() {

    private val notificationManager get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "notification_bar"
        private const val CHANNEL_NAME = "default"
        private const val CHANNEL_DESCRIPTION = "wa sans ashinunguna!"
        private var humi = 0
        private var temp = 0
        private var pm1_0 = 0
        private var pm2_5 = 0
        private var pm10_0 = 0
        private var bulquaezhisu = 0

        public fun getFineDustString_PM10_0(pm10: Int): String {
            return when (pm10) {
                in 0..15 -> "최고 좋음"
                in 16..30 -> "좋음"
                in 31..40 -> "양호"
                in 41..50 -> "보통"
                in 51..75 -> "나쁨"
                in 76..100 -> "상당히 나쁨"
                in 101..150 -> "매우 나쁨"
                else -> "최악"
            }
        }

        public fun getFineDustString_PM2_5(pm2_5: Int): String {
            return when (pm2_5) {
                in 0..8 -> "최고 좋음"
                in 9..15 -> "좋음"
                in 16..20 -> "양호"
                in 21..25 -> "보통"
                in 26..37 -> "나쁨"
                in 38..50 -> "상당히 나쁨"
                in 51..75 -> "매우 나쁨"
                else -> "최악"
            }
        }

        public fun getBulQuaeZhisu(d: Int): String {
            return when (d) {
                in 68..75 -> "일부 사람이 불쾌감을 느낍니다."
                in 76..80 -> "반 이상의 사람이 불쾌감을 느낍니다."
                in 81..1000 -> "대부분의 사람이 불쾌감을 느낍니다."
                else -> "쾌적한 날입니다."
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = CHANNEL_DESCRIPTION
            this.setShowBadge(true)
            this.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        })
    }

    fun updateMainNotification() {
        val view = RemoteViews(packageName, R.layout.main_notification_layout).clone()

        val a = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("notiVals", emptySet())!!
        val vx = RemoteViews(packageName, R.layout.sub_view_main_notification)
        if(a.size < 1)
            view.addView(R.id.first_view, vx)
        if(a.size < 2)
            view.addView(R.id.second_view, vx)
        if(a.size < 3)
            view.addView(R.id.third_view, vx)
        for((i, x) in a.withIndex()) {
            val v = RemoteViews(packageName, R.layout.sub_view_main_notification)
            v.setTextViewText(R.id.subview_1, getRealNameByValue(x))
            v.setInt(R.id.subview_circle_text, "setBackgroundResource", R.drawable.circle)
            v.setTextViewText(R.id.subview_circle_text, getDataByValue(x))

            when (i) {
                0 -> view.addView(R.id.first_view, v)
                1 -> view.addView(R.id.second_view, v)
                2 -> view.addView(R.id.third_view, v)
            }
        }
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID).apply {
                setCustomContentView(view)
                setSmallIcon(R.drawable.ic_launcher_background)
            }.build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var `is`: InputStream? = null

        Thread {
            while(true) {
                while(!SettingsActivity.updateFlag)
                    sleep(100)
                SettingsActivity.updateFlag = false
                //updateMainNotification()
                stopSelf()
                startForegroundService(Intent(this@AutoDataReceiver, AutoDataReceiver::class.java))
                break
            }
        }.start()

        Thread {
            while(true) {
                try {
                    var line: String?
                    val url = URL("http://192.168.1.150:5000/arduinodata")
                    `is` = url.openStream() // throws an IOException
                    val br = BufferedReader(InputStreamReader(`is`))
                    while (br.readLine().also { line = it } != null) {
                        try {
                            val d = line!!.split('|')
                            humi = d[0].toInt()
                            temp = d[1].toInt()
                            pm1_0 = d[2].toInt()
                            pm2_5 = d[3].toInt()
                            pm10_0 = d[4].toInt()
                            bulquaezhisu = (0.81 * temp / 100 + 0.0001 * humi * (temp / 100.0 * 0.99 - 14.3) + 46.3).toInt()
                            updateMainNotification()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        Log.d("asdf", line.toString())
                    }
                } catch (mue: MalformedURLException) {
                    mue.printStackTrace()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                } finally {
                    try {
                        `is`?.close()
                    } catch (ioe: IOException) {
                        // nothing to see here
                    }
                }
                sleep(PreferenceManager.getDefaultSharedPreferences(this).getString("dt", "60000")?.toLong()!!)
            }
        }.start()
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("서버로부터 데이터 받아오는중!")
            setContentText("수신한 데이터: waiting for download data")
            setSmallIcon(R.drawable.ic_launcher_background)
            setVisibility(VISIBILITY_PUBLIC)
        }.build())
        return START_STICKY
    }


    fun getRealNameByValue(s: String): String {
        return when(s) {
            "temp" -> "온도"
            "humi" -> "습도"
            "bulquae" -> "불쾌지수"
            "dust10" -> "미세먼지"
            "dust2" -> "초미세먼지"
            "dust1" -> "극초미세먼지"
            else -> ""
        }
    }

    fun getDataByValue(s: String): String {
        return when(s) {
            "temp" -> "${temp / 100.0}°C"
            "humi" -> "${humi / 100.0}%"
            "bulquae" -> bulquaezhisu.toString()
            "dust10" -> "${pm10_0}ppm"
            "dust2" -> "${pm2_5}ppm"
            "dust1" -> "${pm1_0}ppm"
            else -> "0"
        }
    }
}