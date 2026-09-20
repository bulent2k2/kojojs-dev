# Koco örnekleri

Türkçe Kojo (Koco) ile yazılmış örnek programlar. Her dosya doğrudan
**https://ikojo.fly.dev** adresine yapıştırılıp çalıştırılabilir.

Örnekler Türkçe anahtar kelimeleri (`dez`, `den`, `eğer`, `için`…) kullanır;
bunları yamalı (scala-tr) derleyici tanır. ikojo.fly.dev bu derleyiciyi
çalıştırır. Stok bir Scala derleyicisi bu dosyaları derleyemez.

| Dosya | Ne öğretiyor |
|---|---|
| `01-ilk-adimlar.kojo` | `ileri`, `sağ`, `yinele`, `ev` — kaplumbağanın temeli |
| `02-renkli-cicek.kojo` | `yineleDizinli` ile sayaç, renk listesinde dolaşmak |
| `03-resimler.kojo` | `Resim`, zincirlenebilir dönüşümler (`boyalı`, `döndürülmüş`) |
| `04-klavye-oyunu.kojo` | `tuşBasılıMı`, `canlandır` — ok tuşlarıyla kontrol |
| `05-sekme-oyunu.kojo` | `Yöney2B` ile hız, `sahnedenSek` ile çarpma |
| `06-koleksiyonlar.kojo` | `Dizi`, `Küme`, `Eşlek`, `MiskinDizin` |
| `07-nokta-ve-yoney.kojo` | `Nokta`, `Dikdörtgen`, `Yöney2B` |
| `08-kumanda-kolu.kojo` | `kumandaKolu`, `oynatSahneİçinde` — fareyle sürülen top |
| `09-nerede-ve-dokunma.kojo` | `konumuOku`, `yönüOku`, `dokunuyorMu` — kuyruktan okuma |
| `10-anahtar-kelimeler.kojo` | Türkçe **anahtar kelimeleri** bir arada gösterir: `tanım`, `dez`, `den`, `eğer`/`yoksa`, `için`, `eşle`/`durum` |
| `11-acilar-ve-radyan.kojo` | Radyan nedir — adım adım devinimli anlatım. Masaüstündeki `samples/tr/angles.kojo`'nun tarayıcı sürümü: geçişler `durakla` yerine bir **düğmeye** bağlı (bkz. aşağıdaki not) |
| `12-uc-cisim.kojo` | Yerçekimi benzetimi — Newton mekaniğiyle üç gökcisminin birbirini çekmesi |
| `13-xox-yenilmez.kojo` | **minimax** ve **alfa-beta budaması** — yenilmeyen bir oyun stratejisi nasıl programlanır |
| `14-agir-dolgu.kojo` | Kendini kesen şekillerin dolgusu neden yavaşlar — ölçülmüş maliyet eğrisi, ve dolgu bir karelik bütçeyi aşınca çıkan not (bkz. aşağıdaki not) |
| `15-mesh-olcumu.kojo` | **Örnek değil, ölçü aleti**: kesişen bir şekli her karede yeniden çizen döngünün saniyede kaç kare verdiğini sayar (bkz. aşağıdaki not) |

## Nasıl çalıştırılır

1. https://ikojo.fly.dev adresini aç
2. Düzenleyicideki her şeyi sil, dosyanın içeriğini yapıştır
3. **Çalıştır**

İlk derleme makine yeni başladıysa 10-15 saniye sürebilir; sonrakiler ~1 saniye.

## `durakla` masaüstündeki gibi çalışmaz

Masaüstünde `durakla(n)` iş parçacığını uyutur, yani ondan sonraki **her şey**
bekler. ikojo'da tarayıcıyı bloklamak yok: `durakla` yalnız **kaplumbağa komut
kuyruğuna** bir bekleme ekler. `çiz` / `.sil()` / `.döndür()` gibi **resim**
çağrıları kuyruğa girmez, hemen çalışır.

Sonuç: baştan sona resimle çizen bir masaüstü betiği burada **derlenir ama
yanlış çizer** — bütün adımlar tek karede olup biter, ekranda yalnız son hâl
kalır. Hiçbir hata iletisi çıkmaz.

Tarayıcıda doğru olan iki yol:

* **kullanıcının ilerlettiği adımlar** → bir resmi düğme yapıp
  `fareyeTıklayınca` ile bağlamak (örnek: `11-acilar-ve-radyan.kojo`)
* **sürekli devinim** → `canlandır { ... }`, her karede bir çalışır

## Hız hakkında

`hızıKur(çokHızlı)` animasyonu tamamen kapatır ve çizim anında biter.
Varsayılan hız yavaştır (adım başına ~1 saniye), bu yüzden çok şekil çizen
programlarda `çokHızlı` kullanmak gerekir. Ara değerler: `yavaş`, `orta`,
`hızlı`.

## Ağır dolgu hakkında

