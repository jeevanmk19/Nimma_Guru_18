package com.nimmaguru.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nimmaguru.app.core.model.AppreciationNote

@Entity(tableName = "appreciations")
data class AppreciationEntity(
    @PrimaryKey val id: String,
    val guruId: String,
    val studentName: String,
    val message: String,
    val photoUrl: String?,
    val rating: Int,
    val isApproved: Boolean,
    val createdAt: Long
)

fun AppreciationEntity.toDomain() = AppreciationNote(
    id = id,
    guruId = guruId,
    studentName = studentName,
    message = message,
    photoUrl = photoUrl,
    rating = rating,
    isApproved = isApproved,
    createdAt = createdAt
)

fun AppreciationNote.toEntity() = AppreciationEntity(
    id = id,
    guruId = guruId,
    studentName = studentName,
    message = message,
    photoUrl = photoUrl,
    rating = rating,
    isApproved = isApproved,
    createdAt = createdAt
)
