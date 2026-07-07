package com.smithware.tidypilot

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.smithware.tidypilot.data.TidyPilotDatabase
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class TidyPilotSummaryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uri.authority != AUTHORITY || uri.lastPathSegment != PATH_SUMMARY) return null
        val appContext = context?.applicationContext ?: return emptyCursor()
        val dao = TidyPilotDatabase.get(appContext).dao()
        val today = LocalDate.now()
        val summary = runBlocking {
            val active = dao.activeTaskCount()
            val due = dao.dueTaskCount(today)
            val skipped = dao.skippedTaskCount()
            val dueNames = dao.dueTaskNames(today)
            val roomAverage = dao.averageRoomScore()?.toInt() ?: 0
            val status = when {
                due == 0 -> "No chores due right now"
                due <= 3 -> "Small reset plan ready"
                else -> "Chore queue needs attention"
            }
            val alert = when {
                skipped > 1 -> "$skipped chores have been skipped before. Keep the next step small."
                due > 5 -> "$due chores are due. Build a minimum reset first."
                else -> null
            }
            val keyInfo = "$due due chores, $active active chores, room average $roomAverage/100"
            val counts = listOf("$due due", "$active active", "$skipped skipped").joinToString("|")
            val dueSoon = dueNames.ifEmpty { listOf("No chores due") }.joinToString("|")
            Summary(status, keyInfo, alert, counts, dueSoon)
        }
        return MatrixCursor(COLUMNS).apply {
            addRow(
                arrayOf(
                    APP_ID,
                    summary.status,
                    summary.keyInfo,
                    summary.alert.orEmpty(),
                    summary.counts,
                    summary.dueSoon,
                    "just now",
                    "TidyPilot chores provider"
                )
            )
        }
    }

    override fun getType(uri: Uri): String? = if (uri.authority == AUTHORITY && uri.lastPathSegment == PATH_SUMMARY) {
        "vnd.android.cursor.item/vnd.smithware.tidypilot.summary"
    } else {
        null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun emptyCursor(): Cursor = MatrixCursor(COLUMNS)

    private data class Summary(
        val status: String,
        val keyInfo: String,
        val alert: String?,
        val counts: String,
        val dueSoon: String
    )

    companion object {
        private const val AUTHORITY = "com.smithware.tidypilot.summary"
        private const val PATH_SUMMARY = "summary"
        private const val APP_ID = "tidypilot"
        private val COLUMNS = arrayOf(
            "app_id",
            "status",
            "key_info",
            "alert",
            "counts",
            "due_soon",
            "last_updated",
            "source"
        )
    }
}
