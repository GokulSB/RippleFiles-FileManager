import urllib.request
import zipfile
import io

url = "https://repo1.maven.org/maven2/dev/rikka/shizuku/api/13.1.5/api-13.1.5-sources.jar"
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib.request.urlopen(req)
    data = response.read()
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for info in z.infolist():
            if "Shizuku.java" in info.filename:
                text = z.read(info.filename).decode('utf-8')
                for line in text.split("\n"):
                    if "Process" in line and "public" in line:
                        print(line)
except Exception as e:
    print(e)
