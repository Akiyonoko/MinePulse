"""生成 MinePulse 的像素风图标（程序化绘制，不依赖外部素材）。

用法：python make_icon.py
输出：tools/icon/icon_128.png（原始像素）与 tools/icon/icon_512.png（放大 4 倍，上传用）
"""

import os
import random

from PIL import Image, ImageDraw

SIZE = 128
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon")


def blend(base, top, alpha):
    """把 top 以 alpha(0-1) 叠到 base 上。"""
    return tuple(
        int(b + (t - b) * alpha) for b, t in zip(base[:3], top[:3])
    ) + (255,)


def draw_background(img):
    """径向渐变的暗色岩壁 + 一点噪点，避免大面积死板的纯色。"""
    random.seed(7)
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - SIZE / 2 + 0.5, y - SIZE / 2 + 0.5
            dist = min(1.0, (dx * dx + dy * dy) ** 0.5 / (SIZE * 0.70))
            t = (1.0 - dist) ** 1.6
            noise = random.randint(-4, 4)
            r = max(0, int(8 + 22 * t) + noise)
            g = max(0, int(17 + 40 * t) + noise)
            b = max(0, int(19 + 40 * t) + noise)
            img.putpixel((x, y), (r, g, b, 255))


def draw_ring(img, cx, cy, radius, width, color, alpha):
    """像素化的同心脉冲环。"""
    inner = radius - width / 2
    outer = radius + width / 2
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if inner <= dist <= outer:
                img.putpixel((x, y), blend(img.getpixel((x, y)), color, alpha))


def draw_ore(draw, cx, cy, size, seed):
    """一块带晶体高光的矿石方块。"""
    rng = random.Random(seed)
    x0, y0 = cx - size // 2, cy - size // 2
    x1, y1 = x0 + size - 1, y0 + size - 1

    draw.rectangle([x0, y0, x1, y1], fill=(5, 14, 13, 255))          # 外描边
    draw.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=(20, 66, 62, 255))
    draw.rectangle([x0 + 2, y0 + 2, x1 - 2, y1 - 2], fill=(28, 92, 86, 255))

    # 晶体碎块
    for _ in range(size // 3):
        gx = rng.randint(x0 + 3, x1 - 4)
        gy = rng.randint(y0 + 3, y1 - 4)
        color = rng.choice([(79, 227, 200), (196, 255, 244), (44, 168, 150)])
        draw.rectangle([gx, gy, gx + 2, gy + 2], fill=color + (255,))

    # 上缘高光 + 下缘阴影
    draw.line([x0 + 2, y0 + 2, x1 - 2, y0 + 2], fill=(140, 246, 222, 210))
    draw.line([x0 + 2, y1 - 2, x1 - 2, y1 - 2], fill=(9, 34, 32, 220))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
    draw_background(img)

    # 脉冲环：中心偏亮，向外逐圈变淡
    draw_ring(img, SIZE // 2, SIZE // 2, 30, 3, (86, 255, 214), 0.34)
    draw_ring(img, SIZE // 2, SIZE // 2, 45, 2, (86, 255, 214), 0.20)
    draw_ring(img, SIZE // 2, SIZE // 2, 60, 2, (86, 255, 214), 0.10)

    draw = ImageDraw.Draw(img)

    # 斜向矿脉：从左上到右下，中心那块略大（脉冲源）
    blocks = [(16, 112, 22), (40, 88, 24), (64, 64, 28), (88, 40, 24), (112, 16, 22)]
    for index, (cx, cy, size) in enumerate(blocks):
        draw_ore(draw, cx, cy, size, seed=index * 31 + 5)

    # 中心方块再补一圈亮边，让视觉焦点明确
    cx, cy, size = blocks[2]
    draw.rectangle(
        [cx - size // 2, cy - size // 2, cx + size // 2, cy + size // 2],
        outline=(150, 255, 232, 255),
    )

    icon_128 = img
    icon_512 = img.resize((SIZE * 4, SIZE * 4), Image.NEAREST)

    path_128 = os.path.join(OUT_DIR, "icon_128.png")
    path_512 = os.path.join(OUT_DIR, "icon_512.png")
    icon_128.save(path_128)
    icon_512.save(path_512)
    print("saved:", path_128)
    print("saved:", path_512)


if __name__ == "__main__":
    main()
