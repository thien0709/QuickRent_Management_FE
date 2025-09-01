package com.bxt.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.geojson.Point
import com.bxt.R // Import R từ package của bạn

object MapboxMarkerUtils {

    fun createSimpleMarker(
        context: Context,
        point: Point,
        title: String = "Vị trí giao hàng",
        isDraggable: Boolean = false
    ): PointAnnotationOptions {
        return PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(createBitmapFromDrawable(context, R.drawable.ic_map_pin))
            .withTextField(title)
            .withTextOffset(listOf(0.0, -2.5))
            .withDraggable(isDraggable)
    }


    fun createCustomIconMarker(
        context: Context,
        point: Point,
        iconResId: Int = R.drawable.ic_launcher_foreground,
        title: String = "Vị trí giao hàng"
    ): PointAnnotationOptions {
        return PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(createBitmapFromDrawable(context, iconResId))
            .withTextField(title)
            .withTextOffset(listOf(0.0, -2.0))
    }

    fun createColoredMarker(
        point: Point,
        title: String = "Vị trí giao hàng",
        color: String = "#FF0000"
    ): PointAnnotationOptions {
        return PointAnnotationOptions()
            .withPoint(point)
            .withTextField(title)
            .withTextOffset(listOf(0.0, -2.0))
            .withIconColor(color)
    }

    private fun createBitmapFromDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}