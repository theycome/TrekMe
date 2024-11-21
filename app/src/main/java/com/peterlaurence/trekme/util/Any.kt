package com.peterlaurence.trekme.util

import android.util.Log

/**
 * Created by Ivan Yakushev on 17.11.2024
 */
// TODO - enhance with clickable file-line link
fun Any.log(objekt: Any) =
    Log.i(this::class.simpleName, "$objekt - [${objekt::class.qualifiedName}]")
