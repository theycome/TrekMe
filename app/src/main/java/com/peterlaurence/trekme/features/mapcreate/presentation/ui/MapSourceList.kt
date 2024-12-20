package com.peterlaurence.trekme.features.mapcreate.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.OnBoardingTip
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PopupOrigin
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.MapSourceListViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSourceListUi(
    snackbarHostState: SnackbarHostState,
    sources: List<WmtsSource>,
    onSourceClick: (WmtsSource) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.mapcreate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(sources) { wmtsSource ->
                SourceRow(wmtsSource, onSourceClick)
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SourceRow(source: WmtsSource, onSourceClick: (WmtsSource) -> Unit) {
    Surface(
        onClick = { onSourceClick(source) },
        Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .weight(1f)
            ) {
                if (source == WmtsSource.IGN) {
                    Spacer(Modifier.height(24.dp))
                }

                Text(
                    text = getTitleForSource(source),
                    fontSize = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = getSubtitleForSource(source),
                )

                if (source == WmtsSource.IGN) {
                    val openDialog = remember { mutableStateOf(false) }
                    val annotatedString = buildAnnotatedString {
                        val text = stringResource(id = R.string.ign_legal_notice_btn)

                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                textDecoration = TextDecoration.Underline
                            ), start = 0, end = text.length
                        )

                        withLink(
                            LinkAnnotation.Clickable (
                                tag = "TAG",
                                linkInteractionListener = {
                                    openDialog.value = true
                                },
                            )
                        ) {
                            append(text)
                        }
                    }

                    if (openDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                                openDialog.value = false
                            },
                            text = {
                                Text(text = stringResource(id = R.string.ign_legal_notice))
                            },
                            confirmButton = {
                                TextButton(onClick = { openDialog.value = false }) {
                                    Text(stringResource(id = R.string.ok_dialog))
                                }
                            },
                        )
                    }

                    Text(
                        text = annotatedString,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Image(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp, end = 16.dp)
                    .size(90.dp),
                painter = getImageForSource(source),
                contentDescription = stringResource(id = R.string.accessibility_mapsource_image)
            )
        }
    }
}

@Composable
fun getTitleForSource(source: WmtsSource): String {
    return when (source) {
        WmtsSource.IGN -> stringResource(R.string.ign_source)
        WmtsSource.SWISS_TOPO -> stringResource(R.string.swiss_topo_source)
        WmtsSource.OPEN_STREET_MAP -> stringResource(R.string.open_street_map_source)
        WmtsSource.USGS -> stringResource(R.string.usgs_map_source)
        WmtsSource.IGN_SPAIN -> stringResource(R.string.ign_spain_source)
        WmtsSource.ORDNANCE_SURVEY -> stringResource(R.string.ordnance_survey_source)
        WmtsSource.IGN_BE -> stringResource(id = R.string.ign_be_source)
    }
}

@Composable
private fun getSubtitleForSource(source: WmtsSource): String {
    return when (source) {
        WmtsSource.IGN -> stringResource(R.string.ign_source_description)
        WmtsSource.SWISS_TOPO -> stringResource(R.string.swiss_topo_source_description)
        WmtsSource.OPEN_STREET_MAP -> stringResource(R.string.open_street_map_source_description)
        WmtsSource.USGS -> stringResource(R.string.usgs_map_source_description)
        WmtsSource.IGN_SPAIN -> stringResource(R.string.ign_spain_source_description)
        WmtsSource.ORDNANCE_SURVEY -> stringResource(R.string.ordnance_survey_source_description)
        WmtsSource.IGN_BE -> stringResource(R.string.ign_be_description)
    }
}

@Composable
private fun getImageForSource(source: WmtsSource): Painter {
    return when (source) {
        WmtsSource.IGN -> painterResource(R.drawable.ign_logo)
        WmtsSource.SWISS_TOPO -> painterResource(R.drawable.ic_swiss_topo_logo)
        WmtsSource.OPEN_STREET_MAP -> painterResource(R.drawable.openstreetmap_logo)
        WmtsSource.USGS -> painterResource(R.drawable.usgs_logo)
        WmtsSource.IGN_SPAIN -> painterResource(R.drawable.ign_spain_logo)
        WmtsSource.ORDNANCE_SURVEY -> painterResource(R.drawable.ordnance_survey_logo)
        WmtsSource.IGN_BE -> painterResource(id = R.drawable.ngi_be)
    }
}

@Composable
fun MapSourceListStateful(
    viewModel: MapSourceListViewModel,
    onSourceClick: (WmtsSource) -> Unit,
    onBackClick: () -> Unit
) {
    val sourceList by viewModel.sourceList.collectAsState()
    val showOnBoarding by viewModel.showOnBoarding.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission should always be granted
    }

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noInternetWarning = stringResource(id = R.string.no_internet)
    LaunchedEffectWithLifecycle(flow = viewModel.events) { event ->
        when (event) {
            MapSourceListViewModel.Event.NoInternet -> {
                scope.launch {
                    /* Check internet permission */
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.INTERNET
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        launcher.launch(Manifest.permission.INTERNET)
                    }

                    snackbarHostState.showSnackbar(
                        message = noInternetWarning,
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }
    }

    BoxWithConstraints {
        MapSourceListUi(snackbarHostState, sourceList, onSourceClick, onBackClick = onBackClick)
        if (showOnBoarding) {
            OnBoardingTip(
                modifier = Modifier
                    .width(min(maxWidth * 0.8f, 310.dp))
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter),
                popupOrigin = PopupOrigin.BottomCenter,
                text = stringResource(
                    id = R.string.onboarding_map_create
                ),
                delayMs = 500,
                onAcknowledge = { viewModel.hideOnboarding() }
            )
        }
    }
}
