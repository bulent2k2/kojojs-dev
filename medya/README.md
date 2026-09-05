# Medya (ses ve görüntü)

`bulent2k2/kojo` reposundaki `src/main/resources/media` dizininin değiştirilmemiş
kopyası (`guncelle.sh` ile yenilenir; kaynak commit `KAYNAK.txt`'de). Masaüstü
betikleri bu dosyalara `Ses.vuruş` (`/media/collidium/hit.mp3`), `Görünüş.araba`
(`/media/costumes/car.png`) gibi sabitlerle erişir; sabitler
`src/main/scala/kojo/tr/ses.scala` ve `tr/cizim.scala` içinde.

Sunum: koco-deploy `nginx.conf` `/media/` yolunu `/app/medya` dizinine bağlar
(`build.sh` bu dizini `stage/medya` olarak toplar). Yerel geliştirmede (nginx yok)
`/media/...` istekleri 404 döner; sesler sessizce çalmaz, imgeler yüklenmez.
