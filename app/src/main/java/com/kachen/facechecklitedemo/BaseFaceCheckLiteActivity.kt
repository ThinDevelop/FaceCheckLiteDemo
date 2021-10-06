package com.kachen.facechecklitedemo

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.centerm.smartpos.aidl.sys.AidlDeviceManager

abstract class BaseFaceCheckLiteActivity : AppCompatActivity() {
    protected var manager: AidlDeviceManager? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService()
        mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }
    open fun bindService() {
        var intent = Intent()
        intent.setPackage("com.centerm.smartposservice")
        intent.action = "com.centerm.smartpos.service.MANAGER_SERVICE"
        bindService(intent, conn, BIND_AUTO_CREATE)

        intent = Intent()
        intent.setPackage("com.centerm.centermposoverseaservice")
        intent.action = "com.centerm.CentermPosOverseaService.MANAGER_SERVICE"
        bindService(intent, connNew, BIND_AUTO_CREATE)

    }

    open fun unbindService() {
        unbindService(conn)
        unbindService(connNew)

    }


    var connNew: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            manager = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            manager = AidlDeviceManager.Stub.asInterface(service)
            if (null != manager) {
                onDeviceConnected(manager, false)
            }
        }
    }

    protected var conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            manager = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            manager = AidlDeviceManager.Stub.asInterface(service)
            if (null != manager) {
                onDeviceConnected(manager, true)
            }
        }
    }

    protected open fun log(log: String?) {
        Log.i("Centerm", log)
    }

    abstract fun onDeviceConnected(deviceManager: AidlDeviceManager?, cpay: Boolean)
}