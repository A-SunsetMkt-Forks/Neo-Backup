/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.manager.tasks

import android.content.Context
import android.content.DialogInterface
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.manager.handler.BackupRestoreHelper.ActionType
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logErrors
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.showNotification
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.utils.showActionResult
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

abstract class BaseActionTask(
    val app: Package, oAndBackupX: NeoActivity, val shellHandler: ShellHandler,
    val mode: Int, private val actionType: ActionType, val setInfoBar: (String) -> Unit,
) : CoroutinesAsyncTask<Void?, Void?, ActionResult>() {
    val neoActivityReference: WeakReference<NeoActivity> = WeakReference(oAndBackupX)
    private var signal: CountDownLatch? = null
    protected var result: ActionResult? = null
    protected var notificationId = -1

    override fun onProgressUpdate(vararg values: Void?) {
        val mainActivityX = neoActivityReference.get()
        if (mainActivityX != null && !mainActivityX.isFinishing) {
            val message = getProgressMessage(mainActivityX, actionType)
            mainActivityX.runOnUiThread {
                mainActivityX.showSnackBar("${app.packageLabel}: $message")
                setInfoBar("${app.packageLabel}: $message")
            }
            showNotification(
                mainActivityX, NeoActivity::class.java,
                notificationId, app.packageLabel, message, true
            )
        }
    }

    override fun onPostExecute(result: ActionResult?) {
        val mainActivityX = neoActivityReference.get()
        if (mainActivityX != null && !mainActivityX.isFinishing) {
            val message = getPostExecuteMessage(mainActivityX, actionType, result)
            showNotification(
                mainActivityX, NeoActivity::class.java,
                notificationId, app.packageLabel, message, true
            )
            mainActivityX.showActionResult(this.result!!) { _: DialogInterface?, _: Int ->
                logErrors(
                    LogsHandler.handleErrorMessages(mainActivityX, result?.message)
                        ?: ""
                )
            }
            if (result?.succeeded != true)
                NeoApp.lastErrorPackage = app.packageName
            mainActivityX.updatePackage(app.packageName)
            mainActivityX.dismissSnackBar()
            setInfoBar("")
        }
        if (signal != null) {
            signal!!.countDown()
        }
    }

    private fun getProgressMessage(context: Context, actionType: ActionType): String =
        context.getString(if (actionType == ActionType.BACKUP) R.string.backupProgress else R.string.restoreProgress)

    private fun getPostExecuteMessage(
        context: Context,
        actionType: ActionType,
        result: ActionResult?,
    ): String? {
        return result?.let {
            if (it.succeeded) {
                if (actionType == ActionType.BACKUP) {
                    context.getString(R.string.backupSuccess)
                } else {
                    context.getString(R.string.restoreSuccess)
                }
            } else {
                if (actionType == ActionType.BACKUP) {
                    context.getString(R.string.backupFailure)
                } else {
                    context.getString(R.string.restoreFailure)
                }
            }
        }
    }
}