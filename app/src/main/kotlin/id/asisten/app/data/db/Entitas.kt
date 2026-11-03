package id.asisten.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Room entities (database version 1). Conventions:
 *  - dates are epoch days (Long), times are minutes since midnight (Int), weekdays are ISO 1..7 (Int);
 *  - enums are stored by name (String) so reordering an enum never corrupts data;
 *  - no TypeConverters: conversion lives in Pemetaan.kt, where it is unit-tested.
 * Changing any column requires a version bump plus a Migration in AsistenMigrasi.
 */

@Entity(tableName = "memori")
data class MemoriEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kategori: String,
    val isi: String,
    val sensitif: Boolean,
    val dibuat: Long,
)

@Entity(tableName = "kegiatan")
data class KegiatanEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val judul: String,
    val tanggalHari: Long?,
    val hariMingguan: Int?,
    val mulaiMenit: Int,
    val selesaiMenit: Int,
    val prioritas: String,
    val catatan: String,
)

@Entity(tableName = "jam_sekolah")
data class JamSekolahEntitas(
    @PrimaryKey val hari: Int,
    val masukMenit: Int,
    val pulangMenit: Int,
)

@Entity(tableName = "pelajaran", indices = [Index(value = ["hari"])])
data class PelajaranEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hari: Int,
    val mulaiMenit: Int,
    val selesaiMenit: Int,
    val nama: String,
    val guru: String,
    val ruang: String,
    val istirahat: Boolean,
)

@Entity(tableName = "piket", indices = [Index(value = ["hari"])])
data class PiketEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hari: Int,
    val keterangan: String,
)

@Entity(tableName = "tugas")
data class TugasEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val judul: String,
    val jenis: String,
    val mapel: String,
    val tenggatHari: Long?,
    val prioritas: String,
    val selesai: Boolean,
    val selesaiPada: Long?,
    val catatan: String,
)

@Entity(tableName = "tujuan")
data class TujuanEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val judul: String,
    val jangka: String,
    val tenggatHari: Long?,
    val selesai: Boolean,
    val selesaiPada: Long?,
)

@Entity(
    tableName = "tonggak",
    foreignKeys = [
        ForeignKey(
            entity = TujuanEntitas::class,
            parentColumns = ["id"],
            childColumns = ["tujuanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tujuanId"])],
)
data class TonggakEntitas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tujuanId: Long,
    val judul: String,
    val selesai: Boolean,
)
