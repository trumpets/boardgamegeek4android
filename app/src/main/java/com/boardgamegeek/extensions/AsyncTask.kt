@file:JvmName("TaskUtils")

package com.boardgamegeek.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Execute a background task using Kotlin Coroutines.
 * This replaces the deprecated AsyncTask pattern.
 * 
 * Note: Uses an unmanaged CoroutineScope. For Activities/Fragments, consider using
 * lifecycleScope or viewModelScope for automatic cancellation when lifecycle ends.
 * This implementation matches AsyncTask behavior where tasks continue even if the
 * calling component is destroyed.
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
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error executing background task")
            return@launch
        }
        
        // Execute onComplete callback - let exceptions propagate naturally
        try {
            onComplete?.invoke()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error in completion callback")
        }
    }
}

/**
 * Execute a background task with a result using Kotlin Coroutines.
 * This replaces the deprecated AsyncTask pattern.
 * 
 * Note: Uses an unmanaged CoroutineScope. For Activities/Fragments, consider using
 * lifecycleScope or viewModelScope for automatic cancellation when lifecycle ends.
 * This implementation matches AsyncTask behavior where tasks continue even if the
 * calling component is destroyed.
 */
fun <T> launchTaskWithResult(
    backgroundWork: suspend () -> T,
    onComplete: (T) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        val result: T
        try {
            result = withContext(Dispatchers.IO) {
                backgroundWork()
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error executing background task")
            return@launch
        }
        
        // Execute onComplete callback with result - let exceptions propagate naturally
        try {
            onComplete(result)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error in completion callback")
        }
    }
}

/**
 * Execute a background task with full AsyncTask-like lifecycle support using Kotlin Coroutines.
 * This replaces the deprecated AsyncTask pattern with onPreExecute and onPostExecute support.
 * 
 * Note: Uses an unmanaged CoroutineScope. For Activities/Fragments, consider using
 * lifecycleScope or viewModelScope for automatic cancellation when lifecycle ends.
 * This implementation matches AsyncTask behavior where tasks continue even if the
 * calling component is destroyed.
 */
fun <T> launchTaskWithLifecycle(
    onPreExecute: (() -> Unit)? = null,
    backgroundWork: suspend () -> T,
    onPostExecute: (T) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        // Execute onPreExecute on main thread before starting background work
        try {
            onPreExecute?.invoke()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error in pre-execution callback")
        }
        
        val result: T
        try {
            result = withContext(Dispatchers.IO) {
                backgroundWork()
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error executing background task")
            return@launch
        }
        
        // Execute onPostExecute callback with result on main thread
        try {
            onPostExecute(result)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error in post-execution callback")
        }
    }
}

