# Araçlar

## `ucurum.py` — masaüstü ↔ ikojo uçurum ölçümü

Masaüstü Koco'nun Türkçe betiklerini (`ornekler/masaustu/`) tarar; her betikteki
Türkçe API adını masaüstü tanımlarıyla (`bulent2k2/kojo`: `lite/i18n/trInit.scala`,
`lite/i18n/tr/*.scala`) ve ikojo tanımlarıyla (`src/main/scala/kojo/TurkishTurtle.scala`,
`kojo/tr/*.scala`, İngilizce yüzey `kojo/*.scala`) karşılaştırır. Platform
engellerini (`#yükle`, Swing `ay.*`, ses, öykü, dosya…) işaretler.

Betik başına üç durum:

| durum | anlam |
|---|---|
| `çalışır` | eksik ad yok, platform engeli yok — ikojo'da olduğu gibi derlenmesi beklenir |
| `eksik-ad` | ikojo'da tanımlı olmayan Türkçe ad(lar) kullanıyor; liste TSV'de |
| `platform` | tarayıcıda karşılığı olmayan özellik kullanıyor (engel adı TSV'de) |

```sh
araclar/ucurum.py                                     # ../kojo klonunu bekler
araclar/ucurum.py --kojo ~/src/kojo --en-sik 40
araclar/ucurum.py --tsv ornekler/masaustu/tarama.tsv  # repodaki tarama dosyasını yenile
araclar/ucurum.py --json /tmp/ucurum.json             # tam rapor (eksik adlar, engeller)
```

Bu bir **tanımlayıcı taramasıdır**, derleme değil: imza farklarını (parametre
türü/sayısı) göremez. Gerçek derleme denetimi `ornekler/ornekleri-dogrula.sh`.
İki araç birbirini tamamlar: tarama *neyin* eksik olduğunu söyler, derleme
*gerçekten geçip geçmediğini*.

Plan ve ölçüm belgesi: Koco–ikojo Köprüsü (Claude artifact,
<https://claude.ai/code/artifact/04147d3d-1a10-4b18-a586-d2a105a07764>).

## ornek-dizini.py

Komut -> onu kullanan ÇALIŞAN örnek betik dizini (sozluk/ornekler.json).
kojojs-editor'daki yardım sayfalarının kendi "çalıştır" bağlantılarını
çözerek kuruluyor; zrc dizgeleri olduğu gibi taşınıyor.

    araclar/ornek-dizini.py <kojojs-editor dizini>

## `sozluk-renk-denetle.py` — sözlükteki renk satırları koddakiyle aynı mı

`koco-sozlugu.html`'deki `key:"val"` tablosu elle tutulan HTML. Kodda bir renk
adı eklenir/değişir ve tabloya yansıtılmazsa hiçbir şey hata vermez; sayfa
sessizce eksik veri gösterir. Eylül 2026'da tam bu oldu: koddaki 39 renk adına
karşılık sözlükte 6 satır vardı (#58).

    araclar/sozluk-renk-denetle.py     # fark varsa 1 döner

Karşılaştırma **İngilizce ad** üzerinden: bir sözlük satırı, İngilizce adı
doodle paletinde (`CommonColors.scala`) geçiyorsa renk satırı sayılıyor —
"Türkçesi renge benziyor mu" diye tahmin edilmiyor. Üç yönü de yakalar: kodda
olup sözlükte olmayan, sözlükte olup kodda olmayan, Türkçesi tutmayan.

`uretecler.yml`'de bir adım olarak koşuyor.

## gosteri-uret.py

yardimKomutlar sayfasındaki kısa gösterilerin KAYNAĞI (G tablosu).

    araclar/gosteri-uret.py --scala          # derleme testini üret
    araclar/gosteri-uret.py --html <editor>  # sayfaya yapıştırılacak <tr> satırları

Gösteriler `src/test/scala/kojo/OrnekDerlemeDeneme.scala` üzerinden ikojo
API'sine karşı DERLENEREK sınanıyor: bir komut adı ya da imzası değişirse
`sbt Test/compile` kırılır, sayfaya bozuk örnek girmez.

Sıra: gösteriyi G'ye ekle -> --scala + sbt Test/compile -> --html ile satırı
al, sayfaya yapıştır -> ornek-dizini.py ile sözlüğü tazele.