Kendini kesen bir yolun içini boyamak için şekil üçgenlere ayrılıyor (NON_ZERO
sarım kuralı — masaüstü Kojo'nun Java ile yaptığının aynısı). Bu hesap nokta
sayısıyla **karesele yakın** büyüyor; ölçüldü (kojojs-dev#68):

| nokta (7 kat sarılı) | süre (ısıtılmış, yazılımsal çizici) |
|---|---|
| 250 | ~8 ms |
| 1000 | ~95 ms |
| 2000 | ~440 ms |
| 4000 | ~1840 ms |

**Bu tablo iyimser.** Sayılar aynı girdiyle tekrarlanan çağrıların ortancası
(JIT ısınmış) ve SwiftShader üstünde alındı. Tek atışlık gerçek bir betikte
libtess **soğuk** koşuyor: gerçek bir tarayıcıda 250 nokta ölçeğinde **27-44 ms**
görüldü (kojojs-dev#125), yani tablodakinin birkaç katı. Tabloyu **sıralama**
için okuyun (büyüdükçe kötüleşiyor), mutlak eşik için değil.

Kesişmeyen bir yolda aynı nokta sayısı bedavaya yakın: 4000 noktalı bir çemberin
dolgusu 6 ms'den az. Yani pahalı olan nokta sayısı değil, **kesişmeyle birlikte**
nokta sayısı.

Bir dolgu hesabı bir karelik bütçeyi (~17 ms) aşarsa ikojo çıktı paneline bir
not düşer: ne kadar sürdüğünü, kaç nokta olduğunu ve ne yapılabileceğini yazar.
Davranış değişmiyor — şekil yine çiziliyor; değişen şey, yavaşlığın artık
**sessiz olmaması**. `14-agir-dolgu.kojo` bunu adım adım gösteriyor.

Not **şekil başına en çok bir kez** düşer ve o şeklin **toplam** dolgu süresini
söyler. Bunun sebebi ölçülmüş: bir şekil bitmeden birkaç kez yayınlanıyor
(kaplumbağa komutları kuyrukta işleniyor), ve önceki sürüm her yayını ayrı ayrı
bildirdiği için betikte olmayan nokta sayıları yazıyordu — 250 noktalık bir gül
için "146 nokta" (kojojs-dev#125). Şekil henüz bitmemişken düşen not bunu
açıkça söyler: *"şimdilik N nokta; şekil büyüdükçe artacak"*.

## `15-mesh-olcumu.kojo` bir ölçü aleti

Öteki dosyalar öğretmek için; bu dosya bir **değişikliğin öncesi ve sonrası
aynı şeyle ölçülsün** diye var (kojojs-dev#125). Kesişen bir şekli her karede
yeniden çizer ve saniyede kaç kare düştüğünü yazar.

Ölçtüğü şey şu: bugün dolgu üçgenlere ayrılıp PIXI'ye **üçgen başına bir
`drawPolygon`** ile veriliyor (250 noktalı gülde yayın başına 2 998 çağrı).
#125 bunun yerine tek bir mesh vermeyi tartışıyor. Betik o değişikliği
**yapamaz** — hangi PIXI nesnesinin kullanıldığı kitaplığın içinde; betiğin işi
yalnız kareyi saymak.

Düzeneğin üç kuralı, #68'de üç kez yanlış ölçülmüş olmasından geliyor:

* **tek kaplumbağa**, `sil()` ile yeniden kullanılır — her karede yeni resim
  yaratıp bırakmak sahneyi biriktirir ve senaryoyu değil sahneyi ölçtürür
  (298 ms/kare diye okunan sayı buydu, gerçeği ~20 ms'ydi)
* **ısınma kareleri sayılmaz** — ısıtılmamış çizici ilk kareleri şişiriyor
* **tek sayı değil, birkaç saniyelik dizi** okunur — ölçüm koşudan koşuya %25
  oynuyor
* **tamamlanmış gül** sayılır, canlandırma tiki değil — ilk sürüm tiki sayıyordu
  ve yanlıştı (komutlar kuyrukta, bir gül birkaç kareye yayılıyor); şimdi her
  gülün ardındaki `konumuOku` geri çağrımı, kuyruk oraya varınca sayıyor

Rapordaki her satır **tam bir saniyeyi** kapsar: saniye tabanı ısınma bitince
alınıyor ve ilk (kısmi) saniye basılmadan atılıyor. Bu olmadan ilk satır
sistematik olarak eksik bir saniye sayıyordu — 30 kare/s'lik bir koşuda
"29-31" yerine "1-31" okunurdu (kojojs-dev#127 incelemesi).

Sınırı: `BuAn()` saniyeden ince ölçmüyor, yani çıkan sayı kare *süresi* değil
saniyedeki kare *sayısı*. Kare süresinin dağılımı için tarayıcının
profilleyicisi gerekir.

## Bu örnekler test ediliyor

`ornekleri-dogrula.sh` her dosyayı gerçek derleyiciye gönderip hata dönmediğini
kontrol eder — yani bozuk bir örnek fark edilmeden kalmaz:

```sh
./ornekleri-dogrula.sh                              # canlı sunucuya karşı, bu dizindeki örneklerin hepsi
KOCO=http://localhost:7860 ./ornekleri-dogrula.sh   # yerel konteynere karşı
./ornekleri-dogrula.sh masaustu                     # masaüstü betikleri (özyineli)
./ornekleri-dogrula.sh -g masaustu/derleme.tsv masaustu   # sonucu TSV'ye yaz
./ornekleri-dogrula.sh -b masaustu/derleme.tsv masaustu   # öncekiyle karşılaştır; yalnız gerileme hata
```

Dosya ya da dizin verilebilir; `-b` olmadan her kaldı çıkış kodu 1'dir (buradaki
örneklerin hepsi geçmeli). `-b` ile beklenen durum dosyasına göre yalnız gerileme
(geçti → kaldı) hata sayılır; ilerleme ⬆ ile işaretlenir.

## Masaüstü betikleri (`masaustu/`)

`bulent2k2/kojo` reposundaki 112 Türkçe betiğin değiştirilmemiş kopyası ve
uçurum ölçümü (`tarama.tsv`). Ayrıntı: `masaustu/README.md`, ölçüm aracı
`../araclar/ucurum.py`.
