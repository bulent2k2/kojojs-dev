#!/bin/bash
# Masaüstü Koco'nun ses ve görüntü dosyalarını (kojo: src/main/resources/media)
# bu dizine DEĞİŞTİRMEDEN kopyalar. Betikler bunlara masaüstündeki gibi
# "/media/collidium/hit.mp3" yoluyla erişir (Ses.vuruş, Görünüş.araba, ...);
# koco-deploy'daki nginx /media/ isteğini bu dizine yönlendirir.
#
# Kullanım:  KOJO=../../kojo ./guncelle.sh      (varsayılan: ../../kojo)
set -eu
DIR="$(cd "$(dirname "$0")" && pwd)"
KOJO="${KOJO:-$DIR/../../kojo}"
[ -d "$KOJO/src/main/resources/media" ] || { echo "kojo klonu bulunamadı: $KOJO (KOJO=... ile göster)" >&2; exit 1; }
find "$DIR" -mindepth 1 -type d -exec rm -rf {} + 2>/dev/null || true
cp -r "$KOJO/src/main/resources/media"/* "$DIR/"
{
  echo "kaynak: bulent2k2/kojo src/main/resources/media"
  echo "commit: $(git -C "$KOJO" rev-parse HEAD 2>/dev/null || echo bilinmiyor)"
  echo "tarih:  $(date -u +%Y-%m-%d)"
  echo "dosya:  $(find "$DIR" -type f ! -name 'guncelle.sh' ! -name 'KAYNAK.txt' ! -name 'README.md' | wc -l | tr -d ' ')"
} > "$DIR/KAYNAK.txt"
cat "$DIR/KAYNAK.txt"
