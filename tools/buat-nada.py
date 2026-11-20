#!/usr/bin/env python3
"""Membuat 5 nada pengingat bawaan ASISTEN (app/src/main/res/raw/nada_*.ogg) secara sintesis.

Semua bunyi dihitung dari rumus sederhana (gelombang sinus dengan selubung), jadi tidak ada sampel orang lain
dan tidak ada masalah hak cipta. Skrip ini butuh numpy dan ffmpeg (dengan libvorbis); hasilnya sudah ikut di repo,
jalankan lagi hanya kalau ingin mengubah nadanya:  python3 tools/buat-nada.py

Skrip ini tidak bisa menilai apakah nadanya enak didengar: itu penilaian telinga. Ia hanya membuat dan memeriksa
sifat teknisnya (panjang, puncak, tidak ada bunyi 'klik' di awal dan akhir).
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 32000
KELUAR = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def waktu(detik):
    return np.arange(int(SR * detik)) / SR


def selubung(n, naik=0.004, turun=0.05):
    """Naik cepat dan turun halus supaya tidak ada klik."""
    e = np.ones(n)
    a, b = int(SR * naik), int(SR * turun)
    if a:
        e[:a] = np.linspace(0, 1, a)
    if b:
        e[-b:] *= np.linspace(1, 0, b)
    return e


def taruh(dasar, bunyi, mulai):
    i = int(SR * mulai)
    akhir = min(len(dasar), i + len(bunyi))
    dasar[i:akhir] += bunyi[: akhir - i]


def lonceng():
    total = np.zeros(int(SR * 2.6))
    # perbandingan frekuensi bagian-bagian lonceng (tidak harmonis, itulah yang membuatnya terdengar seperti logam)
    bagian = [(0.5, 0.35, 1.8), (1.0, 1.0, 1.4), (1.19, 0.55, 1.1), (1.5, 0.45, 0.9), (2.0, 0.40, 0.7), (2.74, 0.25, 0.45), (3.76, 0.12, 0.3)]
    for mulai, kuat in ((0.0, 1.0), (1.1, 0.75)):
        t = waktu(1.5)
        bunyi = sum(a * np.exp(-t / tau) * np.sin(2 * np.pi * 880 * r * t) for r, a, tau in bagian)
        bunyi *= np.minimum(1, t / 0.002)
        taruh(total, bunyi * kuat, mulai)
    return total


def kicau():
    total = np.zeros(int(SR * 1.5))
    for k, (mulai, awal, akhir) in enumerate(((0.05, 2900, 4100), (0.42, 3200, 4500), (0.80, 2700, 4300))):
        t = waktu(0.16)
        frek = awal + (akhir - awal) * (t / t[-1]) ** 0.7 + 90 * np.sin(2 * np.pi * 38 * t)
        fase = 2 * np.pi * np.cumsum(frek) / SR
        bunyi = np.sin(fase) + 0.25 * np.sin(2 * fase)
        bunyi *= np.sin(np.pi * t / t[-1]) ** 0.8
        taruh(total, bunyi, mulai)
    return total


def kotak_musik():
    # G5 C6 E6 G6 E6 C6 E6 G6 C7: melodi kecil naik-turun
    catatan = [784.0, 1046.5, 1318.5, 1568.0, 1318.5, 1046.5, 1318.5, 1568.0, 2093.0]
    jarak = 0.21
    total = np.zeros(int(SR * (jarak * len(catatan) + 1.0)))
    for i, f in enumerate(catatan):
        t = waktu(0.9)
        bunyi = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / 0.08)
                 + 0.12 * np.sin(2 * np.pi * 3.01 * f * t) * np.exp(-t / 0.05))
        bunyi *= np.exp(-t / 0.28) * np.minimum(1, t / 0.002)
        taruh(total, bunyi, i * jarak)
    return total


def bel_sekolah():
    t = waktu(2.6)
    getar = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 17 * t))  # pemukul yang bolak-balik
    getar = np.convolve(getar, np.ones(120) / 120, mode="same")  # dihaluskan agar tidak berderak
    inti = (np.sin(2 * np.pi * 1180 * t) + 0.7 * np.sin(2 * np.pi * 1580 * t) + 0.35 * np.sin(2 * np.pi * 2310 * t)
            + 0.2 * np.sin(2 * np.pi * 3050 * t))
    return inti * (0.35 + 0.65 * getar) * selubung(len(t), naik=0.01, turun=0.25)


def alarm_digital():
    total = np.zeros(int(SR * 2.1))
    for kelompok, mulai in enumerate((0.0, 1.1)):
        for i in range(4):
            t = waktu(0.11)
            f = 2000 if i % 2 == 0 else 2500
            bunyi = (np.sin(2 * np.pi * f * t) + 0.18 * np.sin(2 * np.pi * 3 * f * t)) * selubung(len(t), 0.004, 0.012)
            taruh(total, bunyi, mulai + i * 0.2)
    return total


NADA = {"lonceng": lonceng, "kicau": kicau, "kotak_musik": kotak_musik, "bel_sekolah": bel_sekolah, "alarm_digital": alarm_digital}
RMS_TARGET = 0.18  # kira-kira -15 dBFS: sama kerasnya satu dengan yang lain, dan tidak lebih pelan dari notifikasi biasa
PUNCAK_MAKS = 0.9


def normalkan(x):
    x = x - np.mean(x)
    x = x * (RMS_TARGET / np.sqrt(np.mean(x ** 2)))
    puncak = np.max(np.abs(x))
    if puncak > PUNCAK_MAKS:  # nada dengan puncak tajam diturunkan seluruhnya, tidak pernah dipotong (tanpa distorsi)
        x = x * (PUNCAK_MAKS / puncak)
    awal, akhir = int(SR * 0.003), int(SR * 0.03)
    x[:awal] *= np.linspace(0, 1, awal)
    x[-akhir:] *= np.linspace(1, 0, akhir)
    return x


def main():
    KELUAR.mkdir(parents=True, exist_ok=True)
    for nama, fungsi in NADA.items():
        x = normalkan(fungsi())
        with tempfile.TemporaryDirectory() as tmp:
            wav = Path(tmp) / "x.wav"
            with wave.open(str(wav), "wb") as w:
                w.setnchannels(1)
                w.setsampwidth(2)
                w.setframerate(SR)
                w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())
            tujuan = KELUAR / f"nada_{nama}.ogg"
            subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "3", str(tujuan)], check=True)
        print(f"{tujuan.name}: {len(x) / SR:.2f} dtk, {tujuan.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    sys.exit(main())
