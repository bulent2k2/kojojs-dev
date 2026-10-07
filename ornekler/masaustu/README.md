# Masaüstü Koco betikleri

`bulent2k2/kojo` reposundaki Türkçe `.kojo` betiklerinin **değiştirilmemiş**
kopyası; kojo'daki göreli yollarıyla durur (`src/main/resources/samples/tr/…`,
`installer/examples/othello/tr/…`). Kaynak commit `KAYNAK.txt`'de.

Amaç: bu betiklerin hepsinin iKojo'da da çalışması. Ne kadarının çalıştığını iki
dosya söyler:

- `tarama.tsv` — `araclar/ucurum.py` çıktısı: betik başına `çalışır` / `eksik-ad` /
  `platform`, eksik adlar ve engeller. Statik tarama; her API değişikliğinde yenilenir.
- `derleme.tsv` — `../ornekleri-dogrula.sh -g masaustu/derleme.tsv masaustu`
  çıktısı: gerçek `/compile` sonucu, betik başına `geçti` / `kaldı` / `sunucu`
  (HTTP 200 dönmedi; betiğin değil sunucunun sorunu, gerileme sayılmaz).
  Sonraki koşular `-b masaustu/derleme.tsv` ile karşılaştırılır: gerileme varsa
  çıkış kodu 1, ilerleme varsa ⬆ ile yazılır. Sunucu **yamalı (Türkçe anahtar
  sözcüklü) derleyiciyle** koşmalı ve router'ın `KOCO_ORNEKLER`'i bu `ornekler/`
  dizinine bakmalı: `#yükle` satırlarını router genişletiyor; yoksa bölünmüş
  örnekler (robosim, Othello…) içe alınamadan "bulunamadı" ile kalır. Başlık
  satırı koşunun tarihini ve sunucuyu yazar.

Güncelleme (kojo klonundan yeniden kopyalar, `KAYNAK.txt`'yi yazar):

```sh
KOJO=~/src/kojo ornekler/masaustu/guncelle.sh
araclar/ucurum.py --kojo ~/src/kojo --tsv ornekler/masaustu/tarama.tsv
```

Buradaki dosyaları elle düzenlemeyin; düzeltme masaüstü kojo'ya gider, sonra
`guncelle.sh` ile buraya iner. `#yükle` satırları bu dizine göre çözülür
(`// #yükle /samples/tr/oyku-tanimlari` → `src/main/resources/samples/tr/oyku-tanimlari.kojo`);
iKojo bunu henüz desteklemiyor, bu betikler `platform` durumundadır.
