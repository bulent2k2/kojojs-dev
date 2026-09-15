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

## `sozluk-kapsam.py` — sözlük sayfası ↔ masaüstü üretilmiş sözlüğü

`koco-sozlugu.html`'deki satırlar bir kez üretilip **sonra elle düzeltildi**
(bkz. `../sozluk/README.md`). Bugün onu kaynağa bağlayan bir şey yok: Türkçe
katmana yeni bir ad girince sayfa sessizce eskiyor. Eylül 2026 turu 201 adı
elle işledi (754 → 980) ve o turu kimse tetiklemedi, göz kararı başladı.
Bu araç aradaki farkı sayıyla söylüyor.

Kaynak: masaüstü sözlüğünün **iki yarısı** da —
`ceviri-sozlugu.tsv` (üretilmiş: sarmalayıcılardan türetilip derleyici
sondasından geçmiş çiftler) ve `ceviri-kurallar.tsv` (üretecin yanlış
seçimlerini geçersiz kılan elle kurallar). Yalnız üretilmişi okumak sahte
çelişki üretiyor: `zıpla` sayfada `hop`, üretilmiş TSV'de `saveStyle`, ve kural
dosyası zaten `tr>en zıpla * hop` diyor.

```sh
araclar/sozluk-kapsam.py                    # ../kojo klonunu bekler
araclar/sozluk-kapsam.py --kojo ~/src/kojo
araclar/sozluk-kapsam.py --eksik            # sayfada olmayan adların tam listesi
araclar/sozluk-kapsam.py --celisen          # çelişen çiftlerin tamamı
araclar/sozluk-kapsam.py --ortuk            # sayfanın başka biçimde kapsadıkları
araclar/sozluk-kapsam.py --json /tmp/kapsam.json
```

Üç rapor:

| rapor | bugünkü sayı | ne demek |
|---|---|---|
| sayfada olmayan | 147 ad | sözlükte var, sayfada hiç yok — kaynak dosyaya göre öbeklenmiş |
| örtük kapsanan | 127 (64 niteleme, 63 alt) | sayfa adı başka bir yazım biçimiyle yazmış; kuyruğa girmiyor |
| çelişen çift | 82 (37 ayrı, 6 kural, 24 niteleme, 15 imza) | aynı Türkçe ad, örtüşmeyen İngilizce karşılık |

### Sayfanın üç yazım biçimi

Bir adı sayfada aramak salt hücre karşılaştırması değil. Sayfa (a) üye adlarını
**niteleyerek** yazıyor — üretilmiş TSV `dikdörtgen` diyor, sayfa
`Resim.dikdörtgen`; öğrenci de nitelenmişini yazıyor, yani sayfa haklı.
(b) eşanlamlıyı ayrı satıra değil **nota** koyuyor — `["react","tepkiVer","alt: canlan"]`,
üç sözle: `alt:` (30 not), `eski adı:` (7), `takma ad:` (2). (c) kimi hücreyi
**imzasıyla** yazıyor — `RenkADA(arıRenk, doygunluk, aydınlık)`.

Yalnız hücreye bakan sürüm bu adları eksik sayıyordu: **453 → 404**, yani
küratör turunun altıda biri sahte işmiş (ölçüldü, Eylül 2026).

**Notun biçimi bağlayıcı:** ayrıştırıcı `alt:`ten sonra ilk virgüle, noktalı
virgüle ya da parantez açmaya kadarını AD sayıyor. Yani önce ad, açıklama
parantez içinde: `alt: fareyeTıklıyınca (eski yazım)` çalışır,
`alt: fareyeTıklıyınca — eski yazım, iki tarafta da var` çalışmaz (adın
tamamı "fareyeTıklıyınca — eski yazım" olur ve eşleşme düşer). Bu turda tam
bu tuzağa düşüldü: bir not yeniden yazılınca kuyruk sessizce 147'den 148'e
çıktı. Birden çok takma ad `/` ile ayrılır: `alt: ötele/öteleme`.

Örtük sayılmak için **İngilizce taraf da tutmalı**, yoksa aynı Türkçe sözcüğün
iki ayrı anlamı birbirini kapatırdı: `Görünüş.daire` (bir imge yolu) sayfadaki
`daire`=`circle` ile kapanmıyor, `Resim.sil` (`erasePictures`) sayfadaki
`sil`=`clear` ile kapanmıyor, `arayüz` (`Picture.widget`) `interface`/`arabirim`
satırının "alt: arayüz" notuyla kapanmıyor — üçü de kuyrukta kalıyor. Notun
İngilizcesi satırın okunur hâliyse (`ColorHSB(h, s, b)`) ikinci kanıt satırın
kendi Türkçe adı: sözlük hem `RenkADA`ya hem `RenkArıRenkDoygunlukAydınlık`a
aynı İngilizceyi veriyorsa sayfa "biri ötekinin takma adı" derken haklı.

