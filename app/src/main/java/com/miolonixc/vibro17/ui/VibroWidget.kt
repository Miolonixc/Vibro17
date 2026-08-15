package com.miolonixc.vibro17.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import com.miolonixc.vibro17.engine.VibrationEngine
import com.miolonixc.vibro17.model.Effects
import com.miolonixc.vibro17.R

/**
 * Home-screen widget with quick buttons: Поезд, Вечеринка, Стоп.
 * Button taps are sent as broadcasts handled in [onReceive].
 */
class VibroWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val views = android.widget.RemoteViews(context.packageName, R.layout.widget_vibro)
            views.setOnClickPendingIntent(R.id.wBtnTrain, pending(context, "train"))
            views.setOnClickPendingIntent(R.id.wBtnParty, pending(context, "party"))
            views.setOnClickPendingIntent(R.id.wBtnStop, pending(context, "stop"))
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action.startsWith(ACTION_PREFIX)) {
            val engine = VibrationEngine(context)
            val effectId = action.removePrefix(ACTION_PREFIX)
            if (effectId == "stop") {
                engine.stop()
            } else {
                Effects.ALL.firstOrNull { it.id == effectId }?.let { engine.play(it) }
            }
        }
        super.onReceive(context, intent)
    }

    private fun pending(context: Context, effectId: String): PendingIntent {
        val intent = Intent(context, VibroWidget::class.java).setAction(ACTION_PREFIX + effectId)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, effectId.hashCode(), intent, flags)
    }

    companion object {
        const val ACTION_PREFIX = "com.miolonixc.vibro17.WIDGET_"
    }
}
