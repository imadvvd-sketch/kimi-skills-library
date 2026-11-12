package app.ghars.feature.tree

import app.ghars.feature.tree.Tone.Clay
import app.ghars.feature.tree.Tone.Fruit
import app.ghars.feature.tree.Tone.Highlight
import app.ghars.feature.tree.Tone.Leaf
import app.ghars.feature.tree.Tone.Mint
import app.ghars.feature.tree.Tone.Night
import app.ghars.feature.tree.Tone.Sand
import app.ghars.feature.tree.Tone.Withered
import app.ghars.feature.tree.TreeShape.Disc
import app.ghars.feature.tree.TreeShape.Lens
import app.ghars.feature.tree.TreeShape.Mound
import app.ghars.feature.tree.TreeShape.Stem

// Species sold in the store. Original artwork, drawn from scratch as geometry.
// Each is designed as a full tree; the sapling and shrub are the same plant, smaller and not yet in fruit.

private val seed = listOf(Mound(0.16f), Lens(0f, 0.01f, 0.07f, 0.035f, 90f, Mint))

private val sprout = listOf(
    Mound(0.18f),
    Stem(0f, 0f, 0f, 0.2f, 0.028f, Leaf),
    Lens(0f, 0.19f, 0.15f, 0.07f, 150f, Leaf),
    Lens(0f, 0.2f, 0.16f, 0.07f, 35f, Mint),
)

/** Marks fruit and flowers, which only appear on the grown tree. */
private class Ripe(val shape: TreeShape)

private fun younger(full: List<Any>, scale: Float): List<TreeShape> = full.mapNotNull { item ->
    when (val s = item as? TreeShape ?: return@mapNotNull null) {
        is Disc -> s.copy(x = s.x * scale, y = s.y * scale, r = s.r * scale)
        is Lens -> s.copy(x = s.x * scale, y = s.y * scale, length = s.length * scale, width = s.width * scale)
        is Stem -> s.copy(
            x0 = s.x0 * scale, y0 = s.y0 * scale, x1 = s.x1 * scale, y1 = s.y1 * scale,
            // Young trunks are thinner, but not so thin they vanish.
            width = s.width * (scale + 0.15f).coerceAtMost(1f), bend = s.bend * scale,
        )
        is Mound -> s.copy(halfWidth = s.halfWidth * (0.6f + 0.4f * scale))
    }
}

private fun species(full: List<Any>, withered: List<TreeShape>) = SpeciesArt(
    stages = listOf(
        seed,
        sprout,
        younger(full, 0.5f),
        younger(full, 0.75f),
        full.map { (it as? Ripe)?.shape ?: it as TreeShape },
    ),
    withered = withered,
)

/** Date palm: a tall bare trunk and a crown of arching fronds. */
val PalmArt = species(
    full = listOf(
        Mound(0.24f),
        Stem(0.02f, 0f, 0f, 0.62f, 0.07f, Clay, bend = 0.03f),
        Lens(0f, 0.62f, 0.34f, 0.06f, 205f, Leaf),
        Lens(0f, 0.62f, 0.34f, 0.06f, -25f, Leaf),
        Lens(0f, 0.63f, 0.34f, 0.065f, 165f, Leaf),
        Lens(0f, 0.63f, 0.34f, 0.065f, 15f, Leaf),
        Lens(0f, 0.64f, 0.3f, 0.065f, 130f, Leaf),
        Lens(0f, 0.64f, 0.3f, 0.065f, 50f, Leaf),
        Lens(0f, 0.64f, 0.26f, 0.06f, 92f, Mint),
        Ripe(Disc(-0.035f, 0.57f, 0.028f, Fruit)),
        Ripe(Disc(0.035f, 0.57f, 0.028f, Fruit)),
        Ripe(Disc(0f, 0.53f, 0.028f, Fruit)),
    ),
    withered = listOf(
        Mound(0.24f),
        Stem(0.02f, 0f, 0f, 0.62f, 0.065f, Withered, bend = 0.03f),
        Stem(0f, 0.62f, -0.2f, 0.36f, 0.025f, Withered, bend = -0.1f),
        Stem(0f, 0.62f, 0.2f, 0.36f, 0.025f, Withered, bend = 0.1f),
        Stem(0f, 0.62f, -0.1f, 0.3f, 0.025f, Withered, bend = -0.07f),
        Stem(0f, 0.62f, 0.1f, 0.3f, 0.025f, Withered, bend = 0.07f),
        Stem(0f, 0.62f, 0.01f, 0.72f, 0.025f, Withered),
    ),
)

