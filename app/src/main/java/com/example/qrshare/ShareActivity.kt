package com.example.qrshare

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.InputStreamReader


class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent)
            } else {
                val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (fileUri != null) {
                    startWebServer(fileUri)
                    setupPortForwarding()
                }
            }
        }
    }
    private var webServer: NanoHTTPD? = null

    private fun startWebServer(fileUri: Uri) {
        webServer = object : NanoHTTPD(8080) {
            override fun serve(session: IHTTPSession): Response {
                return try {
                    val inputStream = contentResolver.openInputStream(fileUri)
                    val fileName = getFileName(fileUri)

                    val response = newChunkedResponse(Response.Status.OK, getMimeType(fileUri.toString()), inputStream)
                    response.addHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
                    response
                } catch (e: Exception) {
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error")
                }
            }
        }
        webServer?.start()
    }
    private fun getFileName(uri: Uri): String {
        var name = ""
        if (uri.scheme.equals("content")) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                val columnIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor != null && cursor.moveToFirst() && columnIndex!! >= 0) {
                    name = cursor.getString(columnIndex)
                }
            }
        }
        return name
    }

    private var sshSession: Session? = null

    private fun setupPortForwarding() {
        Thread {
            try {
                val jsch = JSch()
                val session = jsch.getSession("qr_share_app", "serveo.net", 22) ?: return@Thread
                val keyString = "AAAAB3NzaC1yc2EAAAADAQABAAABAQDxYGqSKVwJpQD1F0YIhz+bd5lpl7YesKjtrn1QD1RjQcSj724lJdCwlv4J8PcLuFFtlAA8AbGQju7qWdMN9ihdHvRcWf0tSjZ+bzwYkxaCydq4JnCrbvLJPwLFaqV1NdcOzY2NVLuX5CfY8VTHrps49LnO0QpGaavqrbk+wTWDD9MHklNfJ1zSFpQAkSQnSNSYi/M2J3hX7P0G2R7dsUvNov+UgNKpc4n9+Lq5Vmcqjqo2KhFyHP0NseDLpgjaqGJq2Kvit3QowhqZkK4K77AA65CxZjdDfpjwZSuX075F9vNi0IFpFkGJW9KlrXzI4lIzSAjPZBURhUb8nZSiPuzj"
                val key: ByteArray = Base64.decode(keyString, Base64.DEFAULT)
                val hostKey = HostKey("serveo.net", key)
                jsch.hostKeyRepository.add(hostKey, null)
                // session.setConfig("StrictHostKeyChecking", "no") // Uncomment if the host key starts changing frequently
                session.connect()
                sshSession = session
                val channel = session.openChannel("shell")
                channel.connect()
                val input = channel.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(input))
                session.setPortForwardingR(80, "localhost", 8080)

                var line: String
                while (bufferedReader.readLine().also { line = it } != null) {
                    if (line.contains("https://")) {
                        val url = extractUrl(line)
                        runOnUiThread {
                            generateQRCode(url)
                        }
                        break
                    }
                }

            } catch (e: Exception) {
                print("wtf")
                e.printStackTrace()
            }
        }.start()
    }

    private fun extractUrl(line: String): String {
        val regex = Regex("https://\\S+")
        val matchResult = regex.find(line)
        return matchResult?.value ?: ""
    }

    override fun onDestroy() {
        sshSession?.disconnect()
        webServer?.stop()
        super.onDestroy()
    }

    private fun getMimeType(url: String): String {
        val type = if (url.lastIndexOf('.') == -1) {
            "application/octet-stream"
        } else {
            MimeTypeMap.getFileExtensionFromUrl(url)?.let { ext ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            } ?: "application/octet-stream"
        }
        return type
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
