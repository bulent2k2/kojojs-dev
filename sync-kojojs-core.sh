#!/bin/sh
# Copy the KojoJS runtime sources from this repo (the source of truth) into the
# kojojs-core `page` module, which is a GENERATED copy -- never edit it directly.
#
# Override the destination with KOJOJS_CORE=/path/to/kojojs-core if the repos
# are not checked out side by side.
set -eu

# KİLİTLİ ADIM (Faz 2, bkz. oneri-scala-2.13.md): bu repo artık Scala 2.13.18;
# kaynaklar 2.13'e özgü API kullanıyor (distinctBy, LazyList, toIntOption...).
# kojojs-core'un build'i 2.13'e geçene (Faz 3) kadar senkron ONU KIRAR.
# Core hazır olduğunda KOJOJS_CORE_213=1 ile çalıştırıp bu korumayı kaldırın.
if [ "${KOJOJS_CORE_213:-}" != "1" ]; then
  echo "error: kojojs-core henüz Scala 2.13'te değil; senkron core build'ini kırar." >&2
  echo "       Faz 3 tamamlanınca: KOJOJS_CORE_213=1 $0" >&2
  exit 1
fi

HERE=$(cd "$(dirname "$0")" && pwd)
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
for pkg in kojo com pixiscalajs howlerscalajs; do
  rsync -a --delete "$HERE/src/main/scala/$pkg/" "$DEST/$pkg/"
  echo "    $pkg"
done

if command -v meld >/dev/null 2>&1; then
  meld "$HERE/src/main/scala/" "$DEST/"
else
  echo "*** review with: git -C $CORE status"
fi
