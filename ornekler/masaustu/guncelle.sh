#!/bin/bash
# Masaüstü Koco'nun (bulent2k2/kojo) Türkçe .kojo betiklerini bu dizine
# DEĞİŞTİRMEDEN kopyalar. Betikler kojo reposundaki göreli yollarıyla durur;
# böylece "// #yükle /samples/tr/..." satırları bu dizine göre çözülebilir.
#
# Kullanım:  KOJO=../../../kojo ./guncelle.sh      (varsayılan: ../../../kojo)
# NOT: kabuk değişken adları ASCII olmalı (bash Türkçe karakter kabul etmiyor).
set -eu
DIR="$(cd "$(dirname "$0")" && pwd)"
KOJO="${KOJO:-$DIR/../../../kojo}"
[ -d "$KOJO/src/main/resources/samples/tr" ] || { echo "kojo klonu bulunamadı: $KOJO (KOJO=... ile göster)" >&2; exit 1; }

# Kaynak dizinler: kojo reposuna göreli yol. installer/examples altında yalnız
# Türkçe dosyalar (*_tr*, tr/) alınır; İngilizce kardeşleri masaüstünde kalır.
KAYNAKLAR=(
  src/main/resources/samples/tr
  src/main/resources/samples/tr/kojo-kilavuz
  src/main/resources/robosim/tr
  src/main/resources/mathgames/tr
  src/main/resources/challenge/tr
  src/main/resources/ka-bridge/tr
  installer/examples/tiledgame
  installer/examples/othello
  installer/examples/othello/tr
  installer/examples/anagram
  installer/examples/anagram/tr
)

# Eski kopyayı temizle (yalnız betikler; bu dizindeki araç dosyalarına dokunma)
find "$DIR" -type f \( -name '*.kojo' -o -name '*.kojo.installed' \) -delete
find "$DIR" -mindepth 1 -type d -empty -delete

say=0
for k in "${KAYNAKLAR[@]}"; do
  for f in "$KOJO/$k"/*.kojo "$KOJO/$k"/*.kojo.installed; do
    [ -f "$f" ] || continue
    ad=$(basename "$f")
    case "$k" in
      installer/examples/tiledgame|installer/examples/othello|installer/examples/anagram)
        case "$ad" in *_tr.kojo|*_tr.kojo.installed) ;; *) continue ;; esac ;;
    esac
    mkdir -p "$DIR/$k"
    cp -p "$f" "$DIR/$k/$ad"
    say=$((say+1))
  done
done

# Kaynak damgası: hangi kojo sürümünden kopyalandı
{
  echo "kaynak: bulent2k2/kojo"
  echo "commit: $(git -C "$KOJO" rev-parse HEAD 2>/dev/null || echo bilinmiyor)"
  echo "tarih:  $(date -u +%Y-%m-%d)"
  echo "betik:  $say"
} > "$DIR/KAYNAK.txt"
echo "$say betik kopyalandı -> $DIR"
cat "$DIR/KAYNAK.txt"
