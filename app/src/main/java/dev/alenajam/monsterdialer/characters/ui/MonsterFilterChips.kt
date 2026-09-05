package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MonsterFilterChips(
    selectedFilter: MonsterFilter,
    onFilterSelected: (MonsterFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MonsterFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            MonsterFilter.All -> stringResource(R.string.filter_all)
                            MonsterFilter.Regular -> stringResource(R.string.filter_regular)
                            MonsterFilter.RadiantUnlocked -> stringResource(R.string.filter_unlocked_radiant)
                            MonsterFilter.RadiantLocked -> stringResource(R.string.filter_locked_radiant)
                        }
                    )
                }
            )
        }
    }
}
