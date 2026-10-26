"""把一张图缩放到指定边长（图标上传/游戏内显示用）。"""

import sys

from PIL import Image

if __name__ == "__main__":
    src, dst, size = sys.argv[1], sys.argv[2], int(sys.argv[3])
    img = Image.open(src).convert("RGBA")
    img.resize((size, size), Image.LANCZOS).save(dst, optimize=True)
    print("saved:", dst)
