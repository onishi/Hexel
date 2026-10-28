package com.hexel.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexel.core.gesture.GestureClassifier
import com.hexel.launcher.HexelViewModel
import com.hexel.launcher.ui.editor.AppPicker
import com.hexel.launcher.ui.editor.AssignmentEditor
import com.hexel.launcher.ui.home.HexMap

/** Home map, with the swipe editor and app picker shown on top when open. */
@Composable
fun HexelRoot(viewModel: HexelViewModel, classifier: GestureClassifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val icons by viewModel.icons.collectAsStateWithLifecycle()
    val editing = viewModel.editingPackage
    val picking = viewModel.pickingDirection

    // The map stays composed underneath so its scroll position and text layouts are kept.
    HexMap(
        state = state,
        icons = icons,
        camera = viewModel.camera,
        classifier = classifier,
        onLaunch = viewModel::launchApp,
        onLongPress = viewModel::openEditor,
    )

    if (editing != null) {
        AssignmentEditor(
            source = editing,
            state = state,
            icons = icons,
            onPickDirection = viewModel::openPicker,
            onClose = viewModel::closeEditor,
        )
        if (picking != null) {
            AppPicker(
                source = editing,
                direction = picking,
                state = state,
                icons = icons,
                onSelect = { viewModel.assign(picking, it) },
                onClose = viewModel::closePicker,
            )
        }
    }

    // A launcher must never be finished by Back; inside the editor Back goes up one level.
    BackHandler {
        when {
            picking != null -> viewModel.closePicker()
            editing != null -> viewModel.closeEditor()
        }
    }
}
