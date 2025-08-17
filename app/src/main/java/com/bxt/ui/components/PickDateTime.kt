package com.bxt.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Calendar
import java.time.Instant


@RequiresApi(Build.VERSION_CODES.O)
fun PickDateTime(
    context: Context,
    initialDateTime: OffsetDateTime? = null,
    onDateTimePicked: (OffsetDateTime) -> Unit
) {
    val calendar = Calendar.getInstance()
    initialDateTime?.let {
        calendar.set(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute)
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    DatePickerDialog(context, { _, pickedYear, pickedMonth, pickedDay ->
        TimePickerDialog(context, { _, pickedHour, pickedMinute ->
            val picked = OffsetDateTime.of(
                pickedYear,
                pickedMonth + 1,
                pickedDay,
                pickedHour,
                pickedMinute,
                0, 0,
                ZoneId.systemDefault().rules.getOffset(Instant.now())
            )
            onDateTimePicked(picked)
        }, hour, minute, true).show()
    }, year, month, day).show()
}