/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide.init
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker

private const val TAG = "BlurViewModel"


class BlurViewModel(application: Application) : ViewModel() {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    private val workManager = WorkManager.getInstance(application)

    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        imageUri = getImageUri(application.applicationContext)
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }

    /** create the input data bundle with the Uri to operate on */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }


    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        // There is also PeriodicWorkRequest
        // (IMPROVED) workManager.enqueue(OneTimeWorkRequest.from(BlurWorker::class.java))

        // (IMPROVED 2) val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>().setInputData(createInputDataForUri()).build()
        // (IMPROVED 2) workManager.enqueue(blurRequest)

        // (IMPROVED 3) val blurRequest = OneTimeWorkRequest.Builder(BlurWorker::class.java).setInputData(createInputDataForUri()).build()

        // (IMPROVED 4) var workContinuation = workManager.beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))

        Log.d(TAG, "applyBlur() called!")

        var workContinuation = workManager
            .beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequest.from(CleanupWorker::class.java))

        for(i in 0 until blurLevel) {
            val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            if(i == 0) { blurRequest.setInputData(createInputDataForUri()) }
            workContinuation = workContinuation.then(blurRequest.build())
        }

        val constraints = Constraints.Builder().setRequiresCharging(true).build()

        val saveRequest = OneTimeWorkRequest.Builder(SaveImageToFileWorker::class.java)
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()
        workContinuation = workContinuation.then(saveRequest)

        workContinuation.enqueue()

    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
