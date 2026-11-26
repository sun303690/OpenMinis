package com.openminis.app.data.repository

import android.content.Context
import android.util.Log
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.logging.AppLogger
import com.openminis.app.ui.components.QuickTestKind
import com.openminis.app.ui.components.QuickTestState
import com.openminis.app.ui.components.applicableKinds
import com.openminis.app.ui.components.performTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * [T-android-model-auto-check] Background bulk availability check with
 * auto-apply, driven by the "自动检查可用模型" toggle
 * ([com.openminis.app.data.ModelAutoCheckPrefs]).
 *
 * Runs the SAME real-provider smoke test as the manual self-check sheet
 * ([performTest]) against every visible model of one provider instance, then
 * pushes each result into the entry's real `isSelected` flag:
 * available → checked, unavailable → unchecked. A model whose probe never
 * finished keeps its existing choice.
 *
 * Deliberately NOT on any screen's scope: it is triggered right after a list
 * fetch (key save / Refresh / OAuth sign-in), and the user may navigate away
 * while it runs. One run per instance at a time — starting a new run cancels
 * the previous one instead of doubling the request load.
 */
object ModelAutoChecker {
    private const val TAG = "ModelAutoCheck"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = java.util.concurrent.ConcurrentHashMap<String, Job>()

    /**
     * Probe every visible model of [instanceId] and auto-apply availability to
     * its checked state. Returns the run's [Job] (already started), or null
     * when there is nothing to check.
     */
    fun checkAndApply(
        context: Context,
        providerRepository: ProviderRepository,
        instanceId: String,
    ): Job? {
        val entries = providerRepository.entriesFor(instanceId).filter { !it.isHidden }
        if (entries.isEmpty()) return null
        running.remove(instanceId)?.cancel()
        val job = scope.launch {
            AppLogger.info(TAG, "auto-check start instance=${instanceId.take(8)} models=${entries.size}")
            val appContext = context.applicationContext
            val semaphore = Semaphore(3)
            // [FIX-261105-batch-probe-apply] Collect every probe result and
            // persist them with ONE saveConfig. The old per-model
            // updateEntry() did a full config serialize + DB replaceAll +
            // prefs commit for EACH of N models (O(N²), main-thread blocking
            // via runBlocking), which stalled / OOM'd on large model lists.
            val results = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
            coroutineScope {
                entries.forEach { entry ->
                    launch {
                        val selected = probe(entry, providerRepository, appContext, semaphore)
                        if (selected != null) results[entry.id] = selected
                    }
                }
            }
            if (results.isNotEmpty()) providerRepository.applySelectionResults(results)
            AppLogger.info(TAG, "auto-check done instance=${instanceId.take(8)} results=${results.size}")
        }
        running[instanceId] = job
        job.invokeOnCompletion { running.remove(instanceId, job) }
        return job
    }

    /**
     * @return `true` = available (select), `false` = unavailable (deselect),
     * `null` = probe never finished, keep the user's existing choice.
     * Pure probe: persistence is batched by the caller after all probes finish.
     */
    private suspend fun probe(
        entry: ModelEntry,
        providerRepository: ProviderRepository,
        context: Context,
        semaphore: Semaphore,
    ): Boolean? {
        val applicable = applicableKinds(entry)
        val kind = applicable.firstOrNull { it == QuickTestKind.TEXT }
            ?: applicable.firstOrNull()
            ?: QuickTestKind.TEXT
        val result = try {
            semaphore.withPermit { performTest(kind, entry, providerRepository, context) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "probe error ${entry.model.id}: ${e.javaClass.simpleName}: ${e.message}")
            return null // probe never finished → keep the user's existing choice
        }
        return when (result) {
            is QuickTestState.Failure -> {
                AppLogger.warning(TAG, "FAIL ${entry.model.id}: ${result.message} → deselected")
                false
            }
            else -> {
                AppLogger.info(TAG, "OK ${entry.model.id} → selected")
                true
            }
        }
    }
}
