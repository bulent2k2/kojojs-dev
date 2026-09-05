# Masaüstü Koco betikleri

`bulent2k2/kojo` reposundaki Türkçe `.kojo` betiklerinin **değiştirilmemiş**
kopyası; kojo'daki göreli yollarıyla durur (`src/main/resources/samples/tr/…`,
`installer/examples/othello/tr/…`). Kaynak commit `KAYNAK.txt`'de.

Amaç: bu betiklerin hepsinin ikojo'da da çalışması. Ne kadarının çalıştığını iki
dosya söyler:

- `tarama.tsv` — `araclar/ucurum.py` çıktısı: betik başına `çalışır` / `eksik-ad` /
  `platform`, eksik adlar ve engeller. Statik tarama; her API değişikliğinde yenilenir.
- `derleme.tsv` — `../ornekleri-dogrula.sh -g masaustu/derleme.tsv masaustu`
  çıktısı: gerçek `/compile` sonucu, betik başına `geçti` / `kaldı` / `sunucu`
  (HTTP 200 dönmedi; betiğin değil sunucunun sorunu, gerileme sayılmaz). **Henüz
  üretilmedi**; ilk kez canlı ya da yerel sunucuya karşı koşulup repoya alınmalı.
  Sonraki koşular `-b masaustu/derleme.tsv` ile karşılaştırılır: gerileme varsa
  çıkış kodu 1, ilerleme varsa ⬆ ile yazılır.

Güncelleme (kojo klonundan yeniden kopyalar, `KAYNAK.txt`'yi yazar):

```sh
KOJO=~/src/kojo ornekler/masaustu/guncelle.sh
araclar/ucurum.py --kojo ~/src/kojo --tsv ornekler/masaustu/tarama.tsv
```

Buradaki dosyaları elle düzenlemeyin; düzeltme masaüstü kojo'ya gider, sonra
`guncelle.sh` ile buraya iner. `#yükle` satırları bu dizine göre çözülür
(`// #yükle /samples/tr/oyku-tanimlari` → `src/main/resources/samples/tr/oyku-tanimlari.kojo`);
ikojo bunu henüz desteklemiyor, bu betikler `platform` durumundadır.
