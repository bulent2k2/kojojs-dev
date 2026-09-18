#!/bin/sh
# Copy the KojoJS runtime sources from this repo (the source of truth) into the
# kojojs-core `page` module, which is a GENERATED copy -- never edit it directly.
#
# Override the destination with KOJOJS_CORE=/path/to/kojojs-core if the repos
# are not checked out side by side.
set -eu

# --denetle: yalnız paket sınıflandırmasını denetler, senkron YAPMAZ.
# CI bunu koşuyor (bkz. .github/workflows/uretecler.yml). Sebep: asıl senkron
# Faz 3'e kadar kilitli, yani aşağıdaki liste yıllarca hiç koşmayabilir --
# koşmayan bir gözcü hiçbir şey vaat etmiyor.
DENETLE=0
if [ "${1:-}" = "--denetle" ]; then DENETLE=1; fi

HERE=$(cd "$(dirname "$0")" && pwd)

# Çalışma zamanına GİDEN paketler ve bilerek gitmeyenler.
#
# NEDEN AYRI BİR SAV: bu liste ELLE tutuluyor ve yeni bir üst düzey paket
# eklendiğinde sessizce dışarıda kalıyor. Eylül 2026'da tam bu oldu: #65
# `libtessjs` paketini ekledi, `kojo/Ucgenleyici.scala` onu import ediyor ama
# liste güncellenmedi -- senkron koşulduğunda core DERLENMEZDİ ve sebebi aylar
# önceki bir PR olurdu. Aşağıdaki döngü o sınıfı kapatıyor.
PAKETLER="kojo com pixiscalajs howlerscalajs libtessjs"
# driver: yerel gösteri girişi (driver/KojoMain.scala), çalışma zamanının
# parçası değil. Faz 3'te core'un kendi giriş noktasıyla karşılaştırılıp
# doğrulanmalı.
MUAF="driver"

# Kaynak ağacı yoksa glob genişlemez ve döngü sahte bir '*' paketi bildirir
# (ölçüldü). Açık hata daha iyi.
[ -d "$HERE/src/main/scala" ] || {
  echo "error: kaynak ağacı yok: $HERE/src/main/scala" >&2
  exit 2
}

for d in "$HERE"/src/main/scala/*/; do
  paket=$(basename "$d")
  case " $PAKETLER $MUAF " in
    *" $paket "*) ;;
    *)
      echo "error: '$paket' paketi ne kopyalanıyor ne muaf." >&2
      echo "       sync-kojojs-core.sh içindeki PAKETLER ya da MUAF listesine ekleyin." >&2
      exit 1
      ;;
  esac
done

if [ "$DENETLE" = 1 ]; then
  echo "aynı: src/main/scala altındaki paketlerin hepsi sınıflandırılmış ($PAKETLER | muaf: $MUAF)"
  exit 0
fi

# KİLİT KALKTI (Faz 3 tamamlandı). Eskiden burada bir koruma vardı: bu repo
# Scala 2.13.18'e geçtiğinde kaynaklar 2.13'e özgü API kullanmaya başladı
# (distinctBy, LazyList, toIntOption...) ve kojojs-core hâlâ 2.12'deydi, yani
# senkron core build'ini kırardı.
#
# Core artık 2.13: build.sbt'sinde `scalaVersion := "2.13.18"`,
# project/build.properties'te sbt 1.10.11, Scala.js 1.x. Ölçüldü (Eylül 2026,
# kojojs-core 2339b19): kojojs-dev master'ın kaynakları kopyalandıktan sonra
# `page/clean; page/compile` başarılı ve uyarı sayısı senkrondan öncekiyle aynı.
# Korumayı kaldırmak, onu kuran notun kendi koşuluydu.

CORE=${KOJOJS_CORE:-$HERE/../kojojs-core}

if [ ! -d "$CORE/page/src/main/scala" ]; then
  echo "error: no kojojs-core page module at $CORE" >&2
  echo "       set KOJOJS_CORE to the kojojs-core checkout" >&2
  exit 1
fi

DEST=$(cd "$CORE/page/src/main/scala" && pwd)
echo "*** syncing $HERE/src/main/scala -> $DEST"

# rsync --delete so sources dropped upstream also disappear downstream; plain
# `cp` can only add and overwrite, which silently leaves stale files behind.
# --exclude=target: kaynak ağacında sbt'nin bıraktığı `target/` dizinleri
# olabiliyor (src/main/scala/kojo/tr/target/global-logging/*.log -- bu repoda
# .gitignore'lu, ama diskte duruyor). Onlar olmadan kopyalanınca core'a izlenen
# çöp olarak giriyorlardı; elle senkronda tam bu oldu ve beş log dosyası
# temizlendi.
for pkg in $PAKETLER; do
  rsync -a --delete --exclude=target "$HERE/src/main/scala/$pkg/" "$DEST/$pkg/"
  echo "    $pkg"
done

if command -v meld >/dev/null 2>&1; then
  meld "$HERE/src/main/scala/" "$DEST/"
else
  echo "*** review with: git -C $CORE status"
fi
