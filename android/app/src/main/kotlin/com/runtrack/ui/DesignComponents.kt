package com.runtrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runtrack.ui.theme.RunTrackColors

@Composable
internal fun RunTrackCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = RunTrackColors.Surface,
        border = BorderStroke(1.dp, RunTrackColors.Outline)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
internal fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, busy: Boolean = false) {
    Button(onClick, modifier.fillMaxWidth().height(52.dp), enabled = enabled && !busy,
        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        else Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun ScreenHeader(title: String, actionLabel: String, enabled: Boolean = true, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        when (actionLabel) {
            "닫기 ×" -> IconButton(onClick = action, enabled = enabled, modifier = Modifier.testTag("headerAction")) {
                Icon(Icons.Outlined.Close, "닫기", Modifier.size(22.dp))
            }
            "새로고침" -> IconButton(onClick = action, enabled = enabled, modifier = Modifier.testTag("headerAction")) {
                Icon(Icons.Outlined.Refresh, "새로고침", Modifier.size(22.dp))
            }
            else -> TextButton(onClick = action, enabled = enabled, modifier = Modifier
                .background(RunTrackColors.SoftAccent, RoundedCornerShape(12.dp)).testTag("headerAction"),
                contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("추가", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun SectionHeading(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
    }
}

@Composable
internal fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title)
        RunTrackCard(content = content)
    }
}

@Composable
internal fun FieldLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge)
}

@Composable
internal fun Badge(text: String, warning: Boolean = false) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = if (warning) RunTrackColors.Warning else RunTrackColors.Accent,
        modifier = Modifier.background(if (warning) RunTrackColors.WarningSurface else RunTrackColors.SoftAccent,
            RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
}

@Composable
internal fun InformationNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(RunTrackColors.SoftAccent, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = RunTrackColors.Accent)
        Text(text, style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Accent)
    }
}

@Composable
internal fun EmptyStateCard(title: String, detail: String, actionLabel: String, icon: ImageVector = Icons.Outlined.DirectionsRun, action: () -> Unit) {
    RunTrackCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(56.dp).background(RunTrackColors.SoftAccent, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(26.dp), tint = RunTrackColors.Accent)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
            OutlinedButton(onClick = action, shape = RoundedCornerShape(12.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun MetricCaption(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.size(15.dp), tint = RunTrackColors.Muted)
            Text(label, style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
