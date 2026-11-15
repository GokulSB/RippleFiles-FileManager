import os
import sys
import zipfile
import tarfile
import gzip
import bz2
import lzma
import io

fixtures_dir = os.path.dirname(os.path.abspath(__file__))

# 1. Unicode & Emoji + Empty files & folders in ZIP
unicode_zip_path = os.path.join(fixtures_dir, "unicode_emoji.zip")
with zipfile.ZipFile(unicode_zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.writestr("🎉_celebrate_🚀.txt", "Emoji filename content")
    zf.writestr("日本語_ファイル.txt", "Japanese Unicode content")
    zf.writestr("empty_file.txt", "")
    # Empty directory entry
    zf.writestr("empty_folder/", "")
    zf.writestr("nested/level1/level2/deep.txt", "Deeply nested file")
print("Created:", unicode_zip_path)

# 2. Unicode & Emoji in TAR.GZ
unicode_targz_path = os.path.join(fixtures_dir, "unicode_emoji.tar.gz")
with tarfile.open(unicode_targz_path, "w:gz") as tf:
    for name, content in [
        ("🎉_celebrate_🚀.txt", b"Emoji filename content in tar.gz"),
        ("日本語_ファイル.txt", "Japanese Unicode in tar.gz".encode("utf-8")),
        ("empty_file.txt", b""),
        ("nested/level1/deep.txt", b"Deeply nested in tar.gz")
    ]:
        ti = tarfile.TarInfo(name=name)
        ti.size = len(content)
        tf.addfile(ti, io.BytesIO(content))
    # Add empty dir
    ti_dir = tarfile.TarInfo(name="empty_folder")
    ti_dir.type = tarfile.DIRTYPE
    tf.addfile(ti_dir)
print("Created:", unicode_targz_path)

# 3. TAR.BZ2
tarbz2_path = os.path.join(fixtures_dir, "sample.tar.bz2")
with tarfile.open(tarbz2_path, "w:bz2") as tf:
    content = b"Content inside tar.bz2"
    ti = tarfile.TarInfo(name="hello_bz2.txt")
    ti.size = len(content)
    tf.addfile(ti, io.BytesIO(content))
print("Created:", tarbz2_path)

# 4. TAR.XZ
tarxz_path = os.path.join(fixtures_dir, "sample.tar.xz")
with tarfile.open(tarxz_path, "w:xz") as tf:
    content = b"Content inside tar.xz"
    ti = tarfile.TarInfo(name="hello_xz.txt")
    ti.size = len(content)
    tf.addfile(ti, io.BytesIO(content))
print("Created:", tarxz_path)

# 5. Single stream GZIP (.gz)
gz_path = os.path.join(fixtures_dir, "single_stream.gz")
with gzip.open(gz_path, "wb") as f:
    f.write(b"Single stream GZIP payload data 1234567890")
print("Created:", gz_path)

# 6. Single stream BZIP2 (.bz2)
bz2_path = os.path.join(fixtures_dir, "single_stream.bz2")
with bz2.open(bz2_path, "wb") as f:
    f.write(b"Single stream BZIP2 payload data 1234567890")
print("Created:", bz2_path)

# 7. Single stream XZ (.xz)
xz_path = os.path.join(fixtures_dir, "single_stream.xz")
with lzma.open(xz_path, "wb") as f:
    f.write(b"Single stream XZ payload data 1234567890")
print("Created:", xz_path)

# 8. Malicious Symlink TAR (Symlink escaping outside destination)
symlink_tar_path = os.path.join(fixtures_dir, "malicious_symlink.tar")
with tarfile.open(symlink_tar_path, "w") as tf:
    ti_legit = tarfile.TarInfo(name="safe.txt")
    c = b"safe file"
    ti_legit.size = len(c)
    tf.addfile(ti_legit, io.BytesIO(c))
    
    ti_link = tarfile.TarInfo(name="evil_link.txt")
    ti_link.type = tarfile.SYMTYPE
    ti_link.linkname = "../../etc/passwd"
    tf.addfile(ti_link)
print("Created:", symlink_tar_path)

# 9. ZIP64 archive
zip64_path = os.path.join(fixtures_dir, "sample_zip64.zip")
with zipfile.ZipFile(zip64_path, "w", strict_timestamps=False) as zf:
    # Use allowZip64 and writestr with force_zip64
    zf._allowZip64 = True
    zf.writestr("large_entry.bin", b"Zip64 header test data")
print("Created:", zip64_path)

print("All advanced fixtures successfully generated!")
