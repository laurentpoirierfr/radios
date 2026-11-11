package com.flamenco.radios.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flamenco.radios.R

/** Genre proposé dans le sélecteur (couleur ARGB issue de la configuration). */
data class GenreOption(val id: String, val name: String, val color: Int)

/**
 * Couleur d'accent lisible pour un genre : légèrement assombrie en thème clair,
 * éclaircie en thème sombre (contraste du texte sur le fond).
 */
@Composable
fun genreAccent(argb: Int?): Color {
    if (argb == null) return MaterialTheme.colorScheme.primary
    val base = Color(argb)
    return if (isSystemInDarkTheme()) lerp(base, Color.White, 0.45f) else lerp(base, Color.Black, 0.12f)
}

/**
 * Applique la couleur du genre au thème (titres, sélection, curseur, boutons…).
 * Sans genre ([argb] nul), le thème de l'app est conservé.
 */
@Composable
fun GenreTheme(argb: Int?, content: @Composable () -> Unit) {
    if (argb == null) return content()
    val accent = genreAccent(argb)
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = scheme.copy(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.18f) Color.Black else Color.White,
            primaryContainer = lerp(scheme.background, accent, if (dark) 0.30f else 0.16f),
            onPrimaryContainer = scheme.onBackground,
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

/** Rangée de puces : « Tous » puis chaque genre, dans sa couleur. */
@Composable
fun GenreSelector(
    genres: List<GenreOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        GenreChip(stringResource(R.string.all_genres), selected == null, null) { onSelect(null) }
        genres.forEach { genre ->
            GenreChip(genre.name, selected == genre.id, genre.color) { onSelect(genre.id) }
        }
    }
}

@Composable
private fun GenreChip(label: String, isSelected: Boolean, argb: Int?, onClick: () -> Unit) {
    val accent = if (argb == null) MaterialTheme.colorScheme.onSurface else genreAccent(argb)
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent,
            selectedLabelColor = if (accent.luminance() > 0.18f) Color.Black else Color.White,
            labelColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = accent.copy(alpha = 0.6f),
        ),
    )
}
