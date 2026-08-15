package com.miolonixc.vibro17.ui

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.miolonixc.vibro17.engine.VibrationEngine
import com.miolonixc.vibro17.model.Effects

/**
 * Quick Settings tile: tap to toggle a vibration effect (drill by default).
 * Available on Android 7.0+ (API 24).
 */
class VibroTileService : TileService() {

    private lateinit var engine: VibrationEngine
    private var playing = false

    override fun onCreate() {
        super.onCreate()
        engine = VibrationEngine(this)
    }

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        if (playing) {
            engine.stop()
            playing = false
        } else {
            val effect = Effects.ALL.firstOrNull { it.id == "drill" } ?: Effects.ALL.first()
            engine.play(effect)
            playing = true
        }
        updateTile()
    }

    override fun onStopListening() {
        engine.stop()
        playing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (playing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (playing) "Vibro: вкл" else "Vibro 17"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (playing) "Дрель" else "Запустить"
        }
        tile.updateTile()
    }
}
