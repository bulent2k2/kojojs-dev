# Koco Kılavuzu (ikojo çevrim içi yardım)

Masaüstü Koco'nun iki Türkçe **öykü** betiği (Story/Page tabanlı; tarayıcıda
çalışmaz) burada Markdown'a taşındı ve ikojo'nun yardım sayfaları olarak
üretiliyor:

| Kaynak (bulent2k2/kojo) | Burada | ikojo'da |
|---|---|---|
| `src/main/resources/samples/tr/scala-tutorial.kojo` (19 sayfa) | `skala/NN-baslik.md` | <https://ikojo.fly.dev/yardim/skala> |
| `src/main/resources/samples/tr/kojo-documentation.kojo` + `#yükle` ile aldığı `oyku-tanimlari.kojo`, `kojo-kilavuz/{ornek-yazilimlar,komutlar-genel,ek-sayfa,turler}.kojo` (8 bölüm) | `komutlar/NN-baslik.md` | <https://ikojo.fly.dev/yardim/komutlar> |

İçerik çevrilmedi (zaten Türkçe); yapı öyküden kılavuza taşındı: öykünün
"sonraki sayfa / tıkla" yönlendirmeleri sayfa içi bağlantı oldu, "sağ paneldeki
düzenleyici" gibi masaüstüne özgü ifadeler ikojo'ya uyarlandı. Örnekler ikojo'da
tek başına çalışsın diye iki uyarlama daha yapıldı (kılavuzun girişinde de
yazılı): yalın deyişler `satıryaz(...)` içine alındı (ikojo bir deyişin
değerini kendiliğinden göstermez), art arda gelen ve birbirine dayanan
örneklerin tanımları her blokta yinelendi (ikojo her çalıştırmada temiz
başlar).

## Üretim

```sh
python3 kilavuz/uret.py                       # kilavuz/html/{skala,komutlar}.html
python3 kilavuz/uret.py --twirl ../kojojs-editor/server/src/main/twirl/views
                                              # + yardimSkala/yardimKomutlar.scala.html
python3 kilavuz/uret.py --kojo ~/src/kojo     # masaüstü ad listesini bu klondan yenile
```

Harici kütüphane yok; python3 standart kütüphanesi yeter. Çıktılar:

- `html/skala.html`, `html/komutlar.html` — tek başına açılan saf HTML (denetim için).
- `--twirl` verilen dizine `yardimSkala.scala.html`, `yardimKomutlar.scala.html`
  — aynı içerik, Twirl şablonu olarak (`@()` imzası + içerikteki her `@` → `@@`).
  **Bu iki şablonu elle düzenlemeyin**; Markdown'ı düzeltip yeniden üretin.
- `eksik-adlar.tsv` — örneklerde geçen ama ikojo'da olmayan masaüstü adları.
- `masaustu-adlar.txt` — masaüstü Koco TR API'sinin ad listesi (kojo klonundan
  otomatik çıkarılır; klon yoksa bu önbellek kullanılır).

"Editörde aç" bağlantısı `<taban>/?zrc=<gzip+base64url kaynak>` biçiminde;
editörün `decodeSource`'u ile birebir (base64Url → GZIPInputStream). URL 2048
karakteri aşarsa bağlantı yerine **Kopyala** düğmesi üretilir.

## Örnek listesi sayfası (`ornekler.py`)

`kilavuz/ornekler.py` ayrı bir üreteç: `/yardim/ornekler` sayfasını, yani
masaüstü Koco'nun Örnekler + Sergi menülerinden ve ikojo'nun kendi
örneklerinden derlenen betik listesini üretir. Her satır `/ornek/<yol>`
bağlantısı ve bir durum rozeti taşır.

```sh
python3 kilavuz/ornekler.py                    # kilavuz/html/ornekler.html
python3 kilavuz/ornekler.py --twirl ../kojojs-editor/server/src/main/twirl/views
                                               # + yardimOrnekler.scala.html
python3 kilavuz/ornekler.py --kojo ~/src/kojo  # masaüstü klonunu göster
```

Okuduğu kaynaklar:

| Kaynak | Ne için |
|---|---|
| kojo klonu `lite/AppMenu.scala` | Örnekler ve Sergi menülerinin sırası/grupları |
| kojo klonu `Bundle_tr.properties` | menü anahtarlarının Türkçe başlıkları |
| `ornekler/README.md` | ikojo'nun kendi örnekleri (dosya + açıklama) |
| `ornekler/masaustu/derleme.tsv` | **varsa yeğlenir**: gerçek derleme sonucu |
| `ornekler/masaustu/tarama.tsv` | yedek: `araclar/ucurum.py` ad taraması |

