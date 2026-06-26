package com.mckimquyen.binaryeye.sv

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.mckimquyen.binaryeye.view.act.ActivityCamera

@RequiresApi(Build.VERSION_CODES.N)
class ScanTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(applicationContext, ActivityCamera::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }
}
