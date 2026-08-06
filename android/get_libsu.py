import urllib.request
import zipfile
import io

url = "https://repo1.maven.org/maven2/com/github/topjohnwu/libsu/core/6.0.0/core-6.0.0-sources.jar"
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib.request.urlopen(req)
    data = response.read()
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for info in z.infolist():
            if "Shell.java" in info.filename:
                text = z.read(info.filename).decode('utf-8')
                builder_section = text.split("class Builder")[1].split("public Shell build()")[0]
                print(builder_section[:1000]) # Print beginning
                print("====================================")
                print(builder_section[-1000:]) # Print end
except Exception as e:
    print(e)
