package com.calendarb.widget

import android.app.Application
import com.calendarb.widget.data.TaskRepository

/**
 * Application: инициализирует доступ к данным, чтобы виджет-провайдер,
 * RemoteViewsService и приёмники уведомлений могли работать с БД.
 */
class CalendarbApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DataProvider.init(this)
        TaskRepository.get(this)
    }
}
