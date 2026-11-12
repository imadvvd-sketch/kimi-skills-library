package app.ghars.feature.tree

import androidx.annotation.StringRes
import app.ghars.R

class SpeciesInfo(
    val id: String,
    @param:StringRes val name: Int,
    /** "Your olive tree has grown." Arabic needs a different verb form per species. */
    @param:StringRes val grownMessage: Int,
    val art: SpeciesArt,
)

/** What the app knows how to draw and name. The database decides which of these the user owns. */
object SpeciesCatalog {
    private val all = listOf(
        SpeciesInfo("olive", R.string.species_olive, R.string.grown_olive, OliveArt),
        SpeciesInfo("lemon", R.string.species_lemon, R.string.grown_lemon, LemonArt),
        SpeciesInfo("cactus", R.string.species_cactus, R.string.grown_cactus, CactusArt),
        SpeciesInfo("palm", R.string.species_palm, R.string.grown_palm, PalmArt),
        SpeciesInfo("jasmine", R.string.species_jasmine, R.string.grown_jasmine, JasmineArt),
        SpeciesInfo("pomegranate", R.string.species_pomegranate, R.string.grown_pomegranate, PomegranateArt),
        SpeciesInfo("fig", R.string.species_fig, R.string.grown_fig, FigArt),
        SpeciesInfo("sidr", R.string.species_sidr, R.string.grown_sidr, SidrArt),
        SpeciesInfo("cedar", R.string.species_cedar, R.string.grown_cedar, CedarArt),
        SpeciesInfo("argan", R.string.species_argan, R.string.grown_argan, ArganArt),
    ).associateBy { it.id }

    val ids: Set<String> get() = all.keys
    fun find(id: String): SpeciesInfo? = all[id]
    fun get(id: String): SpeciesInfo = all[id] ?: all.getValue("olive")
}
