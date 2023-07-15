package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SaveImageToFileWorker"

class SaveImageToFileWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault())

    override fun doWork(): Result {

        makeStatusNotification("Saving image", applicationContext)
        sleep()

        return try {

            val resourceUri = inputData.getString(KEY_IMAGE_URI)

            val bitmap = BitmapFactory.decodeStream(applicationContext.contentResolver.openInputStream(Uri.parse(resourceUri)))

            // MediaStore.Images - collection of all media with MIME type
            val imageUrl = MediaStore.Images.Media
                .insertImage(applicationContext.contentResolver, bitmap, title, dateFormatter.format(Date()))

            if(!imageUrl.isNullOrEmpty()) {
                val output = workDataOf(KEY_IMAGE_URI to imageUrl)
                Result.success(output)
            } else {
                Log.e(TAG, "Writing to MediaStore failed!")
                Result.failure()
            }

        } catch(e: Exception) {
            e.printStackTrace()
            Result.failure()
        }

    }

}