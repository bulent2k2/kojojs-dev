#!/bin/bash
# Her örneği gerçek derleyiciye gönderir ve hata dönmediğini doğrular.
# NOT: kabuk değişken adları ASCII olmalı (bash Türkçe karakter kabul etmiyor).
# Örnekler yalnızca yazılımcık GÖVDESİ; buradaki prelude onları sarmalar --
# kojojs-editor'ün application.conf'undaki defaultSource ile aynı olmalı.
set -u
KOCO="${KOCO:-https://ikojo.fly.dev}"
DIR="$(cd "$(dirname "$0")" && pwd)"

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
    implicit val kojoWorld = new KojoWorldImpl()
    val builtins = new Builtins()
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
PRE
  cat "$1"
  echo "}"
}

hata=0
for f in "$DIR"/*.kojo; do
  ad=$(basename "$f")
  govde=$(mktemp); sar "$f" > "$govde"
  # derleyici ısınana kadar birkaç deneme
  for i in 1 2 3 4 5 6 7 8 9 10; do
    kod=$(curl -s -m 300 -X POST --data-binary @"$govde" \
      -H "Content-Type: text/plain; charset=utf-8" \
      "$KOCO/compile?opt=fast&scalaVersion=2.12" -o /tmp/koco-out.bin -w "%{http_code}")
    [ "$kod" = "200" ] && break
    sleep 8
  done
  if [ "$kod" != "200" ]; then
    echo "  ✗ $ad -- HTTP $kod: $( (gzip -dc /tmp/koco-out.bin 2>/dev/null || cat /tmp/koco-out.bin) | head -c 60)"
    hata=1
  else
    ann=$(python3 -c "
import gzip,json,io,sys
try: d=json.loads(gzip.open('/tmp/koco-out.bin','rt',encoding='utf-8').read())
except OSError: d=json.load(io.open('/tmp/koco-out.bin',encoding='utf-8'))
a=d.get('annotations') or []
print(len(a)); sys.stderr.write(str(a)[:300] if a else '')
" 2>/tmp/koco-ann)
    if [ "$ann" = "0" ]; then echo "  ✓ $ad"; else
      echo "  ✗ $ad -- $ann uyarı/hata: $(cat /tmp/koco-ann)"; hata=1
    fi
  fi
  rm -f "$govde"
done
exit $hata
