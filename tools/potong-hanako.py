#!/usr/bin/env python3
"""Cuts the Hanako character sheet into the images the app uses.

Usage: python3 tools/potong-hanako.py [lembar.png] [folder-keluaran]
Defaults: docs/referensi/hanako-lembar-karakter.png -> app/src/main/res/drawable-nodpi

Coordinates are written for the 1536x1024 sheet and scaled by the real size of the file, so a larger
version of the same sheet (same layout) can be dropped in and the script run again for sharper images.
Requires Pillow with WebP support.
"""
import sys
from pathlib import Path
from PIL import Image

RAIL = Path(__file__).resolve().parent.parent
LEMBAR = Path(sys.argv[1]) if len(sys.argv) > 1 else RAIL / "docs/referensi/hanako-lembar-karakter.png"
KELUAR = Path(sys.argv[2]) if len(sys.argv) > 2 else RAIL / "app/src/main/res/drawable-nodpi"

REF_W, REF_H = 1536, 1024
INSET = 4  # px (reference scale) removed from every side to drop the lavender cell border

# name -> (left, top, right, bottom) on the 1536x1024 sheet. Names match EkspresiHanako in the app.
EKSPRESI_X = [437, 575, 714, 853, 992, 1130]
LEBAR_SEL = 131
TINGGI_SEL = 143  # same height for every face, so all avatars share one aspect ratio
BARIS = {"atas": (47, 47 + TINGGI_SEL), "bawah": (238, 238 + TINGGI_SEL)}
KOTAK = {}
for nama, baris, kolom in [
    ("normal", "atas", 0), ("senyum", "atas", 1), ("tertawa", "atas", 2),
    ("malu", "atas", 3), ("sedih", "atas", 4), ("marah", "atas", 5),
    ("bingung", "bawah", 0), ("terkejut", "bawah", 1), ("cemberut", "bawah", 2),
    ("mengantuk", "bawah", 3), ("berpikir", "bawah", 4), ("panik", "bawah", 5),
]:
    atas, bawah = BARIS[baris]
    kiri = EKSPRESI_X[kolom]
    KOTAK["hanako_" + nama] = (kiri, atas, kiri + LEBAR_SEL, bawah)

# Not cut yet (not used by the app so far), coordinates for later phases on the 1536x1024 sheet:
#   chibi panel (1378, 468, 1526, 722); gesture row y 450-685 (7 poses from x 390 to 1365);
#   motion row y 745-975 (6 poses from x 375 to 1395).


def main():
    gambar = Image.open(LEMBAR).convert("RGB")
    sx, sy = gambar.width / REF_W, gambar.height / REF_H
    KELUAR.mkdir(parents=True, exist_ok=True)
    for nama, (kiri, atas, kanan, bawah) in KOTAK.items():
        kotak = (
            round((kiri + INSET) * sx), round((atas + INSET) * sy),
            round((kanan - INSET) * sx), round((bawah - INSET) * sy),
        )
        potongan = gambar.crop(kotak)
        potongan.save(KELUAR / f"{nama}.webp", "WEBP", quality=92, method=6)
        print(f"{nama}.webp {potongan.width}x{potongan.height}")


if __name__ == "__main__":
    main()
