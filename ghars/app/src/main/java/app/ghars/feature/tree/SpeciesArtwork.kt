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

// Original artwork for Ghars, drawn from scratch as geometry. Not derived from any other app.

/** Stage 1 for every species: soil with the first green tip showing. */
private val seed = listOf(
    Mound(0.16f),
    Lens(0f, 0.01f, 0.07f, 0.035f, 90f, Mint),
)

/** Stage 2 for leafy species: a stem with two leaves. */
private val sprout = listOf(
    Mound(0.18f),
    Stem(0f, 0f, 0f, 0.2f, 0.028f, Leaf),
    Lens(0f, 0.19f, 0.15f, 0.07f, 150f, Leaf),
    Lens(0f, 0.2f, 0.16f, 0.07f, 35f, Mint),
)

val OliveArt = SpeciesArt(
    stages = listOf(
        seed,
        sprout,
        // Sapling: thin trunk, narrow paired leaves.
        listOf(
            Mound(0.2f),
            Stem(0f, 0f, 0f, 0.4f, 0.035f, Clay),
            Lens(0f, 0.16f, 0.13f, 0.045f, 155f, Leaf),
            Lens(0f, 0.2f, 0.13f, 0.045f, 25f, Leaf),
            Lens(0f, 0.27f, 0.13f, 0.045f, 150f, Leaf),
            Lens(0f, 0.31f, 0.13f, 0.045f, 30f, Leaf),
            Lens(0f, 0.38f, 0.12f, 0.045f, 125f, Leaf),
            Lens(0f, 0.39f, 0.13f, 0.05f, 60f, Mint),
        ),
        // Shrub: the trunk forks and the crown begins.
        listOf(
            Mound(0.22f),
            Stem(0f, 0f, 0.01f, 0.4f, 0.055f, Clay, bend = 0.02f),
            Stem(0.01f, 0.3f, -0.14f, 0.48f, 0.032f, Clay),
            Stem(0.01f, 0.34f, 0.15f, 0.5f, 0.032f, Clay),
            Disc(-0.15f, 0.54f, 0.13f, Leaf),
            Disc(0.15f, 0.56f, 0.13f, Leaf),
            Disc(0f, 0.64f, 0.15f, Leaf),
            Disc(-0.05f, 0.69f, 0.065f, Highlight),
        ),
        // Tree: wide, low crown on a stout leaning trunk, with a few dark olives.
        listOf(
            Mound(0.26f),
            Stem(0.02f, 0f, -0.01f, 0.4f, 0.095f, Clay, bend = 0.035f),
            Stem(0f, 0.32f, -0.2f, 0.54f, 0.045f, Clay),
            Stem(0f, 0.34f, 0.2f, 0.56f, 0.045f, Clay),
            Disc(-0.26f, 0.6f, 0.15f, Leaf),
            Disc(0.26f, 0.62f, 0.15f, Leaf),
            Disc(0f, 0.6f, 0.18f, Leaf),
            Disc(-0.1f, 0.74f, 0.17f, Leaf),
            Disc(0.11f, 0.75f, 0.16f, Leaf),
            Disc(-0.14f, 0.8f, 0.075f, Highlight),
            Disc(0.18f, 0.7f, 0.05f, Highlight),
            Disc(-0.24f, 0.56f, 0.02f, Night),
            Disc(-0.02f, 0.63f, 0.02f, Night),
            Disc(0.2f, 0.58f, 0.02f, Night),
            Disc(0.06f, 0.78f, 0.02f, Night),
        ),
    ),
    withered = listOf(
        Mound(0.26f),
        Stem(0.02f, 0f, -0.01f, 0.4f, 0.09f, Withered, bend = 0.035f),
        Stem(0f, 0.34f, -0.22f, 0.5f, 0.04f, Withered),
        Stem(-0.22f, 0.5f, -0.3f, 0.32f, 0.03f, Withered, bend = -0.04f),
        Stem(0f, 0.36f, 0.2f, 0.5f, 0.04f, Withered),
        Stem(0.2f, 0.5f, 0.3f, 0.34f, 0.03f, Withered, bend = 0.04f),
        Stem(-0.01f, 0.4f, 0.03f, 0.58f, 0.035f, Withered),
        Stem(0.03f, 0.58f, 0.12f, 0.48f, 0.025f, Withered, bend = 0.03f),
        Lens(-0.24f, 0.015f, 0.1f, 0.035f, 8f, Withered),
    ),
)

