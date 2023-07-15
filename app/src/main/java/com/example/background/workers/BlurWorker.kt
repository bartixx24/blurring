package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R

private const val TAG = "BlurWorker"

class BlurWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {

        val appContext = applicationContext

        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        // notification banner
        makeStatusNotification("Blurring image", appContext)
        sleep()

        return try {

            // (IMPROVED) val picture = BitmapFactory.decodeResource(appContext.resources, R.drawable.android_cupcake)

            // check if the resourceUri obtained from the Data is not empty
            if(TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            val picture = BitmapFactory.decodeStream(appContext.contentResolver.openInputStream(Uri.parse(resourceUri)))

            val output = blurBitmap(picture, appContext)

            val outputUri = writeBitmapToFile(appContext, output)

            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            // notification banner
            makeStatusNotification("Output is $outputUri", appContext)

            Result.success(outputData)

        } catch(throwable: Throwable) {
            Log.e(TAG, "Error applying blur")
            throwable.printStackTrace()
            Result.failure()
        }

    }

}