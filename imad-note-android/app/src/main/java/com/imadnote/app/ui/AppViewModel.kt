package com.imadnote.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.imadnote.app.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Creates a ViewModel scoped to the current navigation entry, with the container and nav arguments. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        factory = viewModelFactory {
            initializer { create(container, createSavedStateHandle()) }
        },
    )
}