val LemonArt = SpeciesArt(
    stages = listOf(
        seed,
        sprout,
        // Sapling: one small round crown.
        listOf(
            Mound(0.2f),
            Stem(0f, 0f, 0f, 0.36f, 0.032f, Clay),
            Disc(0f, 0.44f, 0.125f, Leaf),
            Disc(-0.035f, 0.475f, 0.05f, Highlight),
        ),
        // Shrub: fuller crown and the first lemon.
        listOf(
            Mound(0.22f),
            Stem(0f, 0f, 0f, 0.4f, 0.045f, Clay),
            Disc(-0.12f, 0.47f, 0.11f, Leaf),
            Disc(0.12f, 0.47f, 0.11f, Leaf),
            Disc(0f, 0.56f, 0.19f, Leaf),
            Disc(-0.06f, 0.64f, 0.075f, Highlight),
            Lens(0.07f, 0.52f, 0.085f, 0.05f, -70f, Fruit),
        ),
        // Tree: a tall round crown full of fruit.
        listOf(
            Mound(0.24f),
            Stem(0f, 0f, 0f, 0.44f, 0.065f, Clay),
            Disc(-0.21f, 0.54f, 0.13f, Leaf),
            Disc(0.21f, 0.54f, 0.13f, Leaf),
            Disc(0f, 0.65f, 0.265f, Leaf),
            Disc(-0.09f, 0.76f, 0.1f, Highlight),
            Lens(-0.17f, 0.62f, 0.09f, 0.055f, -75f, Fruit),
            Lens(0.12f, 0.74f, 0.09f, 0.055f, -70f, Fruit),
            Lens(0.19f, 0.56f, 0.09f, 0.055f, -80f, Fruit),
            Lens(-0.01f, 0.53f, 0.09f, 0.055f, -72f, Fruit),
        ),
    ),
    withered = listOf(
        Mound(0.24f),
        Stem(0f, 0f, 0f, 0.44f, 0.06f, Withered),
        Stem(0f, 0.4f, -0.16f, 0.6f, 0.035f, Withered),
        Stem(-0.16f, 0.6f, -0.26f, 0.44f, 0.025f, Withered, bend = -0.04f),
        Stem(0f, 0.42f, 0.17f, 0.6f, 0.035f, Withered),
        Stem(0.17f, 0.6f, 0.26f, 0.46f, 0.025f, Withered, bend = 0.04f),
        Stem(0f, 0.44f, 0f, 0.64f, 0.03f, Withered),
        Stem(0f, 0.64f, -0.07f, 0.54f, 0.022f, Withered, bend = -0.03f),
        Lens(0.26f, 0.46f, 0.08f, 0.045f, -85f, Withered),
    ),
)

val CactusArt = SpeciesArt(
    stages = listOf(
        seed,
        // Sprout: a small green nub.
        listOf(
            Mound(0.18f),
            Stem(0f, 0.02f, 0f, 0.13f, 0.11f, Leaf),
            Stem(-0.02f, 0.04f, -0.02f, 0.12f, 0.02f, Highlight),
        ),
        // Sapling: a single column.
        listOf(
            Mound(0.2f),
            Stem(0f, 0.03f, 0f, 0.32f, 0.13f, Leaf),
            Stem(-0.025f, 0.06f, -0.025f, 0.3f, 0.022f, Highlight),
        ),
        // Shrub: the first arm.
        listOf(
            Mound(0.22f),
            Stem(0f, 0.04f, 0f, 0.46f, 0.15f, Leaf),
            Stem(0.06f, 0.22f, 0.2f, 0.22f, 0.08f, Leaf),
            Stem(0.2f, 0.22f, 0.2f, 0.36f, 0.08f, Leaf),
            Stem(-0.03f, 0.08f, -0.03f, 0.44f, 0.024f, Highlight),
        ),
        // Tree: two arms and a flower on top.
        listOf(
            Mound(0.26f),
            Stem(0f, 0.05f, 0f, 0.64f, 0.17f, Leaf),
            Stem(0.07f, 0.3f, 0.24f, 0.3f, 0.09f, Leaf),
            Stem(0.24f, 0.3f, 0.24f, 0.5f, 0.09f, Leaf),
            Stem(-0.07f, 0.2f, -0.24f, 0.2f, 0.09f, Leaf),
            Stem(-0.24f, 0.2f, -0.24f, 0.36f, 0.09f, Leaf),
            Stem(-0.035f, 0.09f, -0.035f, 0.62f, 0.026f, Highlight),
            Stem(0.225f, 0.33f, 0.225f, 0.48f, 0.018f, Highlight),
            Stem(-0.255f, 0.23f, -0.255f, 0.34f, 0.018f, Highlight),
            Disc(0f, 0.75f, 0.05f, Clay),
            Disc(0f, 0.75f, 0.02f, Sand),
            Disc(0.24f, 0.56f, 0.03f, Clay),
        ),
    ),
    withered = listOf(
        Mound(0.26f),
        Stem(0f, 0.05f, 0.07f, 0.52f, 0.15f, Withered, bend = -0.05f),
        Stem(0.09f, 0.3f, 0.24f, 0.3f, 0.075f, Withered),
        Stem(0.24f, 0.3f, 0.26f, 0.15f, 0.075f, Withered),
        Stem(-0.05f, 0.22f, -0.2f, 0.22f, 0.075f, Withered),
        Stem(-0.2f, 0.22f, -0.22f, 0.09f, 0.075f, Withered),
    ),
)
