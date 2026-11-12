package app.ghars.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.GharsButton
import app.ghars.domain.session.SessionRules
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView

/** S02: pick a length, see your tree, plant. One tap on the happy path. */
@Composable
fun HomeScreen(onChooseSpecies: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    val colors = GharsTheme.colors
    val context = LocalContext.current
    val species = SpeciesCatalog.get(ui.speciesId)

    // The session works without notifications, so plant whatever the answer is.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.plant()
    }
    val onPlant = {
        val needsAsking = Build.VERSION.SDK_INT >= 33 && !ui.askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsAsking) {
            viewModel.markAskedForNotifications()
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.plant()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(spacing.space6))
        CircularDurationSlider(
            minutes = ui.minutes,
            onMinutesChange = viewModel::setMinutes,
            modifier = Modifier.widthIn(max = GharsTheme.sizes.ring).fillMaxWidth(),
        ) {
            TreeView(
                speciesId = ui.speciesId,
                stage = SessionRules.STAGES,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = stringResource(R.string.species_picker_title),
                        role = Role.Button,
                        onClick = onChooseSpecies,
                    ),
                description = stringResource(R.string.cd_choose_species, stringResource(species.name)),
            )
        }
        Spacer(Modifier.height(spacing.space6))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.stepMinutes(-1) }, enabled = ui.minutes > SessionRules.MIN_MINUTES) {
                Icon(painterResource(R.drawable.ic_minus), stringResource(R.string.cd_decrease), tint = colors.ink)
            }
            // The ring already announces the value; this is its visible twin.
            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.space4)
                    .clearAndSetSemantics {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = ui.minutes.toString(), style = GharsTheme.type.display, color = colors.ink)
                Text(
                    text = pluralStringResource(R.plurals.minutes_unit, ui.minutes),
                    style = GharsTheme.type.label,
                    color = colors.inkMuted,
                )
            }
            IconButton(onClick = { viewModel.stepMinutes(1) }, enabled = ui.minutes < SessionRules.MAX_MINUTES) {
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.cd_increase), tint = colors.ink)
            }
        }
        Spacer(Modifier.height(spacing.space8))
        GharsButton(
            text = stringResource(R.string.action_plant),
            onClick = onPlant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.space6))
    }
}
