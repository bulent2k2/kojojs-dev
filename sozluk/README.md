# Koco Sözlüğü

`koco-sozlugu.html` — masaüstü Koco'nun (bulent2k2/kojo) Türkçe↔İngilizce
programlama terim sözlüğü, aranabilir tek dosyalık HTML. İki yönlü, aksan
katlamalı (`cizim` → `çizim`), kategorilere ayrılmış (anahtar kelimeler, türler,
kaplumbağa/tuval komutları, resimler, matematik, koleksiyonlar…). 754 girdi.

## Kaynak
Şu dosyalardan derlendi (bulent2k2/kojo):
- `lite/i18n/tr/dict.scala` — kavram çevirileri
- `lite/i18n/trInit.scala`, `lite/i18n/tr/*.scala` — API adları
- `l10n-level2/level2_tr.properties` — arayüz
- `samples/tr/` — örneklerdeki eşleşmeler

Bu ikojo katmanı (kojojs-dev/src/main/scala/kojo/tr/) için eklenen yeni Türkçe
terimler de sözlüğe işlendi (koyu renkler, Dönüştürücü, renkliYazı, birleştirici
aliaslar, oyun API'leri, bölünüyorMu/belirt/buSaniye, Yığın koy/al/tane…).

Canlı (Claude artifact): sözlük ve dokuz dilli dizin `/yardim`'den de bağlı.
