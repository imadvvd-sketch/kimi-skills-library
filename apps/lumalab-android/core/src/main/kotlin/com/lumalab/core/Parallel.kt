package com.lumalab.core

import java.util.stream.IntStream

internal inline fun parallelFor(count: Int, crossinline body: (Int) -> Unit) {
    if (count < 64) {
        for (i in 0 until count) body(i)
    } else {
        IntStream.range(0, count).parallel().forEach { body(it) }
    }
}
