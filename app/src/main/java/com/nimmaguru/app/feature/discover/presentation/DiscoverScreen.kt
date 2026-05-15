package com.nimmaguru.app.feature.discover.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.ui.components.GuruAvatar
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import java.util.Locale

private data class SkillChipOption(val key: String, val labelRes: Int)

private val SKILL_CHIP_OPTIONS = listOf(
    SkillChipOption("Math", R.string.skill_math),
    SkillChipOption("Science", R.string.skill_science),
    SkillChipOption("English", R.string.skill_english),
    SkillChipOption("Kannada", R.string.skill_kannada),
    SkillChipOption("Physics", R.string.skill_physics),
    SkillChipOption("History", R.string.skill_history),
    SkillChipOption("Music", R.string.skill_music),
    SkillChipOption("Art", R.string.skill_art),
    SkillChipOption("Chess", R.string.skill_chess),
    SkillChipOption("Yoga", R.string.skill_yoga),
)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
    onNavigateToGuruDetail: (String) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DiscoverEvent.NavigateToGuruDetail ->
                    onNavigateToGuruDetail(event.guruId)
            }
        }
    }

    when (val state = uiState) {
        is DiscoverUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DiscoverUiState.Success -> {
            DiscoverContent(
                modifier = modifier,
                gurus = state.gurus,
                query = state.query,
                selectedSkills = state.selectedSkills,
                isSearching = state.isSearching,
                onAction = viewModel::onAction,
                onNavigateToOnboarding = onNavigateToOnboarding,
            )
        }
        is DiscoverUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(state.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onAction(DiscoverAction.Retry) }) {
                        Text(stringResource(R.string.retry_button))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverContent(
    modifier: Modifier = Modifier,
    gurus: List<Guru> = emptyList(),
    query: String = "",
    selectedSkills: List<String> = emptyList(),
    isSearching: Boolean = false,
    onAction: (DiscoverAction) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { onAction(DiscoverAction.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search_icon),
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onAction(DiscoverAction.ClearQuery) }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SKILL_CHIP_OPTIONS.forEach { skill ->
                val isSelected = selectedSkills.contains(skill.key)
                FilterChip(
                    selected = isSelected,
                    onClick = { onAction(DiscoverAction.SkillToggled(skill.key)) },
                    label = {
                        Text(
                            text = stringResource(skill.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier.height(48.dp), // R-COMP-03
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.results_count, gurus.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (gurus.isEmpty() && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_gurus_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToOnboarding) {
                        Text(text = stringResource(R.string.discover_become_guru_cta))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(gurus, key = { it.id }) { guru ->
                    GuruCard(
                        guru = guru,
                        onClick = { onAction(DiscoverAction.GuruClicked(guru.id)) },
                    )
                }
            }
        }
    }
}

@Composable
fun GuruCard(
    guru: Guru,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GuruAvatar(
                name = guru.nameEn.ifBlank { stringResource(R.string.default_guru_name) },
                photoUrl = guru.photoUrl,
                size = 64.dp,
                contentDescription = stringResource(R.string.cd_guru_photo),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guru.nameEn.ifBlank { stringResource(R.string.default_guru_name) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = guru.village,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (guru.skills.isNotEmpty()) {
                    Text(
                        text = guru.skills.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", guru.avgRating),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = "${guru.totalSessions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_DiscoverScreen_WithResults() {
    NimmaGuruTheme {
        DiscoverContent(
            gurus = listOf(
                Guru(
                    id = "1", nameEn = "Ramesh Kumar", village = "Hunsur",
                    skills = listOf("Math", "Science"), avgRating = 4.5f, totalSessions = 12,
                ),
            ),
            selectedSkills = listOf("Math"),
        )
    }
}
