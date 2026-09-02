#!/bin/sh
# Copy the KojoJS runtime sources from this repo (the source of truth) into the
# kojojs-core `page` module, which is a GENERATED copy -- never edit it directly.
#
# Override the destination with KOJOJS_CORE=/path/to/kojojs-core if the repos
# are not checked out side by side.
set -eu

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
