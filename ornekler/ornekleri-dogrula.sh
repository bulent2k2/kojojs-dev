#!/bin/bash
# Örnek betikleri gerçek derleyiciye (/compile) gönderir ve hata dönmediğini doğrular.
#
#   ./ornekleri-dogrula.sh                              # bu dizindeki 10 ikojo örneği
#   ./ornekleri-dogrula.sh masaustu                     # masaüstü betikleri (özyineli)
#   ./ornekleri-dogrula.sh -g masaustu/derleme.tsv masaustu   # sonucu TSV'ye yaz
#   ./ornekleri-dogrula.sh -b masaustu/derleme.tsv masaustu   # önceki sonuçla karşılaştır:
#                                                        # yalnız GERİLEME varsa hata kodu döner
#   KOCO=http://localhost:7860 ./ornekleri-dogrula.sh    # yerel konteynere karşı
#
# -b olmadan herhangi bir kaldı = çıkış kodu 1 (ikojo örnekleri için doğru davranış:
# hepsi geçmeli). -b ile beklenen durum dosyası okunur; "geçti" beklenen bir betik
# kalırsa gerileme sayılır; beklenen "kaldı" bir betik geçerse ilerleme olarak yazılır.
# Üç durum: geçti / kaldı (derleyici hata verdi) / sunucu (HTTP 200 dönmedi -- betiğin
# değil sunucunun sorunu, ör. nginx gövde sınırı; gerileme sayılmaz).
#
# Taşınabilirlik: macOS'un /bin/bash'i 3.2 -- ilişkisel dizi (declare -A) YOK,
# beklenen durum awk ile dosyadan okunur.
#
# NOT: kabuk değişken adları ASCII olmalı (bash Türkçe karakter kabul etmiyor).
# Betikler yalnızca yazılımcık GÖVDESİ; buradaki prelude onları sarmalar --
# kojojs-editor'ün application.conf'undaki defaultSource ile aynı olmalı.
set -u
KOCO="${KOCO:-https://ikojo.fly.dev}"
DIR="$(cd "$(dirname "$0")" && pwd)"

GUNCELLE=""; BEKLENEN=""
while getopts "g:b:h" opt; do
  case $opt in
    g) GUNCELLE="$OPTARG" ;;
    b) BEKLENEN="$OPTARG" ;;
    h|*) sed -n '2,17p' "$0"; exit 0 ;;
  esac
done
shift $((OPTIND-1))

