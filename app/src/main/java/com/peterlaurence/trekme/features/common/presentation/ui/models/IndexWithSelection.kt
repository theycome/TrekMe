package com.peterlaurence.trekme.features.common.presentation.ui.models

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.features.common.presentation.ui.models.IndexWithSelection.Type.EVEN
import com.peterlaurence.trekme.features.common.presentation.ui.models.IndexWithSelection.Type.ODD
import com.peterlaurence.trekme.features.common.presentation.ui.models.IndexWithSelection.Type.SELECTED

/**
 * Created by Ivan Yakushev on 14.01.2025
 */
class IndexWithSelection(
    private val index: Int,
    private val selectedIndex: Int,
) {

    val type: Type
        get() =
            if (index == selectedIndex) SELECTED
            else if (index % 2 == 0) EVEN
            else ODD

    enum class Type { SELECTED, ODD, EVEN }

}

@Composable
fun Color.Companion.of(indexWithSelection: IndexWithSelection): Color =
    when (indexWithSelection.type) {
        SELECTED -> MaterialTheme.colorScheme.tertiaryContainer
        ODD -> Transparent
        EVEN -> MaterialTheme.colorScheme.surfaceVariant
    }
