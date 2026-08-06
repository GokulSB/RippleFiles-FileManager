import urllib.request
import re

url = "https://raw.githubusercontent.com/topjohnwu/libsu/master/core/src/main/java/com/topjohnwu/superuser/Shell.java"
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    html = urllib.request.urlopen(req).read().decode('utf-8')
    for line in html.split('\n'):
        if 'public Builder' in line or 'class Builder' in line:
            print(line)
except Exception as e:
    print(e)
