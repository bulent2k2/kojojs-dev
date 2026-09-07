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
set -eu
BURASI="$(cd "$(dirname "$0")" && pwd)"
ONBELLEK="${KOJO_SURUCU_ONBELLEK:-$BURASI/.chromedriver}"
SBT_JAR="${KOJO_SBT_JAR:-$BURASI/../kojo/sbt-launch.1.5.5.jar}"

# --- 0. Platform --------------------------------------------------------
# chromedriver indirme adresi ve zip içindeki dizin adı platforma göre değişiyor.
case "$(uname -s)" in
  Linux)  PLATFORM=linux64 ;;
  Darwin) case "$(uname -m)" in
            arm64) PLATFORM=mac-arm64 ;;
            *)     PLATFORM=mac-x64 ;;
          esac ;;
  *) echo "HATA: desteklenmeyen işletim sistemi: $(uname -s)" >&2; exit 1 ;;
esac

# Bir ANA sürümün bilinen son yayınını sorar (tam sürüm bulunamazsa yedek yol).
son_yayin() {
  curl -fsSL --max-time 60 \
    "https://storage.googleapis.com/chrome-for-testing-public/LATEST_RELEASE_STABLE" 2>/dev/null \
    | grep -E "^$1\\." || true
}

# --- 1. Chrome ikilisi ---------------------------------------------------
CHROME="${KOJO_CHROME:-}"
if [ -z "$CHROME" ]; then
  for aday in \
      /opt/pw-browsers/chromium-*/chrome-linux/chrome \
      "$HOME/.cache/ms-playwright"/chromium-*/chrome-linux/chrome \
      /opt/pw-browsers/chromium-*/chrome-mac/Chromium.app/Contents/MacOS/Chromium \
      "$HOME/Library/Caches/ms-playwright"/chromium-*/chrome-mac/Chromium.app/Contents/MacOS/Chromium \
      "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
      "/Applications/Chromium.app/Contents/MacOS/Chromium"; do
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
    # Önce Chrome'un tam sürümü; olmazsa aynı ANA sürümün bilinen son yayını.
    # (chromedriver ANA sürüme bakıyor, yamaya değil. Playwright'ın Chromium
    # yapıları her zaman bir "Chrome for Testing" yayınına denk gelmiyor.)
    indi=""
    for aday_surum in "$SURUM" "$(son_yayin "$ANA")"; do
      [ -z "$aday_surum" ] && continue
      URL="https://storage.googleapis.com/chrome-for-testing-public/$aday_surum/$PLATFORM/chromedriver-$PLATFORM.zip"
      echo "chromedriver $aday_surum ($PLATFORM) indiriliyor..."
      mkdir -p "$ONBELLEK/$SURUM"
      if curl -fsSL --max-time 180 -o "$ONBELLEK/$SURUM/cd.zip" "$URL"; then indi="$aday_surum"; break; fi
    done
    if [ -z "$indi" ]; then
      echo "HATA: $ANA ana sürümü için chromedriver indirilemedi ($PLATFORM)." >&2
      echo "      Elle indirip şuraya koyabilirsiniz: $SURUCU" >&2
      rm -rf "$ONBELLEK/$SURUM"
      exit 1
    fi
    unzip -oq "$ONBELLEK/$SURUM/cd.zip" -d "$ONBELLEK/$SURUM"
    mv "$ONBELLEK/$SURUM/chromedriver-$PLATFORM/chromedriver" "$SURUCU"
    chmod +x "$SURUCU"
    rm -rf "$ONBELLEK/$SURUM/cd.zip" "$ONBELLEK/$SURUM/chromedriver-$PLATFORM"
  fi
fi

echo "Chrome:       $CHROME ($SURUM)"
echo "chromedriver: $SURUCU"

# --- 3. sbt ---------------------------------------------------------------
# LANG/LC_ALL şart: UTF-8 olmadan sbt Türkçe adlı .class dosyalarında çöküyor.
GOREVLER=("$@")
[ ${#GOREVLER[@]} -eq 0 ] && GOREVLER=("test")

if [ -f "$SBT_JAR" ]; then
  exec env LANG=C.UTF-8 LC_ALL=C.UTF-8 KOJO_CHROME="$CHROME" \
    java -Dsbt.server.forcestart=false \
         -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
         -Dwebdriver.chrome.driver="$SURUCU" \
         -jar "$SBT_JAR" "${GOREVLER[@]}"
elif command -v sbt >/dev/null 2>&1; then
  # Kardeş kojo klonu yoksa kurulu sbt de aynı işi görüyor.
  exec env LANG=C.UTF-8 LC_ALL=C.UTF-8 KOJO_CHROME="$CHROME" \
    sbt -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
        -Dwebdriver.chrome.driver="$SURUCU" "${GOREVLER[@]}"
else
  echo "HATA: ne $SBT_JAR var ne de PATH'te sbt." >&2
  echo "      KOJO_SBT_JAR ile launcher jar'ın yolunu verebilirsiniz." >&2
  exit 1
fi