Çelişki sınıfları, sahte bulguyu ayıklamak için: **ayrı** iki taraf da yalın ad
ama tutmuyor (incelenmesi gereken bunlar); **kural** elle kural sayfayı
doğruluyor, üretilmiş satır ölü veri; **niteleme** yalnız niteleyici farkı
(sayfa `collection.Seq`, sözlük `Seq`); **imza** sayfanın hücresi zaten yalın ad
değil (`scale(x, y)`, `round(n, digits)`, `log base t`) — sayfa yer yer imza ya da
düzyazı yazıyor, tanımlayıcı gibi karşılaştırmak sahte çelişki üretiyor. Ölçüldü:
sınıflandırma olmadan 47 "ayrı" çıkıyordu, 20'si imza 5'i kuralmış.

Sayfayı doğrulamayan kural kuyrukta KALIYOR ama listede görünüyor
(`kalemBoyu … [kural: penThickness]`, sayfa `penWidth`): küratör hikâyeyi bir
bakışta görsün. `[kural: çevirme]` "bilerek çevrilmiyor" demek.

**Bu bir kapı değil, rapor.** Çıkış kodu her zaman 0 (yalnız kojo klonu
bulunamazsa 1). Eksik adların çoğu sayfaya girmemeli — `cizim.scala`'nın
imge yolları, `turler.scala`'nın iç tür takma adları — karar küratörün. Araç
sayfayı da DEĞİŞTİRMİYOR: `koco-sozlugu.html`'e dokunmak artifact'i ve
ikojo'daki kopyayı yeniden yayımlamayı gerektiriyor (`../sozluk/README.md`'deki
beş adım), o ayrı bir tur.

TSV bu depoya **kopyalanmıyor**, kojo klonundan okunuyor: kopyalasak
"üretilmiş dosya kaynağından ayrıldı" sınıfını yeniden açardık, `sozluk-denetle.py`
tam onu kapatmak için var. (`ornek-dizini.py`'nin kojojs-editor dizinini argüman
alması da aynı gerekçe.) Bu yüzden CI'da koşmuyor: kojo klonu ister.

Araç TSV'nin yalnız ilk dört sütununu okuyor (`cins tr en kaynak`), böylece
sözlüğe sütun eklenince kırılmıyor — Eylül 2026'da `sayı` sütunu eklendiğinde
(kojo#65) böyle oldu.

## `adlar.py` — masaüstü ↔ ikojo ad karşılaştırması

`ucurum.py` masaüstü **betiklerini** tarıyor, yani yalnız bir örneğin
*kullandığı* adları görüyor. Hiçbir örneğin kullanmadığı bir eksik ona
görünmez — Eylül 2026'da `Renkler`'deki 24 ad tam böyle kaçtı (#58).
Bu araç tanımları doğrudan karşılaştırıyor.

    araclar/adlar.py                    # ../kojo klonunu bekler
    araclar/adlar.py --kojo ~/src/kojo
    araclar/adlar.py --tsv              # anlık görüntüyü tazele (izlenen dosya)
    araclar/adlar.py --anlik-goruntu    # kojo klonu OLMADAN (CI bunu koşuyor)

**Eşleşme listesi ELLE tutuluyor** (`EŞLEŞMELER`). Otomatik eşleme denendi ve
uydurma boşluk üretti, ölçüldü: iki depo aynı yüzeye farklı kapsayıcı adı
veriyor (`YazıYöntemleri` ↔ `YazıMetotları` → 111 adlık sahte boşluk), ve
gövdesiz bildirimler (`object Matematik extends …`) ayrıştırıcıya sonraki
bloğu yutturuyor (54 ve 132 adlık iki sahte boşluk daha). Her satır bilinçli
bir iddia: "bu iki kapsayıcı aynı yüzey". Yeni çift eklemek ucuz.

**İki ayrı kip, iki ayrı güç:**

| kip | yakaladığı | yakalayamadığı |
|---|---|---|
| `--kojo <klon>` (yerel) | masaüstünde olup ikojo'da olmayan her ad | — |
| `--anlik-goruntu` (CI) | ikojo'nun elindeki bir adı kaybetmesi — ortak olanlar **ve** ikojo'ya özgü olanlar | masaüstünün YENİ ad eklemesi |

CI'ın klonu olmadığı için ikinci kip anlık görüntüye bakıyor
(`araclar/masaustu-adlar.tsv`). Üçüncü sütun bugünkü gerçeği yazar:

| durum | anlamı | CI zorunlu tutuyor mu |
|---|---|---|
| `var` | masaüstünde ve ikojo'da | evet |
| `yalnız-ikojo` | ikojo'nun kendi seçtiği ad (`koyuMor`, `saydam`…) | evet |
| `boşluk` | masaüstünde var, ikojo'da yok | hayır — kapatmak ayrı karar |

`yalnız-ikojo` sonradan eklendi: anlık görüntü başta yalnız masaüstü adlarını
yazıyordu, dolayısıyla ikojo'nun masaüstünden **ayrıldığı** noktalarda seçtiği
adların hiç gözcüsü yoktu (#60 incelemesi ölçtü). Bugünkü boşluklar işi kırmızı
yakmaz ama izlenen bir dosyada göz önünde durur.

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
