package com.lumalab.core

/** Linear undo/redo history of committed edit states. */
class EditHistory<T>(initial: T, private val limit: Int = 100) {
    private val states = ArrayList<T>().apply { add(initial) }
    private var index = 0

    val current: T get() = states[index]
    val canUndo: Boolean get() = index > 0
    val canRedo: Boolean get() = index < states.size - 1

    /** Records [state] unless it equals the current one; discards any redo branch. */
    fun push(state: T): Boolean {
        if (state == current) return false
        while (states.size > index + 1) states.removeAt(states.size - 1)
        states.add(state)
        if (states.size > limit) states.removeAt(0)
        index = states.size - 1
        return true
    }

    fun undo(): T {
        if (canUndo) index--
        return current
    }

    fun redo(): T {
        if (canRedo) index++
        return current
    }
}
