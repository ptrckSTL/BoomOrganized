package com.simplemobiletools.boomorganized.oauth

import com.google.api.client.util.DateTime
import com.google.api.services.drive.model.File

typealias DriveFile = File

data class SheetListItem(
    val title: String,
    val date: DateTime?,
    val description: String?,
    val id: String,
) {
    companion object {
        fun fromDriveFile(file: DriveFile) =
            SheetListItem(
                title = file.name,
                description = file.description,
                id = file.id,
                date = file.modifiedTime
            )
    }
}