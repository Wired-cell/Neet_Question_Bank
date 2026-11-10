package com.example.questionbank.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrUtils {
    
    private const val TAG = "OcrUtils"
    
    /**
     * Result of OCR processing with separated question and options
     */
    data class OcrResult(
        val fullText: String,
        val questionText: String,
        val options: List<String>,
        val hasDiagram: Boolean,
        val diagramRegion: Rect? = null
    )
    
    /**
     * Recognize text from an image URI using ML Kit
     */
    suspend fun recognizeTextFromUri(
        context: Context,
        recognizer: TextRecognizer,
        uri: Uri
    ): String {
        return try {
            val bitmap = getBitmapFromUri(context, uri)
            recognizeTextFromBitmap(recognizer, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recognize text from URI", e)
            ""
        }
    }
    
    /**
     * Recognize text from a Bitmap using ML Kit
     */
    suspend fun recognizeTextFromBitmap(
        recognizer: TextRecognizer,
        bitmap: Bitmap
    ): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = suspendCancellableCoroutine<Text> { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) continuation.resume(text)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }

            val blocks = result.textBlocks
            if (blocks.isEmpty()) {
                return ""
            }

            val stringBuilder = StringBuilder()
            for (block in blocks) {
                stringBuilder.append(block.text).append("\\n")
                for (line in block.lines) {
                    stringBuilder.append(line.text).append("\\n")
                }
            }
            stringBuilder.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recognize text from bitmap", e)
            ""
        }
    }

    /**
     * Process OCR text to extract question and options separately
     * This ALWAYS picks the LAST 4 options regardless of their labels
     * (as per user requirement: "the options should be the last ones")
     */
    fun processOcrText(fullText: String): OcrResult {
        val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Pattern to detect option-like lines
        // Very permissive - matches anything that looks like it could be an option
        val optionPattern = Regex(
            """^(?:\d+[)\.]\s*|[A-Da-d][)\.]\s*|[\(]?\d+[\)]\s*).+"""
        )
        
        val options = mutableListOf<String>()
        var questionEndIndex = lines.size
        
        // ALWAYS look for the last 4 lines that look like options
        // This handles the user's requirement: "the options should be the last ones"
        if (lines.size >= 4) {
            // Start from the end and work backwards
            val candidates = mutableListOf<String>()
            
            for (i in lines.indices.reversed()) {
                val line = lines[i]
                // Accept any non-empty line as a potential option
                // (user said "no matter their label" - so we take the last 4 non-empty lines)
                if (line.isNotBlank()) {
                    candidates.add(0, line) // Add at beginning to maintain order
                    if (candidates.size >= 4) break
                }
            }
            
            if (candidates.size == 4) {
                questionEndIndex = lines.size - 4
                options.addAll(candidates)
            } else if (candidates.size > 0) {
                // If we found some but not 4, take what we have
                questionEndIndex = lines.size - candidates.size
                options.addAll(candidates)
            }
        }
        
        // If we didn't find 4 options at the bottom, try looking for any 4 option-like lines
        if (options.size < 4) {
            val allOptionLines = lines.filter { line ->
                optionPattern.matches(line) || line.matches(Regex("""^[A-Da-d\d]+\)?\.?\s+.+"""))
            }
            
            // Take the last 4 option-like lines
            if (allOptionLines.size >= 4) {
                options.clear()
                options.addAll(allOptionLines.takeLast(4))
                questionEndIndex = lines.indexOfLast { allOptionLines.takeLast(4).contains(it) }
            }
        }
        
        // Extract question text (everything before the options)
        val questionLines = if (questionEndIndex > 0 && questionEndIndex <= lines.size) {
            lines.subList(0, questionEndIndex)
        } else {
            lines.toList()
        }
        
        val questionText = questionLines.joinToString("\n").trim()
        
        // Clean up options - remove any numbering/lettering at the start
        val cleanedOptions = options.mapIndexed { index, option ->
            val cleaned = option
                .replace(Regex("""^[\dA-Da-d]\)\s*"""), "")
                .replace(Regex("""^[\dA-Da-d]\.\s*"""), "")
                .replace(Regex("""^\(\d+\)\s*"""), "")
                .replace(Regex("""^[ivxlCDM]+[)\.]\s*"""), "")  // Handle roman numerals like (i), (ii), etc.
                .trim()
            
            // Add letter prefix (A, B, C, D)
            val letter = ('A' + index).toString()
            "$letter. $cleaned"
        }
        
        // Check if there's likely a diagram
        val hasDiagram = lines.any { line ->
            line.contains("[image]", ignoreCase = true) ||
            line.contains("figure", ignoreCase = true) ||
            line.contains("diagram", ignoreCase = true) ||
            line.contains("shown", ignoreCase = true) ||
            line.contains("as shown", ignoreCase = true)
        }
        
        return OcrResult(
            fullText = fullText,
            questionText = questionText,
            options = cleanedOptions.take(4), // Ensure we only return up to 4 options
            hasDiagram = hasDiagram
        )
    }
    
    /**
     * Extract only the options from OCR text
     */
    fun extractOptionsFromText(text: String): List<String> {
        return processOcrText(text).options
    }
    
    /**
     * Extract only the question text from OCR text
     */
    fun extractQuestionFromText(text: String): String {
        return processOcrText(text).questionText
    }
    
    /**
     * Get Bitmap from URI
     */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to get bitmap from URI", e)
            throw e
        }
    }
    
    /**
     * Create a text recognizer instance
     */
    fun createTextRecognizer(): TextRecognizer {
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    /**
     * Post-process OCR text to improve math symbol recognition
     */
    fun postProcessMathText(text: String): String {
        return text
            .replace("√", "\\sqrt")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("→", "->")
            .replace("←", "<-")
            .replace("≈", "~=")
            .replace("×", "x")
            .replace("÷", "/")
            .replace("±", "+/-")
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace("≠", "!=")
            .replace("°", " deg")
            .replace("·", ".")
            .replace("μ", "mu")
            .replace("λ", "lambda")
            .replace("Σ", "Sigma")
            .replace("∫", "integral")
            .replace("∆", "Delta")
            .replace("π", "pi")
            .replace("θ", "theta")
            .replace("α", "alpha")
            .replace("β", "beta")
            .replace("γ", "gamma")
            .replace("ω", "omega")
            .replace("φ", "phi")
            .replace("ψ", "psi")
            .replace("δ", "delta")
            .replace("ε", "epsilon")
            .replace("ζ", "zeta")
            .replace("η", "eta")
            .replace("ι", "iota")
            .replace("κ", "kappa")
            .replace("ν", "nu")
            .replace("ξ", "xi")
            .replace("ρ", "rho")
            .replace("σ", "sigma")
            .replace("τ", "tau")
            .replace("υ", "upsilon")
            .replace("χ", "chi")
            .trim()
    }
    
    /**
     * Detect if text contains math symbols
     */
    fun containsMathSymbols(text: String): Boolean {
        val mathSymbols = setOf(
            "√", "²", "³", "→", "←", "≈", "×", "÷", "±", "≤", "≥", "≠", 
            "°", "·", "μ", "λ", "Σ", "∫", "∆", "π", "θ", "α", "β", "γ",
            "ω", "φ", "ψ", "δ", "ε", "ζ", "η", "ι", "κ", "ν", "ξ", "ρ",
            "σ", "τ", "υ", "χ"
        )
        return mathSymbols.any { text.contains(it) }
    }
}
