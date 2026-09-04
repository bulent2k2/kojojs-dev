# KojoJS-dev
This is the development repository for the core of KojoJS.

The main goal for the repo - is to enables faster (than the full KojoJS webapp) turnaround for coding/testing/debugging/troubleshooting.

Prerequisites: 
* Install `sbt`, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
* Install `nodejs` and `npm`:
```
sudo apt install nodejs
sudo apt install npm
npm install source-map-support
```

# How to run
```
$ sbt        # sbt 1.x; UTF-8 yereli şart (LANG=C.UTF-8) -- Türkçe adlı sınıflar
> fastLinkJS
```
In another terminal, navigate to the root dir and:
```
./run.sh
```
The `run.sh` script will launch `google-chrome` with `run.html`

## Koco dağıtımı (ikojo.fly.dev)

Türkçe (Koco) sürüm — router + compilerServer + editör tek konteynerde,
nginx önünde — `bulent2k2/koco-deploy` ile paketlenip Fly.io'ya dağıtılıyor.
Tam belge: koco-deploy/README.md. Özet:

```sh
cd <yol>/koco-deploy
git -C ../kojojs-core pull      # güncel 'page' (kojojs-dev'den senkronlanmış runtime)
git -C ../kojojs-dev  pull      # kaynak (isteğe bağlı)

# build.sh İKİ JDK + yamalı derleyici yolu İSTİYOR (yoksa derleme patlar):
export KOCO_JDK_CORE=/path/to/jdk11    # 11/17/21 — kojojs-core Java 9+ ister (readAllBytes)
export KOCO_JDK_EDITOR=/path/to/jdk8   # kojojs-editor sbt 0.13 + Play 2.6 → Java 8
export KOCO_SCALA_TR=/path/to/kojo/scala-tr/build/pack/lib   # yan yana kojo klonu yoksa

./build.sh                     # üç servisi paketler + yamalı jar takası (KOCO_TOOLCHAIN=tr)

# Yerel makinede çalıştır / test et:
docker build -t koco .
docker run --rm -p 7860:7860 --memory 4g koco    # -> http://localhost:7860

# Fly'a dağıt (yerelde kurulan imajı iter):
fly deploy --local-only -a ikojo
```

Not: Bu repo runtime kaynağının DOĞRUSU. Değişiklikten sonra `sync-kojojs-core.sh`
(KOJOJS_CORE_213=1) ile `page`'i kojojs-core'a kopyala; dağıtım oradan paketler.
