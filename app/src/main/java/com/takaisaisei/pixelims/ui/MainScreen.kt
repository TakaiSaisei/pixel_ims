package com.takaisaisei.pixelims.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takaisaisei.pixelims.R
import com.takaisaisei.pixelims.domain.CfgValue
import com.takaisaisei.pixelims.domain.Feature
import com.takaisaisei.pixelims.domain.FeatureCategory
import com.takaisaisei.pixelims.domain.SlotConfig
import com.takaisaisei.pixelims.domain.SlotStatus
import com.takaisaisei.pixelims.telephony.WfcLabels

// Presentation strings for a Feature - the UI's only feature-specific knowledge.
// [shortRes] is only used by the status card, so cosmetic features (not shown there) may omit it.
// [choiceLabelRes] labels the row of a feature's user-selectable choice.
private data class ToggleText(
    val titleRes: Int,
    val descRes: Int,
    val shortRes: Int? = null,
    val choiceLabelRes: Int? = null,
)

private val TOGGLE_TEXT: Map<Feature, ToggleText> = mapOf(
    Feature.VOLTE to ToggleText(
        R.string.toggle_volte_title,
        R.string.toggle_volte_desc,
        R.string.feat_volte_short
    ),
    Feature.VONR to ToggleText(
        R.string.toggle_vonr_title,
        R.string.toggle_vonr_desc,
        R.string.feat_vonr_short
    ),
    Feature.VOWIFI to ToggleText(
        R.string.toggle_vowifi_title,
        R.string.toggle_vowifi_desc,
        R.string.feat_vowifi_short
    ),
    Feature.WFC_ROAMING to ToggleText(
        R.string.toggle_wfc_roaming_title,
        R.string.toggle_wfc_roaming_desc,
        R.string.feat_wfc_roaming_short
    ),
    Feature.SS_UT to ToggleText(
        R.string.toggle_ss_ut_title,
        R.string.toggle_ss_ut_desc,
        R.string.feat_ss_ut_short
    ),
    Feature.COSMETIC_DATA_RAT_ICON to ToggleText(
        R.string.toggle_cosmetic_data_rat_icon_title,
        R.string.toggle_cosmetic_data_rat_icon_desc,
    ),
    Feature.COSMETIC_LTE_PLUS_ICON to ToggleText(
        R.string.toggle_cosmetic_lte_plus_icon_title,
        R.string.toggle_cosmetic_lte_plus_icon_desc,
    ),
    Feature.COSMETIC_4G_FOR_LTE to ToggleText(
        R.string.toggle_cosmetic_4g_for_lte_title,
        R.string.toggle_cosmetic_4g_for_lte_desc,
    ),
    Feature.COSMETIC_4GLTE_FOR_LTE to ToggleText(
        R.string.toggle_cosmetic_4glte_for_lte_title,
        R.string.toggle_cosmetic_4glte_for_lte_desc,
    ),
    Feature.COSMETIC_VOWIFI_ICON to ToggleText(
        R.string.toggle_cosmetic_vowifi_icon_title,
        R.string.toggle_cosmetic_vowifi_icon_desc,
        choiceLabelRes = R.string.cosmetic_vowifi_label_format,
    ),
)

private val IMS_TOGGLES: List<Feature> =
    Feature.displayed.filter { it.category == FeatureCategory.IMS }
private val COSMETIC_TOGGLES: List<Feature> =
    Feature.displayed.filter { it.category == FeatureCategory.COSMETIC }

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onEnableWirelessDebugging: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = message.arg
                ?.let { context.getString(message.resId, it) }
                ?: context.getString(message.resId)
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            // Gate the entire control surface behind a paired/authorized connection: until then the
            // settings are inert, so we only show the setup steps. Once paired, the setup disappears.
            if (!state.isAuthorized) {
                item {
                    SetupCard(
                        state = state,
                        onEnableWirelessDebugging = onEnableWirelessDebugging,
                    )
                }
            } else {
                item { SimSelector(state = state, onSelect = viewModel::selectSlot) }
                item { StatusCard(status = state.status(state.selectedSlot)) }
                item {
                    SettingsCard(
                        slot = state.selectedSlot,
                        config = state.config(state.selectedSlot),
                        carrierName = state.carrierName(state.selectedSlot),
                        applyOnBoot = state.applyOnBoot(state.selectedSlot),
                        isApplying = state.isApplying,
                        onToggle = { flag, value ->
                            viewModel.setFlag(
                                state.selectedSlot,
                                flag,
                                value
                            )
                        },
                        onChoice = { key, value ->
                            viewModel.setValue(state.selectedSlot, key, CfgValue.IntVal(value))
                        },
                        onApplyOnBootChange = { viewModel.setApplyOnBoot(state.selectedSlot, it) },
                        onApply = viewModel::apply,
                        onRestore = viewModel::restore,
                    )
                }
            }
        }
    }
}

