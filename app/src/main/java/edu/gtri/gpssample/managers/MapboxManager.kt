package edu.gtri.gpssample.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import edu.gtri.gpssample.database.*

class MapboxManager(
    var context: Context,
    var pointAnnotationManager: PointAnnotationManager?,
    var polygonAnnotationManager: PolygonAnnotationManager?
)
{
    fun addMarker( point: Point, @DrawableRes resourceId: Int ) : PointAnnotation?
    {
        val pointAnnotationOptions = PointAnnotationOptions().withPoint( point )

        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))?.let { bitmap ->
            pointAnnotationOptions.withIconImage( bitmap )
        }

        pointAnnotationManager?.let { pointAnnotationManager ->
            return pointAnnotationManager.create(pointAnnotationOptions)
        }

        return null
    }

    fun addPolygon( points: List<List<Point>> ) : PolygonAnnotation?
    {
        val polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points)
            .withFillOpacity(0.25 )

        polygonAnnotationManager?.let { polygonAnnotationManager ->
            return polygonAnnotationManager.create(polygonAnnotationOptions)
        }

        return null
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap?
    {
        if (sourceDrawable == null)
        {
            return null
        }

        return if (sourceDrawable is BitmapDrawable)
        {
            sourceDrawable.bitmap
        }
        else
        {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}