# Hedefler: dosya ya da dizin (dizinler özyineli taranır). Varsayılan: bu dizindeki *.kojo
hedefler=()
if [ $# -eq 0 ]; then
  for f in "$DIR"/*.kojo; do hedefler+=("$f"); done
else
  for h in "$@"; do
    if [ -d "$h" ]; then
      while IFS= read -r f; do hedefler+=("$f"); done < <(find "$h" -type f \( -name '*.kojo' -o -name '*.kojo.installed' \) | sort)
    elif [ -f "$h" ]; then hedefler+=("$h")
    else echo "bulunamadı: $h" >&2; exit 2; fi
  done
fi
[ ${#hedefler[@]} -gt 0 ] || { echo "doğrulanacak betik yok" >&2; exit 2; }

sar() {
  cat <<'PRE'
import fiddle.Fiddle.println
import scalajs.js

@js.annotation.JSExportTopLevel("ScalaFiddle")
object ScalaFiddle {
    import kojo.{SwedishTurtle, TurkishTurtle, Turtle, KojoWorldImpl, Vector2D, Picture}
    import kojo.doodle.Color._
    import kojo.Speed._
    import kojo.RepeatCommands._
    import kojo.syntax.Builtins
    implicit val kojoWorld: kojo.KojoWorld = new KojoWorldImpl()
    val builtins = new Builtins()
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
PRE
  cat "$1"
  echo "}"
}

# Betik adı: verilen kökün altındaki göreli yol (TSV anahtarı olarak kararlı kalsın)
ad_ver() {
  # kanonik mutlak yol; böylece "masaustu/x", "./masaustu/x" ve "/abs/.../masaustu/x"
  # aynı TSV anahtarını üretir
  local f
  f="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
  case "$f" in
    "$DIR"/*) echo "${f#"$DIR"/}" ;;
    *) echo "$f" ;;
  esac
}

# Beklenen durum: betik<TAB>durum ... satırlarından awk ile bak (bash 3.2 uyumlu)
if [ -n "$BEKLENEN" ]; then
  [ -f "$BEKLENEN" ] || { echo "beklenen dosyası yok: $BEKLENEN" >&2; exit 2; }
fi
beklenen_oku() { awk -F'\t' -v k="$1" '$1==k {print $2; exit}' "$BEKLENEN"; }

cikti=$(mktemp); ann_dosya=$(mktemp); sonuc=$(mktemp)
trap 'rm -f "$cikti" "$ann_dosya" "$sonuc"' EXIT

# Isınma: konteyner yeni kalktıysa derleyiciler kayıt olmadan ilk istekler
# 5xx döner ve onlarca betik yanlışlıkla "sunucu" olur. Küçük bir gövdeyle
# ilk 200 gelene kadar bekle (en çok ~2 dk).
isin() {
  local govde kod i
  govde=$(mktemp); printf 'satıryaz(1)\n' | sar /dev/stdin > "$govde"
  for i in $(seq 1 15); do
    kod=$(curl -s -m 120 -X POST --data-binary @"$govde" \
      -H "Content-Type: text/plain; charset=utf-8" \
      "$KOCO/compile?opt=fast" -o /dev/null -w "%{http_code}")
    [ "$kod" = "200" ] && { rm -f "$govde"; return 0; }
    echo "  derleyici hazır değil (HTTP $kod), bekleniyor..." >&2
    sleep 8
  done
  rm -f "$govde"; echo "UYARI: derleyici ısınmadı ($KOCO); sonuçlar 'sunucu' çıkabilir" >&2
}
isin

gecti=0; kaldi=0; sunucu=0; gerileme=0; ilerleme=0; eslesen=0
for f in "${hedefler[@]}"; do
  ad=$(ad_ver "$f")
  govde=$(mktemp); sar "$f" > "$govde"
  kod=""
  # derleyici ısınana kadar birkaç deneme -- yalnız bağlantı yok / geçit hataları
  # yinelenir (000, 502-504); 500 ve 4xx kalıcı sayılır, hemen "sunucu" olur
  for i in 1 2 3 4 5 6 7 8 9 10; do
    kod=$(curl -s -m 300 -X POST --data-binary @"$govde" \
      -H "Content-Type: text/plain; charset=utf-8" \
      "$KOCO/compile?opt=fast" -o "$cikti" -w "%{http_code}")
    case "$kod" in 000|502|503|504) sleep 8 ;; *) break ;; esac
  done
  rm -f "$govde"
  durum="kaldı"; ozet=""
  if [ "$kod" != "200" ]; then
    durum="sunucu"
    ozet="HTTP $kod: $( (gzip -dc "$cikti" 2>/dev/null || cat "$cikti") | head -c 80 | tr '\n\t' '  ')"
  else
    ann=$(python3 -c "
import gzip,json,io,sys
try: d=json.loads(gzip.open(sys.argv[1],'rt',encoding='utf-8').read())
except OSError: d=json.load(io.open(sys.argv[1],encoding='utf-8'))
# EditorAnnotation(row, col, text: Seq[String], tpe) -- shared/CompilerMessage.scala
a=[x for x in (d.get('annotations') or []) if x.get('tpe','error')!='warning'] if isinstance(d,dict) else []
print(len(a))
if a:
    x=a[0]; t=x.get('text', x.get('message', ''))
    if isinstance(t, list): t=' '.join(str(s) for s in t)
    sys.stderr.write((str(x.get('row', x.get('line','?')))+': '+str(t)).replace('\n',' ')[:160])
" "$cikti" 2>"$ann_dosya")
    if [ "$ann" = "0" ]; then durum="geçti"; else ozet="$ann hata: $(cat "$ann_dosya")"; fi
  fi
  case "$durum" in
    geçti) gecti=$((gecti+1)) ;;
    sunucu) sunucu=$((sunucu+1)) ;;
    *) kaldi=$((kaldi+1)) ;;
  esac
  isaret="  "
  if [ -n "$BEKLENEN" ]; then
    onceki=$(beklenen_oku "$ad")
    [ -n "$onceki" ] && eslesen=$((eslesen+1))
    # sunucu hatası betiğin gerilemesi değil; yalnız geçti -> kaldı gerileme
    if [ "$onceki" = "geçti" ] && [ "$durum" = "kaldı" ]; then isaret="⬇ "; gerileme=$((gerileme+1)); fi
    if [ "$onceki" != "geçti" ] && [ -n "$onceki" ] && [ "$durum" = "geçti" ]; then isaret="⬆ "; ilerleme=$((ilerleme+1)); fi
  fi
  case "$durum" in
    geçti) echo "${isaret}✓ $ad" ;;
    sunucu) echo "${isaret}⚠ $ad -- $ozet" ;;
    *) echo "${isaret}✗ $ad -- $ozet" ;;
  esac
  printf '%s\t%s\t%s\n' "$ad" "$durum" "$ozet" >> "$sonuc"
done

echo
echo "geçti: $gecti   kaldı: $kaldi   sunucu: $sunucu   toplam: $((gecti+kaldi+sunucu))"
if [ -n "$BEKLENEN" ]; then
  echo "gerileme: $gerileme   ilerleme: $ilerleme   (beklenen: $BEKLENEN, eşleşen: $eslesen)"
  if [ "$eslesen" -eq 0 ]; then
    echo "HATA: hedeflerin hiçbiri beklenen dosyasında yok -- anahtarlar uyuşmuyor mu?" >&2
    exit 2
  fi
fi

if [ -n "$GUNCELLE" ]; then
  {
    echo "# ornekleri-dogrula.sh sonucu -- $(date -u +%Y-%m-%d) -- $KOCO"
    echo "# geçti: $gecti   kaldı: $kaldi   sunucu: $sunucu"
    printf 'betik\tdurum\tözet\n'
    cat "$sonuc"
  } > "$GUNCELLE"
  echo "yazıldı: $GUNCELLE"
fi

if [ -n "$BEKLENEN" ]; then [ "$gerileme" -eq 0 ]; else [ "$kaldi" -eq 0 ]; fi
