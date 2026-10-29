package com.minimal.music.data.model

data class AddedFolder(
    val id: Long = 0L,
    val name: String,
    val path: String,
    val songCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)
