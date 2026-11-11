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

            // ML Kit's block.text already contains the block's lines. Appending both
            // block.text and line.text duplicated every recognized sentence.
            val seen = linkedSetOf<String>()
            val orderedLines = blocks.flatMap { block ->
                block.lines.map { line -> line.text.trim() }
            }.filter { it.isNotBlank() && seen.add(it) }
            orderedLines.joinToString("\n")
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
        // Normalize OCR output: remove repeated lines and whitespace-only differences.
        val lines = fullText.lineSequence()
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        // Only explicit numbered/lettered answer markers start an option.
        val optionStart = Regex("""^\s*(?:\(?[1-4]\s*[).]|[A-Da-d]\s*[).])\s*\S.*$""")
        val firstOption = lines.indexOfFirst { optionStart.matches(it) }
        val questionLines = if (firstOption >= 0) lines.take(firstOption) else lines
        val optionLines = if (firstOption >= 0) lines.drop(firstOption) else emptyList()

        // Join wrapped option lines into the preceding option instead of creating
        // artificial indented/one-line paragraphs.
        val groupedOptions = mutableListOf<String>()
        optionLines.forEach { line ->
            if (optionStart.matches(line) && groupedOptions.size < 4) {
                groupedOptions.add(line)
            } else if (groupedOptions.isNotEmpty()) {
                groupedOptions[groupedOptions.lastIndex] =
                    groupedOptions.last() + " " + line
            }
        }

        val cleanedOptions = groupedOptions.take(4).mapIndexed { index, option ->
            val content = option.replace(
                Regex("""^\s*(?:\(?[1-4]\s*[).]|[A-Da-d]\s*[).])\s*"""),
                ""
            ).trim()
            "${('A' + index)}. $content"
        }

        // OCR line boxes are often split at the printed page's line width.
        // A single space gives a readable paragraph and avoids false indentation.
        val questionText = questionLines.joinToString(" ").replace(Regex("\\s+"), " ").trim()

        val hasDiagram = lines.any { line ->
            line.contains("[image]", ignoreCase = true) ||
                line.contains("figure", ignoreCase = true) ||
                line.contains("diagram", ignoreCase = true) ||
                line.contains("shown", ignoreCase = true)
        }

        return OcrResult(
            fullText = lines.joinToString("\n"),
            questionText = questionText,
            options = cleanedOptions,
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
            .replace("√", "sqrt")
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
