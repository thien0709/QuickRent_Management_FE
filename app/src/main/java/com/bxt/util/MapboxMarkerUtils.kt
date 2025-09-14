// com/bxt/util/MapboxMarkerUtils.kt
package com.bxt.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.toColorInt
import com.bxt.R
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

object MapboxMarkerUtils {

    // Cache để tăng hiệu năng cho các marker mới
    private val bitmapCache = LruCache<String, Bitmap>(5)

    // --- CÁC HÀM MỚI ĐƯỢC BỔ SUNG ---

    fun createStartMarker(point: Point): PointAnnotationOptions {
        val bitmap = bitmapCache.get("start") ?: createColoredCircleBitmap("#4CAF50").also {
            bitmapCache.put("start", it)
        }
        return createBaseOptions(point, bitmap, "Xuất phát")
    }

    fun createDestinationMarker(point: Point): PointAnnotationOptions {
        val bitmap = bitmapCache.get("destination") ?: createColoredCircleBitmap("#F44336").also {
            bitmapCache.put("destination", it)
        }
        return createBaseOptions(point, bitmap, "Điểm đến")
    }

    fun createUserLocationMarker(point: Point): PointAnnotationOptions {
        val bitmap = bitmapCache.get("user_location") ?: createColoredCircleBitmap("#2196F3").also {
            bitmapCache.put("user_location", it)
        }
        return createBaseOptions(point, bitmap, "Vị trí của bạn")
    }

    fun createPickupMarker(point: Point, itemName: String): PointAnnotationOptions {
        val bitmap = bitmapCache.get("pickup") ?: createColoredCircleBitmap("#FF9800").also {
            bitmapCache.put("pickup", it)
        }
        return createBaseOptions(point, bitmap, "📦 Lấy: $itemName")
    }

    fun createDeliveryMarker(point: Point, itemName: String): PointAnnotationOptions {
        val bitmap = bitmapCache.get("delivery") ?: createColoredCircleBitmap("#9C27B0").also {
            bitmapCache.put("delivery", it)
        }
        return createBaseOptions(point, bitmap, "🚚 Giao: $itemName")
    }

    // --- CÁC HÀM CŨ CỦA BẠN (GIỮ NGUYÊN) ---

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
        @DrawableRes iconResId: Int = R.drawable.ic_launcher_foreground,
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

    // --- CÁC HÀM HELPER (PRIVATE) ---

    private fun createBaseOptions(point: Point, icon: Bitmap, label: String): PointAnnotationOptions {
        return PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(icon)
            .withTextField(label)
            .withTextSize(12.0)
            .withTextAnchor(TextAnchor.TOP)
            .withTextOffset(listOf(0.0, 1.0))
            .withTextColor("#000000")
            .withTextHaloColor("#FFFFFF")
            .withTextHaloWidth(2.0)
    }

    private fun createColoredCircleBitmap(colorHex: String): Bitmap {
        val size = 52
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorHex.toColorInt()
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val radius = (size / 2f) - strokePaint.strokeWidth
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
        return bitmap
    }

    /**
     * Sửa lại hàm cũ của bạn để không bị crash và hoạt động ổn định.
     */
    private fun createBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
            ?: throw IllegalArgumentException("Drawable resource not found!")

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}