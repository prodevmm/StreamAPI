package com.streamapi.custom

import android.content.Context

class StreamBuilder(internal val context: Context, internal val url: String) {
    internal var timeout = 10000L
    internal var skipResolution = false
    internal var resolutionProcessGap = 1000L

    /**
     * Timeout for the whole process. Default value is 10000
     * @param timeout
     * @return StreamAPIBuilder
     * @see StreamBuilder
     */
    @Suppress("unused")
    fun setTimeout(timeout: Long): StreamBuilder {
        this.timeout = timeout
        return this
    }


    /**
     * Skip resolution will skip the process of extracting resolution names which takes +1 second by default.
     */
    @Suppress("unused")
    fun skipResolutionProcess(): StreamBuilder {
        this.skipResolution = true
        return this
    }

    /**
     * Resolution process gap is the waiting time after the stream extraction process
     * @param gap
     */
    @Suppress("unused")
    fun setResolutionProcessGap(gap: Long): StreamBuilder {
        this.resolutionProcessGap = gap
        return this
    }

    @Suppress("unused")
    fun build(): StreamAPI {
        return StreamAPI(this)
    }
}