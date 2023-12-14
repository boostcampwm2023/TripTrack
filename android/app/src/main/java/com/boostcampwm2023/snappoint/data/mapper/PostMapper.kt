package com.boostcampwm2023.snappoint.data.mapper

import com.boostcampwm2023.snappoint.data.remote.model.BlockType
import com.boostcampwm2023.snappoint.data.remote.model.File
import com.boostcampwm2023.snappoint.data.remote.model.PostBlock
import com.boostcampwm2023.snappoint.data.remote.model.response.DeletePostResponse
import com.boostcampwm2023.snappoint.data.remote.model.response.GetPostResponse
import com.boostcampwm2023.snappoint.presentation.model.PositionState
import com.boostcampwm2023.snappoint.presentation.model.PostBlockCreationState
import com.boostcampwm2023.snappoint.presentation.model.PostBlockState
import com.boostcampwm2023.snappoint.presentation.model.PostSummaryState

fun PostBlock.asPostBlockState(): PostBlockState {
    return when(type){
        BlockType.TEXT.type -> {
            PostBlockState.TEXT(
                uuid = blockUuid!!,
                content = this.content,
            )
        }
        else -> {
            if(files!!.size == 1 && this.files[0].mimeType!!.startsWith("image")){
                PostBlockState.IMAGE(
                    uuid = blockUuid!!,
                    description = this.content,
                    content = this.files[0].url!!,
                    url480P = this.files[0].url480P!!,
                    url144P = this.files[0].url144P!!,
                    position = this.asPositionState(),
                    fileUuid = this.files[0].fileUuid
                )
            } else {
                val (thumbnail, video) = this.files.partition { it.mimeType!!.startsWith("image") }
                PostBlockState.VIDEO(
                    uuid = blockUuid!!,
                    description = this.content,
                    content = video[0].url!!,
                    position = this.asPositionState(),
                    thumbnail144P = if(thumbnail.isEmpty()) "" else thumbnail[0].url144P!!,
                    thumbnail480P = if(thumbnail.isEmpty()) "" else thumbnail[0].url480P!!,
                    thumbnail720P = if(thumbnail.isEmpty()) "" else thumbnail[0].url720P!!,
                    thumbnailUuid = if(thumbnail.isEmpty()) "" else thumbnail[0].fileUuid,
                    fileUuid = video[0].fileUuid
                )
            }

        }
    }
}

fun PostBlockCreationState.asPostBlock(): PostBlock {
    return when (this) {
        is PostBlockCreationState.TEXT -> {
            PostBlock(
                blockUuid = this.uuid,
                type = BlockType.TEXT.type,
                content = this.content,
            )
        }

        is PostBlockCreationState.IMAGE -> {
            PostBlock(
                blockUuid = this.uuid,
                content = this.description,
                type = BlockType.MEDIA.type,
                latitude = this.position.latitude,
                longitude = this.position.longitude,
                files = listOf(File(fileUuid = this.fileUuid)),
            )
        }

        is PostBlockCreationState.VIDEO -> {
            PostBlock(
                blockUuid = this.uuid,
                content = this.description,
                type = BlockType.MEDIA.type,
                latitude = this.position.latitude,
                longitude = this.position.longitude,
                files = listOf(File(fileUuid = this.fileUuid)),
            )
        }
    }
}

fun PostBlock.asPositionState(): PositionState {
    return PositionState(
        latitude = this.latitude!!,
        longitude = this.longitude!!
    )
}

fun GetPostResponse.asPostSummaryState(): PostSummaryState {
    return PostSummaryState(
        uuid = this.postUuid,
        title = this.title,
        author = this.nickname,
        timeStamp = this.createdAt,
        summary = this.summary,
        email = email,
        nickname = nickname,
        postBlocks = this.blocks.map { it.asPostBlockState() }
    )
}

fun List<GetPostResponse>.asPostSummaryState(): List<PostSummaryState> {
    return this.map { response ->
        response.asPostSummaryState()
    }
}

fun DeletePostResponse.asPostSummaryState(): PostSummaryState {
    return PostSummaryState(
        uuid = this.postUuid,
        title = this.title,
        author = this.nickname,
        timeStamp = this.createdAt,
        summary = this.summary,
        email = email,
        nickname = nickname,
        postBlocks = this.blocks.map { it.asPostBlockState() }
    )
}