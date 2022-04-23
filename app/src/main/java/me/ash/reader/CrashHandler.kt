package me.ash.reader

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : UncaughtExceptionHandler {
    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(p0: Thread, p1: Throwable) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        Toast.makeText(context, p1.message, Toast.LENGTH_LONG).show()
        Looper.loop()
        p1.printStackTrace()
        Log.e("RLog", "uncaughtException: ${p1.message}")
        android.os.Process.killProcess(android.os.Process.myPid());
        exitProcess(1)
    }
}
