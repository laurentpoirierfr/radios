#!/usr/bin/env python3
"""
make_logo.py — fabrique le logo 128x128 d'une station au format attendu par
config/radios.json (data URI base64, PNG ou WebP selon le plus léger).

Ligne de commande (affiche la data URI sur la sortie standard) :

    tools/make_logo.py IMAGE [--bg '#141018'] [--pad 0.1] [--crop center]
    tools/make_logo.py --generic "RADIO|CAPRICE" "DUB TECHNO" [--accent '#9B7DFF']

IMAGE : PNG, JPEG, WebP, ICO (plus grande taille) ou SVG (rendu via librsvg).

Module Python (scripts d'ajout de genre) :

    from make_logo import fit, generic
    logo = fit("telechargements/logo.png", bg="#000000", pad=0.05)

Dépendances : Pillow ; pour le SVG, PyGObject + librsvg (présents sur GNOME).
"""
import argparse
import base64
import io
import os
import sys

from PIL import Image, ImageDraw, ImageFont

SIZE = 128
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def _hex(color):
    """'#RRGGBB' -> (r, g, b, 255) ; None reste None."""
    if color is None:
        return None
    c = color.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)


def encode(img):
    """Encode en data URI : PNG si pas nettement plus lourd que le WebP, sinon WebP."""
    png, webp = io.BytesIO(), io.BytesIO()
    img.save(png, "PNG", optimize=True)
    img.save(webp, "WEBP", quality=90, method=6)
    p, w = png.getvalue(), webp.getvalue()
    fmt, data = ("png", p) if len(p) <= len(w) * 1.3 else ("webp", w)
    return f"data:image/{fmt};base64," + base64.b64encode(data).decode()


def load(path):
    """Ouvre une image (y compris ICO et SVG) en RGBA."""
    if path.lower().endswith(".svg"):
        import warnings
        import gi
        warnings.filterwarnings("ignore", category=DeprecationWarning)
        gi.require_version("Rsvg", "2.0")
        from gi.repository import Rsvg
        handle = Rsvg.Handle.new_from_file(path)
        handle.set_dpi(600)  # rendu large, réduit ensuite
        tmp = io.BytesIO(handle.get_pixbuf().save_to_bufferv("png", [], [])[1])
        return Image.open(tmp).convert("RGBA")
    im = Image.open(path)
    if im.format == "ICO":
        im = im.ico.getimage(max(im.ico.sizes()))
    return im.convert("RGBA")


def fit(path, bg=None, pad=0.0, crop=None, max_scale=None):
    """
    Logo carré 128x128 : recadrage optionnel ('center' = carré central),
    marge relative `pad`, fond `bg` (#RRGGBB) sinon transparence conservée.
    """
    im = load(path)
    if crop == "center":
        side = min(im.size)
        left, top = (im.width - side) // 2, (im.height - side) // 2
        im = im.crop((left, top, left + side, top + side))
    inner = int(SIZE * (1 - 2 * pad))
    scale = inner / max(im.size)
    if max_scale:
        scale = min(scale, max_scale)
    im = im.resize((max(1, round(im.width * scale)), max(1, round(im.height * scale))), Image.LANCZOS)
    canvas = Image.new("RGBA", (SIZE, SIZE), _hex(bg) or (0, 0, 0, 0))
    canvas.alpha_composite(im, ((SIZE - im.width) // 2, (SIZE - im.height) // 2))
    return encode(canvas.convert("RGB") if bg else canvas)


def generic(title_lines, subtitle="", accent="#9B7DFF", bg="#16121C"):
    """
    Logo générique sobre quand aucun logo officiel n'est exploitable :
    nom en blanc (1 à 3 lignes), sous-titre et filet bas dans la couleur `accent`.
    À signaler dans le champ `notes` de la station.
    """
    acc = _hex(accent)[:3]
    canvas = Image.new("RGB", (SIZE, SIZE), _hex(bg)[:3])
    d = ImageDraw.Draw(canvas)
    d.rectangle((0, SIZE - 6, SIZE, SIZE), fill=acc)
    big = ImageFont.truetype(FONT, 19 if max(len(t) for t in title_lines) <= 9 else 15)
    small = ImageFont.truetype(FONT, 12)
    lines = len(title_lines) + (1 if subtitle else 0)
    y = (SIZE - 6 - lines * 24) / 2
    for line in title_lines:
        w = d.textlength(line, font=big)
        d.text(((SIZE - w) / 2, y), line, font=big, fill=(240, 240, 245))
        y += 24
    if subtitle:
        w = d.textlength(subtitle, font=small)
        d.text(((SIZE - w) / 2, y + 6), subtitle, font=small, fill=acc)
    return encode(canvas)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("image", nargs="?", help="image source (png, jpg, webp, ico, svg)")
    ap.add_argument("--bg", help="couleur de fond #RRGGBB (défaut : transparent)")
    ap.add_argument("--pad", type=float, default=0.0, help="marge relative, ex. 0.1")
    ap.add_argument("--crop", choices=["center"], help="recadrer en carré central")
    ap.add_argument("--generic", nargs=2, metavar=("TITRE", "SOUS_TITRE"),
                    help='logo générique, lignes du titre séparées par | ("RADIO|CAPRICE" "ACID")')
    ap.add_argument("--accent", default="#9B7DFF", help="couleur du logo générique")
    ap.add_argument("--preview", help="écrit aussi un aperçu PNG à ce chemin")
    a = ap.parse_args()

    if a.generic:
        uri = generic(a.generic[0].split("|"), a.generic[1], accent=a.accent)
    elif a.image:
        uri = fit(a.image, bg=a.bg, pad=a.pad, crop=a.crop)
    else:
        ap.error("donner une IMAGE ou --generic")
    if a.preview:
        with open(a.preview, "wb") as f:
            f.write(base64.b64decode(uri.split(",", 1)[1]))
    print(uri)


if __name__ == "__main__":
    sys.exit(main())
