package com.peterlaurence.trekme.features.about.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.privacyPolicyUrl
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.ConfirmDialog
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.launch

/**
 * Displays the link on the user manual, and encourages the user to give feedback about the
 * application.
 */
@Composable
fun AboutStateful(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val helpUri = stringResource(id = R.string.help_url)
    val linkError = stringResource(id = R.string.link_error)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isShowingEmailConfirmation by remember { mutableStateOf(false) }

    val onLinkError = {
        scope.launch {
            snackbarHostState.showSnackbar(linkError)
        }
        Unit
    }

    AboutScreen(
        scrollState = scrollState,
        snackbarHostState = snackbarHostState,
        onUserManualClick = {
            runCatching {
                uriHandler.openUri(helpUri)
            }.onFailure {
                onLinkError()
            }
        },
        onAppRating = {
            val packageName = context.applicationContext.packageName
            try {
                uriHandler.openUri("market://details?id=$packageName")
            } catch (e: ActivityNotFoundException) {
                runCatching {
                    uriHandler.openUri("http://play.google.com/store/apps/details?id=$packageName")
                }.onFailure {
                    onLinkError()
                }
            }
        },
        onSendMail = { isShowingEmailConfirmation = true },
        onBackClick = onBackClick,
        onLinkError = onLinkError
    )

    if (isShowingEmailConfirmation) {
        ConfirmDialog(
            contentText = stringResource(R.string.email_explanation),
            onConfirmPressed = { sendMail(context)},
            onDismissRequest = { isShowingEmailConfirmation = false },
            confirmButtonText = stringResource(R.string.ok_dialog),
            cancelButtonText = stringResource(R.string.cancel_dialog_string)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    scrollState: ScrollState,
    snackbarHostState: SnackbarHostState,
    onUserManualClick: () -> Unit,
    onAppRating: () -> Unit,
    onSendMail: () -> Unit,
    onBackClick: () -> Unit,
    onLinkError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            UserManualSection(onUserManualClick)
            Spacer(Modifier.height(16.dp))
            AppRatingSection(onAppRating)
            Spacer(Modifier.height(16.dp))
            UserFeedback(onSendMail)
            Spacer(Modifier.height(16.dp))
            PrivacyPolicy(onLinkError = onLinkError)
        }
    }
}

@Composable
private fun ColumnScope.UserManualSection(
    onUserManualClick: () -> Unit
) {
    Text(
        stringResource(
            id = R.string.user_manual_title
        ),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.user_manual_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onUserManualClick,
    ) {
        Text(stringResource(id = R.string.user_manual_btn))
    }
}

@Composable
private fun ColumnScope.AppRatingSection(
    onAppRating: () -> Unit
) {
    Text(
        stringResource(
            id = R.string.rating_title
        ),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.rating_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onAppRating,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
        )
    ) {
        Text(stringResource(id = R.string.rate_the_app))
    }
}

@Composable
private fun ColumnScope.UserFeedback(
    onSendMail: () -> Unit
) {
    Text(
        stringResource(id = R.string.user_feedback),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.feedback_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    SmallFloatingActionButton(
        onClick = onSendMail,
        shape = CircleShape,
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        containerColor = MaterialTheme.colorScheme.secondary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
    ) {
        Icon(
            painterResource(id = R.drawable.ic_baseline_mail_outline_24),
            contentDescription = stringResource(id = R.string.mail_button),
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
private fun PrivacyPolicy(onLinkError: () -> Unit) {
    Text(
        stringResource(id = R.string.privacy_policy_title),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.privacy_policy_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )

    val uriHandler = LocalUriHandler.current
    val annotatedLinkString = buildAnnotatedString {
        val str = stringResource(id = R.string.privacy_policy_link)
        val placeHolderStr = stringResource(id = R.string.privacy_policy)
        val startIndex = str.indexOf('%')
        val endIndex = startIndex + placeHolderStr.length
        append(str.format(placeHolderStr))
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            ), start = 0, end = endIndex + 1
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
            ), start = startIndex, end = endIndex
        )

        addLink(
            LinkAnnotation.Clickable(
                tag = "TAG",
                linkInteractionListener = {
                    runCatching {
                        uriHandler.openUri(privacyPolicyUrl)
                    }.onFailure {
                        onLinkError()
                    }
                },
            ),
            start = startIndex,
            end = endIndex
        )
    }

    Text(text = annotatedLinkString)
}

private fun sendMail(context: Context) {
    val emailIntent = Intent(
        Intent.ACTION_SENDTO,
        Uri.fromParts("mailto", context.getString(R.string.email_support), null)
    )
    runCatching {
        context.startActivity(emailIntent)
    }
}


@Preview(locale = "fr")
@Preview(locale = "fr", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AboutPreview() {
    TrekMeTheme {
        Column(Modifier.size(400.dp, 700.dp)) {
            AboutScreen(
                rememberScrollState(),
                snackbarHostState = SnackbarHostState(),
                onUserManualClick = {},
                onAppRating = {},
                onSendMail = {},
                onBackClick = {},
                onLinkError = {}
            )
        }
    }
}