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
  local f="$1"
  case "$f" in
    "$DIR"/*) echo "${f#"$DIR"/}" ;;
    *) echo "$f" ;;
  esac
}

# Beklenen durumları oku (betik<TAB>durum ...)
declare -A beklenen
if [ -n "$BEKLENEN" ]; then
  [ -f "$BEKLENEN" ] || { echo "beklenen dosyası yok: $BEKLENEN" >&2; exit 2; }
  while IFS=$'\t' read -r b d _; do
    case "$b" in ''|'#'*|betik) continue ;; esac
    beklenen["$b"]="$d"
  done < "$BEKLENEN"
fi

cikti=$(mktemp); ann_dosya=$(mktemp); sonuc=$(mktemp)
trap 'rm -f "$cikti" "$ann_dosya" "$sonuc"' EXIT

gecti=0; kaldi=0; gerileme=0; ilerleme=0
for f in "${hedefler[@]}"; do
  ad=$(ad_ver "$f")
  govde=$(mktemp); sar "$f" > "$govde"
  kod=""
  # derleyici ısınana kadar birkaç deneme
  for i in 1 2 3 4 5 6 7 8 9 10; do
    kod=$(curl -s -m 300 -X POST --data-binary @"$govde" \
      -H "Content-Type: text/plain; charset=utf-8" \
      "$KOCO/compile?opt=fast" -o "$cikti" -w "%{http_code}")
    [ "$kod" = "200" ] && break
    sleep 8
  done
  rm -f "$govde"
  durum="kaldı"; ozet=""
  if [ "$kod" != "200" ]; then
    ozet="HTTP $kod: $( (gzip -dc "$cikti" 2>/dev/null || cat "$cikti") | head -c 80 | tr '\n\t' '  ')"
  else
    ann=$(python3 -c "
import gzip,json,io,sys
try: d=json.loads(gzip.open(sys.argv[1],'rt',encoding='utf-8').read())
except OSError: d=json.load(io.open(sys.argv[1],encoding='utf-8'))
a=[x for x in (d.get('annotations') or []) if x.get('severity','error')!='warning'] if isinstance(d,dict) else []
print(len(a))
if a:
    x=a[0]; sys.stderr.write((str(x.get('row', x.get('line','?')))+': '+str(x.get('text', x.get('message', x)))).replace('\n',' ')[:160])
" "$cikti" 2>"$ann_dosya")
    if [ "$ann" = "0" ]; then durum="geçti"; else ozet="$ann hata: $(cat "$ann_dosya")"; fi
  fi
  if [ "$durum" = "geçti" ]; then gecti=$((gecti+1)); else kaldi=$((kaldi+1)); fi
  isaret="  "
  if [ -n "$BEKLENEN" ]; then
    onceki="${beklenen[$ad]:-}"
    if [ "$onceki" = "geçti" ] && [ "$durum" = "kaldı" ]; then isaret="⬇ "; gerileme=$((gerileme+1)); fi
    if [ "$onceki" = "kaldı" ] && [ "$durum" = "geçti" ]; then isaret="⬆ "; ilerleme=$((ilerleme+1)); fi
  fi
  if [ "$durum" = "geçti" ]; then echo "${isaret}✓ $ad"; else echo "${isaret}✗ $ad -- $ozet"; fi
  printf '%s\t%s\t%s\n' "$ad" "$durum" "$ozet" >> "$sonuc"
done

echo
echo "geçti: $gecti   kaldı: $kaldi   toplam: $((gecti+kaldi))"
[ -n "$BEKLENEN" ] && echo "gerileme: $gerileme   ilerleme: $ilerleme   (beklenen: $BEKLENEN)"

if [ -n "$GUNCELLE" ]; then
  {
    echo "# ornekleri-dogrula.sh sonucu -- $(date -u +%Y-%m-%d) -- $KOCO"
    echo "# geçti: $gecti   kaldı: $kaldi"
    printf 'betik\tdurum\tözet\n'
    cat "$sonuc"
  } > "$GUNCELLE"
  echo "yazıldı: $GUNCELLE"
fi

if [ -n "$BEKLENEN" ]; then [ "$gerileme" -eq 0 ]; else [ "$kaldi" -eq 0 ]; fi
