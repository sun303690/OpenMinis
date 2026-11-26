package com.openminis.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.logging.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "ModelSelfCheck"

/**
 * [T-android-model-self-check] Bulk "can I actually use this model" check for
 * the models the user selected on the provider screen.
 *
 * Reuses the SAME real-provider smoke test as [QuickTestSheet] (via
 * [performTest]) so a green result means the credential + model + capability
 * path genuinely responded — never a mere `GET /models` reachability probe.
 *
 * Design notes (mirrors iOS `ModelQuickTestSheet` bulk check):
 *  - Manual, opt-in: only ever runs when the user taps "Self-check selected".
 *    No automatic/background probing that would quietly bill every model.
 *  - Low concurrency: at most 3 models are tested at once via [Semaphore].
 *  - Cancellable: the run Job is cancelled on sheet dismiss and re-run.
 *  - Auto-applies: each model's result is pushed back to its real `isSelected`
 *    flag the moment the probe returns (`Ok` → checked, `Failed` → unchecked),
 *    so the user never has to tap "apply". A `Pending`/`Running` model keeps its
 *    existing choice until its own probe completes.
 *  - Image/speech-only models are tested with their primary capability kind
 *    (chosen by [applicableKinds]); text-capable models use a real completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelfCheckSheet(
    entries: List<ModelEntry>,
    providerRepository: ProviderRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val items = remember(entries) { entries.map { SelfCheckItem(it) } }
    var runJob by remember { mutableStateOf<Job?>(null) }
    val semaphore = remember { Semaphore(3) }

    val isRunning by remember { derivedStateOf { items.any { it.status is SelfCheckStatus.Running } } }
    val okCount by remember { derivedStateOf { items.count { it.status is SelfCheckStatus.Ok } } }

    fun kindFor(entry: ModelEntry): QuickTestKind {
        val applicable = applicableKinds(entry)
        // Prefer a real text completion when the model supports it; otherwise
        // fall back to its most distinctive capability (image / speech / …).
        return applicable.firstOrNull { it == QuickTestKind.TEXT }
            ?: applicable.firstOrNull()
            ?: QuickTestKind.TEXT
    }

    // [FIX-261105-batch-probe-apply] Results collected during one run, kept at
    // composable scope so closing the sheet mid-run can still flush them.
    val pending = remember { java.util.concurrent.ConcurrentHashMap<String, Boolean>() }

    /** Persist everything collected so far in ONE write, off this screen's
     *  scope (it may already be cancelled when the sheet is dismissed). */
    fun flush() {
        if (pending.isEmpty()) return
        val snapshot = pending.toMap()
        pending.clear()
        providerRepository.applySelectionResultsAsync(snapshot)
    }

    /**
     * [FIX-261105-batch-probe-apply] Accumulate one model's result instead of
     * writing it immediately. The visible per-item status still flips the
     * instant its probe finishes; only the persisted `isSelected` flags are
     * applied together at the end of the run. Per-model updateEntry() used to
     * do O(N²) JSON/DB work and N main-thread disk commits on large lists —
     * the same defect behind the "Select all" crash.
     */
    fun collectOne(item: SelfCheckItem, sink: MutableMap<String, Boolean>) {
        when (item.status) {
            is SelfCheckStatus.Ok -> sink[item.entry.id] = true
            is SelfCheckStatus.Failed -> sink[item.entry.id] = false
            else -> Unit // Pending / Running: keep the user's existing choice.
        }
    }

    fun runAll() {        runJob?.cancel()
        val job = Job()
        runJob = job
        items.forEach { it.status = SelfCheckStatus.Pending; it.elapsedMs = 0 }
        pending.clear()
        scope.launch(job) {
            coroutineScope {
                items.forEach { item ->
                    launch {
                        item.status = SelfCheckStatus.Running
                        val start = System.currentTimeMillis()
                        val result = semaphore.withPermit {
                            performTest(kindFor(item.entry), item.entry, providerRepository, context)
                        }
                        item.elapsedMs = System.currentTimeMillis() - start
                        item.status = when (result) {
                            is QuickTestState.Failure -> SelfCheckStatus.Failed(result.message)
                            else -> SelfCheckStatus.Ok(item.elapsedMs)
                        }
                        collectOne(item, pending)
                        when (item.status) {
                            is SelfCheckStatus.Failed ->
                                AppLogger.warning(TAG, "FAIL ${item.entry.model.id}: ${(item.status as SelfCheckStatus.Failed).message}")
                            else ->
                                AppLogger.info(TAG, "OK ${item.entry.model.id} in ${item.elapsedMs}ms")
                        }
                    }
                }
            }
            flush()
            AppLogger.info(TAG, "self-check run finished")
        }
    }

    androidx.compose.runtime.LaunchedEffect(entries) { runAll() }
    DisposableEffect(Unit) { onDispose { runJob?.cancel(); flush() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.model_self_check_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { runAll() }, enabled = !isRunning) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.model_self_check_retry))
                }
                MinisTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.model_self_check_done))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.model_self_check_summary, okCount, items.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.model_self_check_autosave),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))

            items.forEach { item -> SelfCheckRow(item) }
        }
    }
}

/** Per-model mutable self-check status, observed by its row. */
private class SelfCheckItem(val entry: ModelEntry) {
    var status by mutableStateOf<SelfCheckStatus>(SelfCheckStatus.Pending)
    var elapsedMs by mutableStateOf(0L)
}

/** Lifecycle of a single model's availability probe. */
private sealed interface SelfCheckStatus {
    object Pending : SelfCheckStatus
    object Running : SelfCheckStatus
    data class Ok(val elapsedMs: Long) : SelfCheckStatus
    data class Failed(val message: String) : SelfCheckStatus
}

@Composable
private fun SelfCheckRow(item: SelfCheckItem) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (val s = item.status) {
            is SelfCheckStatus.Running ->
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            is SelfCheckStatus.Ok ->
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            is SelfCheckStatus.Failed ->
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
            SelfCheckStatus.Pending ->
                Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.entry.model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(item.entry.model.id, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            when (val s = item.status) {
                is SelfCheckStatus.Running ->
                    Text(stringResource(R.string.model_self_check_running), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                is SelfCheckStatus.Pending ->
                    Text(stringResource(R.string.model_self_check_pending), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                is SelfCheckStatus.Ok -> {
                    val time = if (s.elapsedMs > 0) " (%.1fs)".format(s.elapsedMs / 1000.0) else ""
                    Text(stringResource(R.string.model_self_check_ok) + time, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                is SelfCheckStatus.Failed ->
                    Text(s.message, style = MaterialTheme.typography.bodySmall, color = scheme.error)
            }
        }
    }
}
