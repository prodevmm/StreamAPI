package com.streamapi.custom.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Stream(
    val resolution: String,
    val url: String
) : Parcelable {
    override fun toString(): String {
        return resolution
    }
}