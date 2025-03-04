package com.machiav3lli.backup.ui.pages

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.BACKUP_DIRECTORY_INTENT
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PREFS_LANGUAGES_SYSTEM
import com.machiav3lli.backup.R
import com.machiav3lli.backup.THEME
import com.machiav3lli.backup.accentColorItems
import com.machiav3lli.backup.data.entity.BooleanPref
import com.machiav3lli.backup.data.entity.EnumPref
import com.machiav3lli.backup.data.entity.IntPref
import com.machiav3lli.backup.data.entity.ListPref
import com.machiav3lli.backup.data.entity.Pref
import com.machiav3lli.backup.data.entity.StringEditPref
import com.machiav3lli.backup.data.entity.StringPref
import com.machiav3lli.backup.secondaryColorItems
import com.machiav3lli.backup.themeItems
import com.machiav3lli.backup.ui.compose.component.InnerBackground
import com.machiav3lli.backup.ui.compose.component.PrefsGroup
import com.machiav3lli.backup.ui.compose.component.StringEditPreference
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowsOutLineVertical
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarX
import com.machiav3lli.backup.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.backup.ui.compose.icons.phosphor.Clock
import com.machiav3lli.backup.ui.compose.icons.phosphor.EyedropperSample
import com.machiav3lli.backup.ui.compose.icons.phosphor.FingerprintSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.FolderNotch
import com.machiav3lli.backup.ui.compose.icons.phosphor.List
import com.machiav3lli.backup.ui.compose.icons.phosphor.Lock
import com.machiav3lli.backup.ui.compose.icons.phosphor.Spinner
import com.machiav3lli.backup.ui.compose.icons.phosphor.Swatches
import com.machiav3lli.backup.ui.compose.icons.phosphor.TagSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.TextAa
import com.machiav3lli.backup.ui.compose.icons.phosphor.Translate
import com.machiav3lli.backup.ui.dialogs.BaseDialog
import com.machiav3lli.backup.ui.dialogs.EnumPrefDialogUI
import com.machiav3lli.backup.ui.dialogs.ListPrefDialogUI
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.backupDirConfigured
import com.machiav3lli.backup.utils.backupFolderExists
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.getLanguageList
import com.machiav3lli.backup.utils.isBiometricLockAvailable
import com.machiav3lli.backup.utils.isDeviceLockAvailable
import com.machiav3lli.backup.utils.isDeviceLockEnabled
import com.machiav3lli.backup.utils.recreateActivities
import com.machiav3lli.backup.utils.restartApp
import com.machiav3lli.backup.utils.setBackupDir
import com.machiav3lli.backup.utils.setCustomTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

@Composable
fun UserPrefsPage() {
    val context = LocalContext.current
    val openDialog = remember { mutableStateOf(false) }
    var dialogsPref by remember { mutableStateOf<Pref?>(null) }
    var backupDir by remember { mutableStateOf(backupDirConfigured) }   //TODO hg42 remember ???

    val prefs = Pref.prefGroups["user"]?.toPersistentList() ?: persistentListOf()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    val uri = it.data ?: return@let
                    val oldDir = try {
                        backupDirConfigured
                    } catch (e: StorageLocationNotConfiguredException) {
                        "" // Can be ignored, this is about to set the path
                    }
                    if (oldDir != uri.toString()) {
                        val flags = it.flags and (
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                        Timber.i("setting uri $uri")
                        backupDir = setBackupDir(uri)
                    }
                }
            }
        }

    InnerBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PrefsGroup(prefs = prefs) { pref ->
                    dialogsPref = pref
                    openDialog.value = true
                }
            }
        }
    }

    if (openDialog.value) {
        BaseDialog(onDismiss = { openDialog.value = false }) {
            when (dialogsPref) {
                //pref_languages,
                is ListPref -> ListPrefDialogUI(
                    pref = dialogsPref as ListPref,
                    openDialogCustom = openDialog,
                )

                //pref_appTheme,
                //pref_appAccentColor,
                //pref_appSecondaryColor,
                is EnumPref -> EnumPrefDialogUI(
                    pref = dialogsPref as EnumPref,
                    openDialogCustom = openDialog,
                )
            }
        }
    }
}

fun onThemeChanged(pref: Pref) {
    NeoApp.context.setCustomTheme()
    recreateActivities()
}

val pref_languages = ListPref(
    key = "user.languages",
    titleId = R.string.prefs_languages,
    icon = Phosphor.Translate,
    entries = NeoApp.context.getLanguageList(),
    defaultValue = PREFS_LANGUAGES_SYSTEM,
    onChanged = {
        val pref = it as ListPref
        // does not work as expected, because restartApp doesn't really restart the whole app
        //if (pref.value == PREFS_LANGUAGES_SYSTEM)
        if (pref_restartAppOnLanguageChange.value)
            NeoApp.context.restartApp()   // does not really restart the app, only recreates
        else
            recreateActivities()
    },
)

val pref_appTheme = EnumPref(
    key = "user.appThemeNeo",
    titleId = R.string.prefs_theme,
    icon = Phosphor.Swatches,
    entries = themeItems,
    defaultValue = if (Android.minSDK(31)) THEME.DYNAMIC.ordinal
    else THEME.SYSTEM.ordinal,
    onChanged = ::onThemeChanged,
)

val pref_appAccentColor = EnumPref(
    key = ".appAccentColor", //TODO restore in future
    titleId = R.string.prefs_accent_color,
    icon = Phosphor.EyedropperSample,
    //iconTint = { MaterialTheme.colorScheme.primary },
    entries = accentColorItems,
    defaultValue = with(SystemUtils.packageName) {
        when {
            contains("hg42")  -> 8
            contains("debug") -> 4
            else              -> 0
        }
    },
    onChanged = ::onThemeChanged,
)

