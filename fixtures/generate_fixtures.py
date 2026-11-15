# generate_fixtures.py
import os
import sys
import zipfile
import io

fixtures_dir = os.path.dirname(os.path.abspath(__file__))

# 1. Plain ZIP
plain_zip_path = os.path.join(fixtures_dir, "parity_plain.zip")
with zipfile.ZipFile(plain_zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.writestr("file1.txt", "Content of file 1")
    zf.writestr("file2.txt", "Content of file 2: 1234567890")
    zf.writestr("subfolder/inner.txt", "Inner file in subfolder")
print("Created:", plain_zip_path)

# 2. Malicious Zip-Slip ZIP
zipslip_path = os.path.join(fixtures_dir, "parity_zipslip.zip")
with zipfile.ZipFile(zipslip_path, "w") as zf:
    zf.writestr("legit.txt", "This is a legitimate file")
    zf.writestr("../../evil_escape.txt", "PWNED: Should never be extracted!")
    zf.writestr("/root_escape.txt", "ROOT ESCAPE: Should never be extracted!")
print("Created:", zipslip_path)

# 3. 10k Small Files ZIP (Streaming / Flatline memory test)
tenk_path = os.path.join(fixtures_dir, "parity_10k.zip")
with zipfile.ZipFile(tenk_path, "w", zipfile.ZIP_STORED) as zf:
    for i in range(10000):
        zf.writestr(f"items/file_{i:05d}.txt", f"data_{i}")
print("Created:", tenk_path)