/** Cedar: flat, level tiers that narrow towards the top. */
val CedarArt = species(
    full = listOf(
        Mound(0.24f),
        Stem(0f, 0f, 0f, 0.74f, 0.045f, Clay),
        Lens(0f, 0.19f, 0.37f, 0.14f, 177f, Leaf),
        Lens(0f, 0.19f, 0.37f, 0.14f, 3f, Leaf),
        Lens(0f, 0.32f, 0.31f, 0.13f, 176f, Leaf),
        Lens(0f, 0.32f, 0.31f, 0.13f, 4f, Leaf),
        Lens(0f, 0.45f, 0.25f, 0.12f, 175f, Leaf),
        Lens(0f, 0.45f, 0.25f, 0.12f, 5f, Leaf),
        Lens(0f, 0.57f, 0.19f, 0.11f, 174f, Leaf),
        Lens(0f, 0.57f, 0.19f, 0.11f, 6f, Leaf),
        Lens(0f, 0.68f, 0.13f, 0.1f, 172f, Leaf),
        Lens(0f, 0.68f, 0.13f, 0.1f, 8f, Leaf),
        Lens(0f, 0.72f, 0.15f, 0.07f, 90f, Mint),
        Lens(0.05f, 0.335f, 0.2f, 0.035f, 4f, Highlight),
        Lens(-0.05f, 0.465f, 0.15f, 0.03f, 175f, Highlight),
        Lens(0.04f, 0.585f, 0.11f, 0.028f, 6f, Highlight),
    ),
    withered = listOf(
        Mound(0.24f),
        Stem(0f, 0f, 0f, 0.76f, 0.045f, Withered),
        Stem(0f, 0.24f, -0.28f, 0.12f, 0.022f, Withered, bend = -0.02f),
        Stem(0f, 0.24f, 0.28f, 0.12f, 0.022f, Withered, bend = 0.02f),
        Stem(0f, 0.42f, -0.2f, 0.3f, 0.02f, Withered, bend = -0.02f),
        Stem(0f, 0.42f, 0.2f, 0.3f, 0.02f, Withered, bend = 0.02f),
        Stem(0f, 0.6f, -0.12f, 0.5f, 0.018f, Withered),
        Stem(0f, 0.6f, 0.12f, 0.5f, 0.018f, Withered),
    ),
)

/** Sidr: twin trunks under a wide, low umbrella. */
val SidrArt = species(
    full = listOf(
        Mound(0.26f),
        Stem(0f, 0f, -0.12f, 0.44f, 0.06f, Clay, bend = -0.02f),
        Stem(0f, 0f, 0.12f, 0.44f, 0.06f, Clay, bend = 0.02f),
        Disc(-0.3f, 0.52f, 0.13f, Leaf),
        Disc(0.3f, 0.52f, 0.13f, Leaf),
        Disc(-0.14f, 0.58f, 0.17f, Leaf),
        Disc(0.14f, 0.58f, 0.17f, Leaf),
        Disc(0f, 0.64f, 0.16f, Leaf),
        Disc(-0.16f, 0.66f, 0.07f, Highlight),
        Disc(0.12f, 0.7f, 0.05f, Highlight),
        Ripe(Disc(-0.28f, 0.5f, 0.02f, Clay)),
        Ripe(Disc(-0.05f, 0.56f, 0.02f, Clay)),
        Ripe(Disc(0.2f, 0.54f, 0.02f, Clay)),
        Ripe(Disc(0.31f, 0.56f, 0.02f, Clay)),
    ),
    withered = listOf(
        Mound(0.26f),
        Stem(0f, 0f, -0.12f, 0.44f, 0.055f, Withered, bend = -0.02f),
        Stem(0f, 0f, 0.12f, 0.44f, 0.055f, Withered, bend = 0.02f),
        Stem(-0.12f, 0.44f, -0.32f, 0.38f, 0.025f, Withered, bend = -0.02f),
        Stem(-0.12f, 0.44f, -0.06f, 0.6f, 0.025f, Withered),
        Stem(0.12f, 0.44f, 0.32f, 0.38f, 0.025f, Withered, bend = 0.02f),
        Stem(0.12f, 0.44f, 0.08f, 0.6f, 0.025f, Withered),
        Stem(-0.32f, 0.38f, -0.34f, 0.26f, 0.02f, Withered),
        Stem(0.32f, 0.38f, 0.34f, 0.26f, 0.02f, Withered),
    ),
)

