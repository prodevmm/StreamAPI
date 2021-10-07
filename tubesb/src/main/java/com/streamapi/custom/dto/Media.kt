package com.streamapi.custom.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Media(
    val quality: String,
    val resolution: String,
    val fileSize: String,
    val url: String,
    internal val downloadRoute: String
) : Parcelable {
    override fun toString(): String {
        return quality
    }
}