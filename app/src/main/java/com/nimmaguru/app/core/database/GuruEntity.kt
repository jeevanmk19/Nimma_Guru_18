package com.nimmaguru.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nimmaguru.app.core.model.Guru

@Entity(tableName = "gurus")
data class GuruEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameKn: String,
    val photoUrl: String,
    val village: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val skills: List<String>,
    val availability: Map<String, String>,
    val bioEn: String,
    val bioKn: String,
    val phone: String,
    val isPublic: Boolean,
    val contactConsent: Boolean,
    val langPref: String,
    val totalSessions: Int,
    val totalStudents: Int,
    val appreciationCount: Int,
    val avgRating: Float,
    val fameScore: Float,
    val createdAt: Long,
    val updatedAt: Long
)

fun GuruEntity.toDomain() = Guru(
    id = id,
    nameEn = nameEn,
    nameKn = nameKn,
    photoUrl = photoUrl,
    village = village,
    district = district,
    latitude = latitude,
    longitude = longitude,
    skills = skills,
    availability = availability,
    bioEn = bioEn,
    bioKn = bioKn,
    phone = phone,
    isPublic = isPublic,
    contactConsent = contactConsent,
    langPref = langPref,
    totalSessions = totalSessions,
    totalStudents = totalStudents,
    appreciationCount = appreciationCount,
    avgRating = avgRating,
    fameScore = fameScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Guru.toEntity() = GuruEntity(
    id = id,
    nameEn = nameEn,
    nameKn = nameKn,
    photoUrl = photoUrl,
    village = village,
    district = district,
    latitude = latitude,
    longitude = longitude,
    skills = skills,
    availability = availability,
    bioEn = bioEn,
    bioKn = bioKn,
    phone = phone,
    isPublic = isPublic,
    contactConsent = contactConsent,
    langPref = langPref,
    totalSessions = totalSessions,
    totalStudents = totalStudents,
    appreciationCount = appreciationCount,
    avgRating = avgRating,
    fameScore = fameScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)