/** Pomegranate: a many-stemmed shrub hung with round red fruit. */
val PomegranateArt = species(
    full = listOf(
        Mound(0.24f),
        Stem(0f, 0f, -0.16f, 0.4f, 0.035f, Clay, bend = -0.03f),
        Stem(0f, 0f, 0f, 0.46f, 0.035f, Clay),
        Stem(0f, 0f, 0.16f, 0.4f, 0.035f, Clay, bend = 0.03f),
        Disc(-0.2f, 0.48f, 0.14f, Leaf),
        Disc(0.2f, 0.48f, 0.14f, Leaf),
        Disc(0f, 0.58f, 0.19f, Leaf),
        Disc(-0.06f, 0.65f, 0.07f, Highlight),
        Ripe(Disc(-0.22f, 0.42f, 0.045f, Clay)),
        Ripe(Lens(-0.22f, 0.375f, 0.03f, 0.022f, -90f, Clay)),
        Ripe(Disc(0.06f, 0.5f, 0.045f, Clay)),
        Ripe(Lens(0.06f, 0.455f, 0.03f, 0.022f, -90f, Clay)),
        Ripe(Disc(0.24f, 0.46f, 0.045f, Clay)),
        Ripe(Lens(0.24f, 0.415f, 0.03f, 0.022f, -90f, Clay)),
        Ripe(Disc(-0.06f, 0.68f, 0.04f, Clay)),
    ),
    withered = listOf(
        Mound(0.24f),
        Stem(0f, 0f, -0.16f, 0.4f, 0.032f, Withered, bend = -0.03f),
        Stem(0f, 0f, 0f, 0.5f, 0.032f, Withered),
        Stem(0f, 0f, 0.16f, 0.4f, 0.032f, Withered, bend = 0.03f),
        Stem(-0.16f, 0.4f, -0.26f, 0.28f, 0.022f, Withered, bend = -0.03f),
        Stem(0.16f, 0.4f, 0.26f, 0.28f, 0.022f, Withered, bend = 0.03f),
        Stem(0f, 0.5f, 0.08f, 0.4f, 0.02f, Withered, bend = 0.03f),
    ),
)

/** Argan: a twisted, leaning trunk under a flat, spreading crown. */
val ArganArt = species(
    full = listOf(
        Mound(0.26f),
        Stem(-0.08f, 0f, 0.06f, 0.38f, 0.1f, Clay, bend = -0.09f),
        Stem(0.06f, 0.36f, -0.18f, 0.5f, 0.045f, Clay),
        Stem(0.06f, 0.36f, 0.24f, 0.5f, 0.045f, Clay),
        Disc(-0.3f, 0.54f, 0.11f, Leaf),
        Disc(-0.12f, 0.58f, 0.14f, Leaf),
        Disc(0.08f, 0.6f, 0.14f, Leaf),
        Disc(0.28f, 0.56f, 0.12f, Leaf),
        Disc(0.38f, 0.52f, 0.08f, Leaf),
        Disc(-0.14f, 0.63f, 0.06f, Highlight),
        Disc(0.2f, 0.62f, 0.045f, Highlight),
        Ripe(Disc(-0.28f, 0.5f, 0.022f, Fruit)),
        Ripe(Disc(-0.04f, 0.53f, 0.022f, Fruit)),
        Ripe(Disc(0.16f, 0.54f, 0.022f, Fruit)),
        Ripe(Disc(0.34f, 0.5f, 0.022f, Fruit)),
    ),
    withered = listOf(
        Mound(0.26f),
        Stem(-0.08f, 0f, 0.06f, 0.38f, 0.095f, Withered, bend = -0.09f),
        Stem(0.06f, 0.36f, -0.2f, 0.48f, 0.04f, Withered),
        Stem(-0.2f, 0.48f, -0.3f, 0.34f, 0.025f, Withered, bend = -0.03f),
        Stem(0.06f, 0.36f, 0.26f, 0.48f, 0.04f, Withered),
        Stem(0.26f, 0.48f, 0.36f, 0.34f, 0.025f, Withered, bend = 0.03f),
        Stem(0.06f, 0.38f, 0.04f, 0.54f, 0.03f, Withered),
    ),
)