Rozetler iki kaynağı **birleştirir**: durum `derleme.tsv`'den gelir, ama bir
betik derlemede `kaldı` ve taramada `platform` ise "masaüstü" rozeti ve engel
adı (Swing arayüzü, öykü anlatıcı, MIDI…) korunur — orada eksik olan komut
değil platformdur. `kaldı` satırlarında derleyicinin hata özeti rozetin
`title`'ında görünür.

`--twirl` çıktısı `uret.py`nin şablonlarıyla aynı kurallara uyar (elle
düzenlemeyin, yeniden üretin) ve Twirl'ün 65535 baytlık metot sınırına
takılmamak için sayfa parçalara bölünür.

> `menuler()` masaüstü `AppMenu.scala` içindeki sabit satırlara
> (`menuBar.add(samplesMenu)` gibi) dayanır: kojo klonu **master**ta güncel
> olmalı; upstream orayı değiştirirse üreteç sessizce bozulmak yerine
> `ValueError` ile durur ve buradaki desen güncellenir.

## Markdown alt kümesi

`# Başlık` (bölüm adı; dosya başında, bir kez), `##`/`###`/`####` alt başlıklar,
paragraflar, `- ` listeler, `> ` not kutusu, `` `kod` ``, `**kalın**`, `*eğik*`,
`[metin](adres)`, GFM boru tabloları (hücre içindeki `|` için `\|`) ve
```` ```scala ```` kod blokları. Kılavuzlar arası bağlantı için yer tutucu:
`[…]({komutlar}#b02)`, `[…]({skala}#b05)` — üretimde ortama göre çözülür
(`komutlar.html#b02` ya da `/yardim/komutlar#b02`). Bölüm kimlikleri dosya
numarasından gelir: `03-anahtar-sozcukler.md` → `#b03`.

Dosya başına `<!-- hücreler: çalıştır -->` konursa, tablo hücresi yalnız bir
kod parçasından oluşuyorsa (`` `ileri(100)` `` gibi) o hücre "editörde aç"
bağlantısı olur (masaüstü öyküsündeki tıklanabilir gözlerin karşılığı).

## Masaüstü işareti kuralı

Kod bloğunun **kapanış ```` ``` ```` satırının hemen ardındaki** satır:

```
<!-- masaüstü -->                        rozeti zorla
<!-- masaüstü: tuvalAlanı→tuvalSınırları, durakla -->  rozet + karşılık bilgisi
<!-- ikojo -->                           otomatik bulguyu sustur (yanlış alarm)
<!-- ikojo: en, boy -->                  yalnız bu adları sustur
```

İşaret olmasa da `uret.py` her bloğu tarar: bir ad masaüstü TR API'sinde var,
ikojo'da (`kojo/TurkishTurtle.scala`, `kojo/tr/*.scala`, İngilizce yüzey) yok ve
blokta yerel olarak tanımlanmamışsa bloğun altına turuncu **masaüstü** rozeti ve
"ikojo'da yok: ad → karşılık" satırı düşer. Bilinen karşılıklar `uret.py`
içindeki `KARSILIK` sözlüğünde (`tuvalAlanı → tuvalSınırları`,
`silipSakla → silVeSakla`, `yaklaş → yaklaşXY`, `durdur → canlandırmayıDurdur`…).
Kodun kendisi masaüstündeki gibi bırakılır; okur ikojo'da denerken karşılığı
kullanır. Farkların özeti: <https://ikojo.fly.dev/yardim/farklar>.

## "Editörde aç" bağlantıları

`/?zrc=` bağlantısı gövdeyi değil, editörün beklediği **tam ScalaFiddle kaynağını**
(`object ScalaFiddle { ... }` + `$FiddleStart`/`$FiddleEnd` işaretleri) taşır;
istemci yalnız gövdeyi gösterir. `uret.py` içindeki `SARMAL_BAS`/`SARMAL_SON`
şablonu kojojs-editor `application.conf` `scalafiddle.defaultSource` ile aynı
olmalı; prelude değişirse ikisi birden güncellenir (bkz. `ornekler/ornekleri-dogrula.sh`
`sar()`, aynı şablon). Yalın gövde gönderilirse derleyici "expected class or object
definition" verir.
