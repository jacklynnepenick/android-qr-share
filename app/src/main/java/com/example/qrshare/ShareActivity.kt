package com.example.qrshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix


class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        val intent: Intent = intent
        val action: String? = intent.action
        val type: String? = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent) // Handle text being sent
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        val sharedText: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            generateQRCode(sharedText)
        }
    }

    private fun generateQRCode(text: String?) {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        val minDimension = width.coerceAtMost(height)
        val writer = MultiFormatWriter()
        try {
            val matrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, minDimension, minDimension)
            val bitmap: Bitmap = Bitmap.createBitmap(minDimension, minDimension, Bitmap.Config.RGB_565)
            for (x in 0..<minDimension) {
                for (y in 0..<minDimension) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            val imageView: ImageView = findViewById(R.id.qrCodeImageView)
            imageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
