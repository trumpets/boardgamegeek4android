@file:JvmName("TaskUtils")

package com.boardgamegeek.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Execute a background task using Kotlin Coroutines.
 * This replaces the deprecated AsyncTask pattern.
 */
fun launchTask(
    backgroundWork: suspend () -> Unit,
    onComplete: (() -> Unit)? = null
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            withContext(Dispatchers.IO) {
                backgroundWork()
            }
            onComplete?.invoke()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error executing background task")
        }
    }
}

/**
 * Execute a background task with a result using Kotlin Coroutines.
 * This replaces the deprecated AsyncTask pattern.
 */
fun <T> launchTaskWithResult(
    backgroundWork: suspend () -> T,
    onComplete: (T) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val result = withContext(Dispatchers.IO) {
                backgroundWork()
            }
            onComplete(result)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error executing background task")
        }
    }
}