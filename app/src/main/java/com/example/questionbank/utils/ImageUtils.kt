package com.example.questionbank.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    
    private const val TAG = "ImageUtils"
    
    /**
     * Create a temporary file for storing images
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JB_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    /**
     * Save bitmap to file
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap to file", e)
            false
        }
    }
    
    /**
     * Get bitmap from URI
     */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get bitmap from URI", e)
            null
        }
    }
    
    /**
     * Crop bitmap to a rectangle
     */
    fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }
    
    /**
     * Crop bitmap with aspect ratio
     */
    fun cropBitmapToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val newWidth: Int
        val newHeight: Int
        
        if (width / height > aspectRatio) {
            newHeight = height
            newWidth = (height * aspectRatio).toInt()
        } else {
            newWidth = width
            newHeight = (width / aspectRatio).toInt()
        }
        
        val x = (width - newWidth) / 2
        val y = (height - newHeight) / 2
        
        return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
    }
    
    /**
     * Resize bitmap
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratio = width.toFloat() / height
        
        val newWidth: Int
        val newHeight: Int
        
        if (ratio > 1) {
            newWidth = maxWidth
            newHeight = (maxWidth / ratio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Rotate bitmap
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Create a bitmap with a border
     */
    fun addBorderToBitmap(bitmap: Bitmap, borderColor: Int, borderWidth: Int): Bitmap {
        val width = bitmap.width + borderWidth * 2
        val height = bitmap.height + borderWidth * 2
        
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint()
        paint.color = borderColor
        paint.style = Paint.Style.FILL
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawBitmap(bitmap, borderWidth.toFloat(), borderWidth.toFloat(), null)
        
        return output
    }
    
    /**
     * Detect if an image is a diagram (physics or biology)
     * This is a simple heuristic based on image characteristics
     */
    fun isLikelyDiagram(bitmap: Bitmap): Boolean {
        // Simple heuristic: diagrams often have more edges and less uniform color
        // This is a placeholder - in a real app, you'd use ML
        
        // Check if image is mostly line drawings (low color variation)
        val edgeDensity = calculateEdgeDensity(bitmap)
        val colorVariance = calculateColorVariance(bitmap)
        
        // Diagrams typically have high edge density and low color variance
        return edgeDensity > 0.3 && colorVariance < 0.5
    }
    
    /**
     * Calculate edge density (simplified)
     */
    private fun calculateEdgeDensity(bitmap: Bitmap): Float {
        // This is a very simplified version
        // In a real app, use edge detection algorithms
        var edgePixels = 0
        var totalPixels = 0
        
        for (y in 1 until bitmap.height - 1) {
            for (x in 1 until bitmap.width - 1) {
                val pixel = bitmap.getPixel(x, y)
                val neighbor = bitmap.getPixel(x + 1, y)
                
                // Simple edge detection: if pixel differs significantly from neighbor
                if (colorDistance(pixel, neighbor) > 100) {
                    edgePixels++
                }
                totalPixels++
            }
        }
        
        return if (totalPixels > 0) edgePixels.toFloat() / totalPixels else 0f
    }
    
    /**
     * Calculate color variance
     */
    private fun calculateColorVariance(bitmap: Bitmap): Float {
        var sumR = 0
        var sumG = 0
        var sumB = 0
        var count = 0
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                sumR += (pixel and 0xFF0000) ushr 16
                sumG += (pixel and 0xFF00) ushr 8
                sumB += pixel and 0xFF
                count++
            }
        }
        
        if (count == 0) return 0f
        
        val meanR = sumR.toFloat() / count
        val meanG = sumG.toFloat() / count
        val meanB = sumB.toFloat() / count
        
        var varianceR = 0f
        var varianceG = 0f
        var varianceB = 0f
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel and 0xFF0000) ushr 16
                val g = (pixel and 0xFF00) ushr 8
                val b = pixel and 0xFF
                
                varianceR += (r - meanR) * (r - meanR)
                varianceG += (g - meanG) * (g - meanG)
                varianceB += (b - meanB) * (b - meanB)
            }
        }
        
        val totalVariance = (varianceR + varianceG + varianceB) / (count * 3)
        return totalVariance / (255f * 255f) // Normalize to 0-1
    }
    
    /**
     * Calculate color distance between two pixels
     */
    private fun colorDistance(pixel1: Int, pixel2: Int): Float {
        val r1 = (pixel1 and 0xFF0000) ushr 16
        val g1 = (pixel1 and 0xFF00) ushr 8
        val b1 = pixel1 and 0xFF
        
        val r2 = (pixel2 and 0xFF0000) ushr 16
        val g2 = (pixel2 and 0xFF00) ushr 8
        val b2 = pixel2 and 0xFF
        
        return Math.sqrt(
            Math.pow((r1 - r2).toDouble(), 2.0) +
            Math.pow((g1 - g2).toDouble(), 2.0) +
            Math.pow((b1 - b2).toDouble(), 2.0)
        ).toFloat()
    }
}