/** Jasmine: a round bush covered in small pale flowers. */
val JasmineArt = species(
    full = listOf(
        Mound(0.26f),
        Stem(0f, 0f, 0f, 0.2f, 0.04f, Clay),
        Disc(-0.2f, 0.26f, 0.17f, Leaf),
        Disc(0.2f, 0.26f, 0.17f, Leaf),
        Disc(0f, 0.4f, 0.23f, Leaf),
        Disc(-0.08f, 0.5f, 0.08f, Highlight),
        Ripe(Disc(-0.24f, 0.3f, 0.034f, Sand)), Ripe(Disc(-0.24f, 0.3f, 0.013f, Fruit)),
        Ripe(Disc(-0.1f, 0.38f, 0.034f, Sand)), Ripe(Disc(-0.1f, 0.38f, 0.013f, Fruit)),
        Ripe(Disc(0.06f, 0.5f, 0.034f, Sand)), Ripe(Disc(0.06f, 0.5f, 0.013f, Fruit)),
        Ripe(Disc(0.12f, 0.32f, 0.034f, Sand)), Ripe(Disc(0.12f, 0.32f, 0.013f, Fruit)),
        Ripe(Disc(0.26f, 0.24f, 0.034f, Sand)), Ripe(Disc(0.26f, 0.24f, 0.013f, Fruit)),
        Ripe(Disc(-0.02f, 0.24f, 0.034f, Sand)), Ripe(Disc(-0.02f, 0.24f, 0.013f, Fruit)),
    ),
    withered = listOf(
        Mound(0.26f),
        Stem(0f, 0f, 0f, 0.22f, 0.036f, Withered),
        Stem(0f, 0.2f, -0.24f, 0.3f, 0.022f, Withered, bend = -0.02f),
        Stem(-0.24f, 0.3f, -0.3f, 0.14f, 0.018f, Withered, bend = -0.03f),
        Stem(0f, 0.2f, 0.24f, 0.3f, 0.022f, Withered, bend = 0.02f),
        Stem(0.24f, 0.3f, 0.3f, 0.14f, 0.018f, Withered, bend = 0.03f),
        Stem(0f, 0.22f, -0.04f, 0.46f, 0.022f, Withered),
        Stem(-0.04f, 0.46f, 0.06f, 0.36f, 0.018f, Withered, bend = 0.03f),
    ),
)

/** Fig: a short trunk carrying a few very large leaves and dark fruit. */
val FigArt = species(
    full = listOf(
        Mound(0.24f),
        Stem(0f, 0f, 0f, 0.3f, 0.07f, Clay),
        Stem(0f, 0.28f, -0.2f, 0.46f, 0.04f, Clay),
        Stem(0f, 0.28f, 0.2f, 0.46f, 0.04f, Clay),
        Stem(0f, 0.3f, 0f, 0.56f, 0.04f, Clay),
        Lens(-0.2f, 0.45f, 0.2f, 0.11f, 185f, Leaf),
        Lens(-0.2f, 0.45f, 0.22f, 0.12f, 130f, Leaf),
        Lens(0.2f, 0.45f, 0.2f, 0.11f, -5f, Leaf),
        Lens(0.2f, 0.45f, 0.22f, 0.12f, 50f, Leaf),
        Lens(0f, 0.55f, 0.22f, 0.12f, 140f, Leaf),
        Lens(0f, 0.55f, 0.22f, 0.12f, 40f, Leaf),
        Lens(0f, 0.55f, 0.24f, 0.12f, 90f, Mint),
        Lens(-0.2f, 0.46f, 0.2f, 0.11f, 85f, Leaf),
        Lens(0.2f, 0.46f, 0.2f, 0.11f, 95f, Leaf),
        Ripe(Lens(-0.3f, 0.52f, 0.07f, 0.055f, -90f, Night)),
        Ripe(Lens(0.3f, 0.54f, 0.07f, 0.055f, -90f, Night)),
        Ripe(Lens(-0.1f, 0.68f, 0.07f, 0.055f, -90f, Night)),
    ),
    withered = listOf(
        Mound(0.24f),
        Stem(0f, 0f, 0f, 0.3f, 0.065f, Withered),
        Stem(0f, 0.28f, -0.2f, 0.46f, 0.036f, Withered),
        Stem(-0.2f, 0.46f, -0.28f, 0.3f, 0.022f, Withered, bend = -0.03f),
        Stem(0f, 0.28f, 0.2f, 0.46f, 0.036f, Withered),
        Stem(0.2f, 0.46f, 0.28f, 0.3f, 0.022f, Withered, bend = 0.03f),
        Stem(0f, 0.3f, 0f, 0.56f, 0.036f, Withered),
        Lens(0.28f, 0.3f, 0.12f, 0.06f, -95f, Withered),
    ),
)
