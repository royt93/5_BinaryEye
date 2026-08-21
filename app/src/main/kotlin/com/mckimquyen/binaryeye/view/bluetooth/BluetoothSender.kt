package com.mckimquyen.binaryeye.view.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.preference.ListPreference
import com.mckimquyen.binaryeye.database.Scan
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "roy93~BluetoothSender"

// [FIX ML-4] Nhan CoroutineScope tu caller thay vi dung GlobalScope
fun Scan.sendBluetoothAsync(
    host: String,
    scope: CoroutineScope,
    callback: (Boolean, Boolean) -> Unit,
) {
    scope.launch(Dispatchers.IO) {
        val (connected, sent) = BluetoothConnectionManager.sendTo(host, content)
        withContext(Dispatchers.Main) {
            callback(connected, sent)
        }
    }
}

fun setBluetoothHosts(listPref: ListPreference) {
    // [FIX ML-3] getDefaultAdapter() deprecated API 31, co the tra ve null
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
        Log.w(TAG, "setBluetoothHosts: device has no Bluetooth adapter")
        return
    }
    val devices = try {
        adapter.bondedDevices
    } catch (e: SecurityException) {
        // Do nothing, either the user has denied Bluetooth access
        // or the permission was removed by the system. We're catching
        // the exception to keep the app from crashing.
        Log.w(TAG, "setBluetoothHosts: SecurityException – Bluetooth permission denied", e)
        null
    } ?: return
    listPref.entries = devices.map { it.name }.toTypedArray()
    listPref.entryValues = devices.map { it.address }.toTypedArray()
    listPref.callChangeListener(listPref.value)
}

// [FIX ML-3] Boc Bluetooth state vao object de co synchronization va null-safe adapter
internal object BluetoothConnectionManager {
    private val uuid = UUID.fromString("8a8478c9-2ca8-404b-a0de-101f34ab71ae")

    // [FIX ML-3] getDefaultAdapter() deprecated; tra ve null-safe
    private val adapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var writer: OutputStreamWriter? = null

    @Volatile
    private var isConnected = false

    // [FIX ML-3] Dung @Synchronized de tranh race condition tu nhieu coroutine
    @Synchronized
    fun sendTo(host: String, message: String): Pair<Boolean, Boolean> {
        val connected = if (isConnected) true else connect(host)
        val sent = if (connected) send(message) else false
        return connected to sent
    }

    @Synchronized
    private fun connect(deviceAddress: String): Boolean = try {
        val device = findByAddress(deviceAddress) ?: run {
            Log.w(TAG, "connect: device not found for address $deviceAddress")
            return false
        }
        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket?.connect()
        writer = socket?.outputStream?.writer()
        isConnected = true
        Log.d(TAG, "connect: connected to $deviceAddress")
        true
    } catch (e: Exception) {
        Log.w(TAG, "connect: failed", e)
        close()
        false
    }

    @Synchronized
    private fun send(message: String): Boolean = try {
        writer?.apply {
            write(message)
            write("\n")
            flush()
        }
        true
    } catch (e: Exception) {
        Log.w(TAG, "send: failed", e)
        close()
        false
    }

    @Synchronized
    fun close() {
        try { writer?.close() } catch (e: IOException) { /* ignore */ }
        try { socket?.close() } catch (e: IOException) { /* ignore */ }
        writer = null
        socket = null
        isConnected = false
        Log.d(TAG, "close: Bluetooth connection released")
    }

    private fun findByAddress(address: String): BluetoothDevice? {
        val a = adapter ?: run {
            Log.w(TAG, "findByAddress: no Bluetooth adapter available")
            return null
        }
        return try {
            a.bondedDevices?.firstOrNull { it.address == address }
        } catch (e: SecurityException) {
            Log.w(TAG, "findByAddress: SecurityException", e)
            null
        }
    }
}
