"""Rebuild the small antialiased status glyphs and cut out the existing XH mark (Pillow)."""
from pathlib import Path
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/meteor-client/textures/icons"
out = root / "status"
out.mkdir(exist_ok=True)
S = 16

def icon(name, draw):
    image = Image.new("RGBA", (16 * S, 16 * S))
    canvas = ImageDraw.Draw(image)
    def line(points, width=1.5):
        canvas.line([(round(x*S), round(y*S)) for x, y in points], fill="white", width=round(width*S), joint="curve")
    def box(bounds, radius=0, fill=None, width=1.5):
        canvas.rounded_rectangle(tuple(round(v*S) for v in bounds), round(radius*S), fill=fill, outline=None if fill else "white", width=round(width*S))
    def ellipse(bounds):
        canvas.ellipse(tuple(round(v*S) for v in bounds), fill="white")
    def arc(bounds, start, end, width=1.5):
        canvas.arc(tuple(round(v*S) for v in bounds), start, end, fill="white", width=round(width*S))
    draw(line, box, ellipse, arc)
    image.resize((64, 64), Image.Resampling.LANCZOS).save(out / f"{name}.png")

icon("user", lambda l,b,e,a: (e((5.2,1.5,10.8,7.1)), b((2.2,8.2,13.8,14.4),2.5,"white")))
icon("fps", lambda l,b,e,a: (b((2,8,4.5,14),.6,"white"), b((6.8,2,9.3,14),.6,"white"), b((11.5,5.5,14,14),.6,"white")))
icon("server", lambda l,b,e,a: (b((2,1.6,14,7),1), b((2,9,14,14.4),1), e((4,3.6,5.5,5.1)), e((4,11,5.5,12.5)), l([(8,4.3),(12,4.3)],1), l([(8,11.7),(12,11.7)],1)))
icon("version", lambda l,b,e,a: (l([(8,1.2),(14,4.6),(14,11.4),(8,14.8),(2,11.4),(2,4.6),(8,1.2)]), l([(2,4.6),(8,8),(14,4.6)]), l([(8,8),(8,14.8)]), l([(8,1.2),(8,4)],1)))
icon("ping", lambda l,b,e,a: (a((-.5,2,16.5,18),220,320,1.8), a((2.5,6,13.5,16),220,320,1.8), a((5.4,9.7,10.6,14.6),220,320,1.8), e((7,13,9,15))))
icon("tps", lambda l,b,e,a: (a((1.5,2.5,14.5,15.5),155,355,1.7), l([(2.2,12),(13.8,12)],1.7), l([(8,10),(11.8,5.8)],1.6), e((6.9,8.9,9.1,11.1))))

# The existing user's logo has a dark photographic background. Remove only that
# background; retain the original white faces, gold offset, and edge colours.
brand = Image.open(root / "gui/xiaohsense.jpg").convert("RGBA")
pixels = []
for r, g, b, _ in brand.getdata():
    alpha = max(0, min(255, round((max(r,g,b) - 48) * 255 / 96)))
    pixels.append((r,g,b,alpha))
brand.putdata(pixels)
brand = brand.crop(brand.getbbox())
brand.thumbnail((256, 144), Image.Resampling.LANCZOS)
canvas = Image.new("RGBA", (256, 144))
canvas.alpha_composite(brand, ((256-brand.width)//2, (144-brand.height)//2))
canvas.save(out / "xh.png")