@Composable
private fun wfcLabelOptions(carrierName: String?): List<String> {
    val name = carrierName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.carrier_placeholder)
    return WfcLabels.templates().map { runCatching { it.format(name) }.getOrDefault(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimSelector(state: MainUiState, onSelect: (Int) -> Unit) {
    val tabs = state.slotTabs
    if (tabs.size <= 1) {
        Text(
            text = stringResource(R.string.sim_slot, (tabs.firstOrNull() ?: 0) + 1),
            style = MaterialTheme.typography.titleMedium,
        )
        return
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, slot ->
            SegmentedButton(
                selected = state.selectedSlot == slot,
                onClick = { onSelect(slot) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
            ) {
                Text(stringResource(R.string.sim_slot, slot + 1))
            }
        }
    }
}

@Composable
private fun StatusCard(status: SlotStatus) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.status_card_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.status_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            StatusRow(
                label = stringResource(R.string.status_ims_label),
                loaded = status.loaded,
                ok = status.imsRegistered,
            )
            IMS_TOGGLES.forEach { feature ->
                val shortRes = TOGGLE_TEXT.getValue(feature).shortRes ?: return@forEach
                StatusRow(
                    label = stringResource(shortRes),
                    loaded = status.loaded,
                    ok = status.effective[feature] == true,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, loaded: Boolean, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        when {
            !loaded -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )

            ok -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.status_on),
                tint = MaterialTheme.colorScheme.primary,
            )

            else -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.status_off),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    slot: Int,
    config: SlotConfig,
    carrierName: String?,
    applyOnBoot: Boolean,
    isApplying: Boolean,
    onToggle: (Feature, Boolean) -> Unit,
    onChoice: (String, Int) -> Unit,
    onApplyOnBootChange: (Boolean) -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ims_settings_title),
                style = MaterialTheme.typography.titleMedium,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            IMS_TOGGLES.forEach { feature ->
                val text = TOGGLE_TEXT.getValue(feature)
                ConfigToggle(
                    title = stringResource(text.titleRes),
                    description = stringResource(text.descRes),
                    checked = config[feature],
                    onCheckedChange = { onToggle(feature, it) },
                    enabled = !isApplying,
                )
            }

            if (COSMETIC_TOGGLES.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = stringResource(R.string.cosmetic_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                COSMETIC_TOGGLES.forEach { feature ->
                    val text = TOGGLE_TEXT.getValue(feature)
                    ConfigToggle(
                        title = stringResource(text.titleRes),
                        description = stringResource(text.descRes),
                        checked = config[feature],
                        onCheckedChange = { onToggle(feature, it) },
                        enabled = !isApplying,
                    )
                    val choice = feature.intChoice
                    if (choice != null && text.choiceLabelRes != null && config[feature]) {
                        ChoiceRow(
                            title = stringResource(text.choiceLabelRes),
                            options = wfcLabelOptions(carrierName),
                            selectedIndex = config.intValue(choice.key, choice.default),
                            onSelect = { onChoice(choice.key, it) },
                            enabled = !isApplying,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            ConfigToggle(
                title = stringResource(R.string.apply_on_boot_title),
                description = stringResource(R.string.apply_on_boot_desc),
                checked = applyOnBoot,
                onCheckedChange = onApplyOnBootChange,
                enabled = !isApplying,
            )

            Text(
                text = stringResource(R.string.apply_desc, slot + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onApply,
                    enabled = !isApplying,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_apply),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isApplying,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.action_restore),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    state: MainUiState,
    onEnableWirelessDebugging: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.setup_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            StepRow(
                title = stringResource(R.string.step1_title),
                description = stringResource(R.string.step1_desc),
                chipText = stringResource(
                    if (state.isWifiConnected) R.string.status_connected else R.string.status_disconnected,
                ),
                chipTone = if (state.isWifiConnected) StatusTone.POSITIVE else StatusTone.NEGATIVE,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            val (step2Desc, step2Chip, step2Tone) = if (state.hasPort) {
                Triple(R.string.step2_state_unpaired, R.string.badge_unpaired, StatusTone.WARNING)
            } else {
                Triple(R.string.step2_state_off, R.string.badge_not_connected, StatusTone.NEUTRAL)
            }
            StepRow(
                title = stringResource(R.string.step2_title),
                description = stringResource(step2Desc),
                chipText = stringResource(step2Chip),
                chipTone = step2Tone,
            )
            OutlinedButton(
                onClick = onEnableWirelessDebugging,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.enable_wireless_debugging),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.setup_pairing_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun StepRow(
    title: String,
    description: String,
    chipText: String,
    chipTone: StatusTone,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusChip(text = chipText, tone = chipTone)
    }
}
