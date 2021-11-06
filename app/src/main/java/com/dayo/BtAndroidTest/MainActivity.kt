package com.dayo.BtAndroidTest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import androidx.fragment.app.FragmentActivity
import java.io.IOException
import java.lang.Exception
import java.lang.reflect.Method
import java.util.*
import android.os.Looper
import android.os.Message
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    lateinit var btAdapter: BluetoothAdapter
    lateinit var btSocket: BluetoothSocket
    lateinit var connection: BTConnection


    companion object {
        public val MESSAGE_READ = 2 // used in bluetooth handler to identify message update
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permission_list = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityCompat.requestPermissions(this@MainActivity, permission_list, 1)
        val btManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter
        if(!btAdapter.isEnabled)
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
    }

    override fun onStart()
    {
        super.onStart()
        connectPairedHC06()
    }

    private fun connectPairedHC06() {
        for(x in btAdapter.bondedDevices) {
            Log.d("asdf", x.name)
            if(x.name == "HC-06") {
                Log.d("asdf", "HC06 found")
                Log.d("asdf", x.address)
                if(btAdapter.getRemoteDevice(x.address) == null) Log.d("asdf", "NULL!")
                else {
                    var flag = false;
                    Thread {
                        btSocket = createBluetoothSocket(btAdapter.getRemoteDevice(x.address))!!
                        btSocket.connect()

                        val mHandler = object : Handler(Looper.getMainLooper()) {
                            override fun handleMessage(msg: Message) {
                                Log.d("asdf", "recvmsg")
                                if (msg.what == MESSAGE_READ) {
                                    Log.d("asdf", "asdfasdfasdf")
                                    var readMessage: String? = null
                                    try
                                    {
                                        readMessage = String(
                                            (msg.obj as ByteArray)!!,
                                            Charset.forName("UTF-8")
                                        )
                                    } catch (e: UnsupportedEncodingException) {
                                        e.printStackTrace()
                                    }
                                    Log.d("asdf", readMessage!!)
                                    val parsedData = readMessage.split('|')
                                    val humi = Integer.parseInt(parsedData[0])
                                    val temp = Integer.parseInt(parsedData[1])
                                    val pm1_0 = Integer.parseInt(parsedData[2])
                                    val pm2_5 = Integer.parseInt(parsedData[3])
                                    val pm10_0 = Integer.parseInt(parsedData[4])
                                    Log.d("asdf", "humi: $humi, temp: $temp, pm1_0: $pm1_0, pm2_5: $pm2_5, pm10_0: $pm10_0")
                                }
                            }
                        }

                        connection = BTConnection(btSocket, mHandler)
                        flag = true
                        connection.run()
                    }.start()
                    while(!flag){}

                    connection.write("a")
                }
            }
        }
    }

    fun searchBT() {
        if(btAdapter.isDiscovering) btAdapter.cancelDiscovery()
        else if(btAdapter.isEnabled) {
            btAdapter.startDiscovery()
            registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        }
        else Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        //unregisterReceiver(receiver)
        super.onDestroy()
    }

    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(BluetoothDevice.ACTION_FOUND == intent?.action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if(device?.name == "HC-06"){}
            }
        }
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket? {
        val uid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        return try {
            device.createInsecureRfcommSocketToServiceRecord(uid)
        } catch (e: Exception) {
            device.createRfcommSocketToServiceRecord(uid)
        }
    }
}