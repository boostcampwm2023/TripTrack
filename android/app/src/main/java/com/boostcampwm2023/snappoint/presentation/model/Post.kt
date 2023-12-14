package com.boostcampwm2023.snappoint.presentation.model

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class PostSummaryState(
    val uuid: String = "",
    val title: String = "",
    val author: String = "",
    val timeStamp: String = "",
    val summary: String = "",
    val email: String = "",
    val nickname: String = "",
    val postBlocks: List<PostBlockState> = emptyList()
)

@Serializable
sealed class PostBlockState {
    abstract val content: String
    abstract val uuid: String
    @Serializable
    data class TEXT(
        override val content: String = "",
        override val uuid: String = "",
    ) : PostBlockState()
    @Serializable
    data class IMAGE(
        override val content: String = "",
        override val uuid: String = "",
        val url480P: String = "",
        val url144P: String = "",
        val description: String = "",
        val position: PositionState = PositionState(0.0, 0.0),
        val fileUuid: String = ""
    ) : PostBlockState()
    @Serializable
    data class VIDEO(
        override val content: String = "",
        override val uuid: String = "",
        val description: String = "",
        val thumbnail720P: String = "",
        val thumbnail480P: String = "",
        val thumbnail144P: String = "",
        val thumbnailUuid: String = "",
        val position: PositionState = PositionState(0.0, 0.0),
        val fileUuid: String = ""
    ) : PostBlockState()
    @Serializable
    enum class ViewType {
        TEXT,
        IMAGE,
        VIDEO,
    }
}

@Serializable
data class PositionState(
    val latitude: Double,
    val longitude: Double
){
    fun asDoubleArray(): DoubleArray{
        return doubleArrayOf(latitude, longitude)
    }
    fun asLatLng(): LatLng{
        return LatLng(latitude, longitude)
    }
}
