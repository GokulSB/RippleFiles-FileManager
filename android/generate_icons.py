import os
from PIL import Image

src_path = r'C:\Users\Gokul\.gemini\antigravity\brain\9552224a-079b-4619-886d-43322b594d17\ripple_file_manager_icon_1786633552062.jpg'
src_img = Image.open(src_path).convert('RGBA')

# Get background color from top-left pixel
bg_color = src_img.getpixel((0, 0))
bg_hex = '#{:02x}{:02x}{:02x}'.format(bg_color[0], bg_color[1], bg_color[2])

print('Background color:', bg_hex)

# Create values directory and colors.xml if not exists
values_dir = r'd:/File Manager/android/app/src/main/res/values'
os.makedirs(values_dir, exist_ok=True)
colors_xml_path = os.path.join(values_dir, 'ic_launcher_background.xml')
with open(colors_xml_path, 'w') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">' + bg_hex + '</color>\n</resources>')

# Densities for adaptive icon foreground (108dp base)
sizes = {
    'mdpi': 108,
    'hdpi': 162,
    'xhdpi': 216,
    'xxhdpi': 324,
    'xxxhdpi': 432
}

base_dir = r'd:/File Manager/android/app/src/main/res'

for density, size in sizes.items():
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    safe_zone_size = int(size * (72.0 / 108.0))
    
    # Actually, to make it blend perfectly, instead of transparent canvas, let's use the beige background color!
    # If the background of the XML is beige, and the foreground PNG has beige padding, it's totally seamless!
    # Wait, if the foreground is entirely opaque, there is no parallax effect. Parallax effect requires transparency in the foreground.
    # So we MUST make the beige part of the foreground transparent.
    # Let's flood fill the beige color with transparent!
    
    # But wait, flood fill might be complex. Let's just crop the central folder icon (ignoring the beige background).
    # Since we scaled the original to safe zone, the folder icon fits perfectly. The background handles the rest.
    resized_src = src_img.resize((safe_zone_size, safe_zone_size), Image.Resampling.LANCZOS)
    
    offset = (size - safe_zone_size) // 2
    canvas.paste(resized_src, (offset, offset))
    
    dir_path = os.path.join(base_dir, f'mipmap-{density}')
    os.makedirs(dir_path, exist_ok=True)
    canvas.save(os.path.join(dir_path, 'ic_launcher_foreground.png'), 'PNG')

print('Adaptive icons generated successfully!')
