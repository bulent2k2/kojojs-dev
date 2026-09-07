#!/bin/bash
# Tarayıcı testlerini koşar: Selenium + başsız Chrome.
#
#   ./test-tarayici.sh                 # bütün testler
#   ./test-tarayici.sh 'testOnly *Boya*'
#   KOJO_CHROME=/yol/chrome ./test-tarayici.sh
#
# Neden bu betik: build.sbt zaten SeleniumJSEnv'i VARSAYILAN yapıyor, ama iki
# şey eksik kalıyordu -- Chrome ikilisinin yeri ve ONUNLA AYNI ANA SÜRÜMDEN bir
# chromedriver. ChromeDriver ana sürüm uyuşmazlığında oturum açmayı reddediyor
# ("This version of ChromeDriver only supports Chrome version N"). Betik Chrome'u
# bulup sürümünü okuyor ve eşleşen sürücüyü indirip önbelleğe alıyor.
#
# NOT: kabuk değişken adları ASCII olmalı (bash Türkçe karakter kabul etmiyor).
set -u
BURASI="$(cd "$(dirname "$0")" && pwd)"
ONBELLEK="${KOJO_SURUCU_ONBELLEK:-$BURASI/.chromedriver}"
SBT_JAR="${KOJO_SBT_JAR:-$BURASI/../kojo/sbt-launch.1.5.5.jar}"

# --- 1. Chrome ikilisi ---------------------------------------------------
CHROME="${KOJO_CHROME:-}"
if [ -z "$CHROME" ]; then
  for aday in /opt/pw-browsers/chromium-*/chrome-linux/chrome \
              "$HOME/.cache/ms-playwright"/chromium-*/chrome-linux/chrome; do
    [ -x "$aday" ] && CHROME="$aday" && break
  done
fi
if [ -z "$CHROME" ]; then
  for ad in google-chrome chromium chromium-browser; do
    yol="$(command -v "$ad" 2>/dev/null)" && [ -n "$yol" ] && CHROME="$yol" && break
  done
fi
if [ -z "$CHROME" ] || [ ! -x "$CHROME" ]; then
  echo "HATA: Chrome bulunamadı. KOJO_CHROME ile yolunu verin." >&2
  exit 1
fi

SURUM="$("$CHROME" --version 2>/dev/null | grep -oE '[0-9]+(\.[0-9]+){2,3}' | head -1)"
if [ -z "$SURUM" ]; then
  echo "HATA: Chrome sürümü okunamadı: $CHROME" >&2
  exit 1
fi
ANA="${SURUM%%.*}"

# --- 2. Eşleşen chromedriver --------------------------------------------
SURUCU="$ONBELLEK/$SURUM/chromedriver"
if [ ! -x "$SURUCU" ]; then
  # Varsa aynı ANA sürümden başka bir yamayı kullan: chromedriver ana sürüme bakıyor.
  var="$(ls -1 "$ONBELLEK/$ANA."*/chromedriver 2>/dev/null | head -1 || true)"
  if [ -n "$var" ]; then
    SURUCU="$var"
  else
    echo "chromedriver $SURUM indiriliyor..."
    mkdir -p "$ONBELLEK/$SURUM"
    URL="https://storage.googleapis.com/chrome-for-testing-public/$SURUM/linux64/chromedriver-linux64.zip"
    if ! curl -fsSL --max-time 180 -o "$ONBELLEK/$SURUM/cd.zip" "$URL"; then
      echo "HATA: chromedriver indirilemedi: $URL" >&2
      echo "      (Chrome sürümü bir 'Chrome for Testing' yayını olmayabilir.)" >&2
      echo "      Elle indirip şuraya koyabilirsiniz: $ONBELLEK/$SURUM/chromedriver" >&2
      exit 1
    fi
    unzip -oq "$ONBELLEK/$SURUM/cd.zip" -d "$ONBELLEK/$SURUM"
    mv "$ONBELLEK/$SURUM/chromedriver-linux64/chromedriver" "$SURUCU"
    chmod +x "$SURUCU"
    rm -rf "$ONBELLEK/$SURUM/cd.zip" "$ONBELLEK/$SURUM/chromedriver-linux64"
  fi
fi

echo "Chrome:       $CHROME ($SURUM)"
echo "chromedriver: $SURUCU"

# --- 3. sbt ---------------------------------------------------------------
# LANG/LC_ALL şart: UTF-8 olmadan sbt Türkçe adlı .class dosyalarında çöküyor.
GOREVLER=("$@")
[ ${#GOREVLER[@]} -eq 0 ] && GOREVLER=("test")
exec env LANG=C.UTF-8 LC_ALL=C.UTF-8 KOJO_CHROME="$CHROME" \
  java -Dsbt.server.forcestart=false \
       -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
       -Dwebdriver.chrome.driver="$SURUCU" \
       -jar "$SBT_JAR" "${GOREVLER[@]}"