val pref_appSecondaryColor = EnumPref(
    key = ".appSecondaryColor", //TODO restore in future
    titleId = R.string.prefs_secondary_color,
    icon = Phosphor.EyedropperSample,
    //iconTint = { MaterialTheme.colorScheme.secondary },
    entries = secondaryColorItems,
    defaultValue = with(SystemUtils.packageName) {
        when {
            contains(".rel")  -> 0
            contains("debug") -> 4
            else              -> 3
        }
    },
    onChanged = ::onThemeChanged,
)

val pref_pathBackupFolder = StringEditPref(
    key = "user.pathBackupFolder",
    titleId = R.string.prefs_pathbackupfolder,
    icon = Phosphor.FolderNotch,
    iconTint = {
        val pref = it as StringEditPref
        val alpha = if (pref.value == runCatching { backupDirConfigured }.getOrNull()) 1f else 0.3f
        if (pref.value.isEmpty()) Color.Gray
        else if (backupFolderExists(pref.value)) Color.Green.copy(alpha = alpha)
        else Color.Red.copy(alpha = alpha)
    },
    UI = { it, _, index, groupSize ->
        val context = LocalContext.current
        val pref = it as StringEditPref
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                    result.data?.let {
                        val uri = it.data ?: return@let
                        val oldDir = try {
                            backupDirConfigured
                        } catch (e: StorageLocationNotConfiguredException) {
                            "" // Can be ignored, this is about to set the path
                        }
                        if (oldDir != uri.toString()) {
                            val flags = it.flags and (
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                            //TODO hg42 check and remember if flags are read only and implement appropriate actions elsewhere
                            context.contentResolver.takePersistableUriPermission(uri, flags)
                            Timber.i("setting uri $uri")
                            setBackupDir(uri)
                        }
                    }
                }
            }
        val onClick = {
            launcher.launch(BACKUP_DIRECTORY_INTENT)
        }
        StringEditPreference(
            pref = pref,
            index = index,
            groupSize = groupSize,
            onClick = onClick,
        )
    },
    defaultValue = "",
) {
    val pref = it as StringPref
    if (pref.value != "") {
        setBackupDir(Uri.parse(pref.value))
    }
}

val pref_deviceLock = BooleanPref(
    key = "user.deviceLock",
    titleId = R.string.prefs_devicelock,
    summaryId = R.string.prefs_devicelock_summary,
    icon = Phosphor.Lock,
    defaultValue = false,
    enableIf = { NeoApp.context.isDeviceLockAvailable() }
)

val pref_biometricLock = BooleanPref(
    key = "user.biometricLock",
    titleId = R.string.prefs_biometriclock,
    summaryId = R.string.prefs_biometriclock_summary,
    icon = Phosphor.FingerprintSimple,
    defaultValue = false,
    enableIf = { NeoApp.context.isBiometricLockAvailable() && isDeviceLockEnabled() }
)

val pref_multilineInfoChips = BooleanPref(
    key = "user.multilineInfoChips",
    titleId = R.string.prefs_multilineinfochips,
    summaryId = R.string.prefs_multilineinfochips_summary,
    icon = Phosphor.ArrowsOutLineVertical,
    defaultValue = false
)

val pref_singularBackupRestore = BooleanPref(
    key = "user.singularBackupRestore",
    titleId = R.string.prefs_singularbackuprestore,
    summaryId = R.string.prefs_singularbackuprestore_summary,
    icon = Phosphor.List,
    defaultValue = true
)

val pref_newAndUpdatedNotification = BooleanPref(
    key = "user.newAndUppdatedNotification",
    titleId = R.string.prefs_newandupdatednotification,
    summaryId = R.string.prefs_newandupdatednotification_summary,
    icon = Phosphor.CircleWavyWarning,
    defaultValue = false
)

val pref_squeezeNavText = BooleanPref(
    key = "user.squeezeNavText",
    titleId = R.string.prefs_squeezenavtext,
    summaryId = R.string.prefs_squeezenavtext_summary,
    icon = Phosphor.TextAa,
    defaultValue = false
)

val pref_altNavBarItem = BooleanPref(
    key = "user.altNavBarItem",
    titleId = R.string.prefs_altnavbaritem,
    summaryId = R.string.prefs_altnavbaritem_summary,
    icon = Phosphor.TagSimple,
    defaultValue = false
)

val pref_altBackupDate = BooleanPref(
    key = "user.altBackupDate",
    titleId = R.string.prefs_altbackupdate,
    summaryId = R.string.prefs_altbackupdate_summary,
    icon = Phosphor.CalendarX,
    defaultValue = false
)

val pref_altBlockLayout = BooleanPref(
    key = "user.altBlockLayout",
    titleId = R.string.prefs_altblocklayout,
    summaryId = R.string.prefs_altblocklayout_summary,
    icon = Phosphor.Swatches,
    defaultValue = false
)

val pref_busyLaserBackground = BooleanPref(
    key = "user.busyLaserBackground",
    titleId = R.string.prefs_laserbackground,
    summaryId = R.string.prefs_laserbackground_summary,
    icon = Phosphor.Spinner,
    defaultValue = true
)

val pref_oldBackups = IntPref(
    key = "user.oldBackups",
    titleId = R.string.prefs_oldbackups,
    summaryId = R.string.prefs_oldbackups_summary,
    icon = Phosphor.Clock,
    entries = (1..30).toList(),
    defaultValue = 2
)
