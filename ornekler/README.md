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
| `14-agir-dolgu.kojo` | Kendini kesen şekillerin dolgusu neden yavaş**tı** ve artık neden değil — **gerileme gösterimi**: iki gül, panel sessiz kalmalı; eski yolun ölçülmüş maliyet eğrisi ve notu konsol anahtarıyla (`localStorage.kojoDolgu`, bkz. aşağıdaki not) |
| `15-mesh-olcumu.kojo` | **Örnek değil, ölçü aleti**: kesişen bir şekli her karede yeniden çizen döngünün saniyede kaç kare verdiğini sayar (bkz. aşağıdaki not) |
| `16-yuz-bin-komut.kojo` | **Ölçü aleti**: 100 000 kaplumbağa komutunu tek bir dolgulu şekil olarak çizer ve süresini yazar — komut pompasının hızı, dolguyla (bkz. aşağıdaki not) |

## Nasıl çalıştırılır

1. https://ikojo.fly.dev adresini aç
2. Düzenleyicideki her şeyi sil, dosyanın içeriğini yapıştır
3. **Çalıştır**

İlk derleme makine yeni başladıysa 10-15 saniye sürebilir; sonrakiler ~1 saniye.

## `durakla` masaüstündeki gibi çalışmaz

Masaüstünde `durakla(n)` iş parçacığını uyutur, yani ondan sonraki **her şey**
bekler. iKojo'da tarayıcıyı bloklamak yok: `durakla` yalnız **kaplumbağa komut
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

**Bugün (kojojs-dev#147):** 64 noktadan büyük dolgular **hiç üçgenlenmiyor**.
Şekil ekran kartına bir yelpaze olarak veriliyor, sarım sayısı stencil
tamponunda sayılıyor, sıfır olmayan pikseller boyanıyor (OpenGL'in klasik
NON_ZERO dolgusu). Bedel nokta sayısıyla doğrusal ve ekran kartında: 250×7
gülde 0.1–0.4 ms, 1000×7'de 0.3–0.8 ms — eski yolun 100–400'de biri
(kojojs-dev#152, SwiftShader; gerçek donanımda oran küçülür, sınıfı değişmez).
Sonuç piksel piksel aynı: silüet kenarında %0.06, ters sarımlı delikler dâhil.
Bu yolda üçgenleme yok, dolayısıyla aşağıda anlatılan **not da yok**;
`14-agir-dolgu.kojo` artık bir gerileme gösterimi — çalıştırınca panel sessiz
kalmalı.

**64 neden:** küçük dolgular PIXI'nin tek partisinde gidiyor, stencil ise şekil
başına iki çizim çağrısı; 2000 küçük kareyle ölçüldü, stencil orada kaybediyor
(20 ms'ye 0.8 ms). 64 ve altında libtess'in en kötü bedeli 4 ms. Sınır
kitaplıkta (`StencilDolgu.Eşik`).

**Eski yol duruyor** ve üç durumda çalışıyor: 64 ve altı nokta, PIXI 4 / stencil
tamponu vermeyen bir bağlam, ve elle istenince: tarayıcı konsolunda
`localStorage.kojoDolgu = "libtess"` (geri almak için `delete
localStorage.kojoDolgu`; tuval editörün aynı-kökenli çerçevesinde koştuğu
için depo ortak). Adrese `?dolgu=libtess` eklemek de okunuyor ama editörde
güvenilmez: yönlendirici sorguyu kök sayfaya çevirirken düşürüyor (canlıda
görüldü, kojojs-dev#147). Aşağıdaki her şey — maliyet eğrisi, soğuk/sıcak, not
makinesi — **o yolu** anlatıyor; sayılar tarihsel değil, anahtarla bugün de
alınabilir.

### Eski yol: libtess

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
libtess **soğuk** koşuyor. Gerçek bir tarayıcıda 250×7 ölçeğinde üç ölçüm
(kojojs-dev#130):

| nokta | süre |
|---|---|
| 251 | 22 / 32 / 35 ms (üç koşu, elle tamamlama satırıyla) |
| 251 | 18 ms (tek koşu, satırsız — kojojs-dev#134) |
| 251 | 21 ms (tek koşu, satırsız, yeni komut pompası — kojojs-dev#131) |
| 251 | 23 / 31 / 38 ms (üç koşu, `Resim{}` içinde — kojojs-dev#143) |

Tablodaki 8 ms'nin **2.8 ile 4.4 katı** arası. **Üç sayının yayılmasına
dikkat**: en büyüğü en küçüğün **1.6 katı** (35 ve 22). Yüzde vermiyoruz,
çünkü hangi tabana bölündüğüne göre %37 ile %59 arası değişiyor. Üçü de aynı
makinede, aynı şekil, tamamlanmış hâlde ölçüldü — fark işin kendisinden değil
koşudan koşuya değişimden geliyor. (O üçü örneğin eski hâlinde, elle eklenmiş
bir "şekli tamamla" satırıyla alındı; satır tam çokgeni bir kez daha
üçgenlettiği için toplama fazladan bir üçgenleme katıyordu; satırsız sayının
küçük çıkması beklenir. 18 ms gerçekten bandın altında — ama tek koşu, ve
koşudan koşuya yayılma 1.6 kat. Farkı bir üçgenlemeye yazmadan önce satırsız
hâlin de yayılmasını ölçmek gerekir.)

Sayı tam **251**, yani betiğin kendi geometrisi: kalem inince bir başlangıç
noktası konuyor, sonra 250 kenar ekleniyor. (Düzeltmeden önce bu örnek "146 nokta" gibi betikte karşılığı olmayan
sayılar yazıyordu — dolgu şekil bitmeden de yayınlanıyor ve not her yayını ayrı
ayrı bildiriyordu.)

Soğukla sıcak arasındaki fark **ölçüldü** (kojojs-dev#143, gerçek donanım):
her karede `resimleriSil(); çiz(Resim { gül })` yapan bir `canlandır` döngüsü
300 kareyi 5.0 saniyede bitiriyor — **60 kare/s**, yani ısınmış gülün dolgusu
bütçeye (16.7 ms) sığıyor. Koşu başına düşen tek not ilk, soğuk gülün:
20 / 22 / 27 ms. Tek atışlık koşulardaki 18–38 ms'nin hepsi soğuk sayı; yani
tablonun iyimser olduğu artık hipotez değil: soğuk/sıcak **en az** 1.2 (en hızlı
soğuk koşu), en yavaş soğuk koşu için en az 2.3 — alt sınır; üst sınır bu
veriden çıkmıyor, sıcak tarafın 16.7'nin ne kadar altında olduğu ölçülmedi
(#68'in ısıtılmış ~8 ms'i sıcak sayılsa 2.5–4.8 kat olurdu). 400 noktada aynı
tablo: 300 kare 5.0 s, tek not soğuk gülün, 36 ms — ısınmış 400 noktalı gül de
bütçeye sığıyor, oran orada en az 2.2 kat. 700 noktada da (tek not, soğuk 49 ms):
en az 2.9 kat. Sıcak gülün bütçeyi aştığı yer 700 ile 1000 arasında — 1000'de
ölçü aleti 7–8 (bu makine) / 15–17 (öteki) gül/s okuyor. Bu ölçüm bir süre
yapılamadı, çünkü buradaki eski sav — "ölçü aletinin döngüsünde not
düşmüyor, demek ki sıcak ≤16.7 ms" — yanlıştı (kojojs-dev#133 incelemesi):
o çıkarım şeklin *tamamlanmış* olmasını gerektiriyor, oysa aletin gülü
`canlandır` içinde hiç tamamlanmıyor. `Resim{}` için bu kapı yok (aşağıda),
ölçüm o yoldan geldi.

Tabloyu **büyük ölçek farkları** için okuyun (250 ile 4000 arasındaki fark
gerçek), yakın sayıları karşılaştırmak ya da mutlak eşik çıkarmak için değil.
Düzeltme öncesi alınan üç ölçüm de bunu söylüyordu: sıralama nokta sayısını
**izlemiyordu** (146 nokta 44 ms ama 236 nokta 27 ms), çünkü o sayılar tek tek
yarım yayınlardı ve bu ölçekte koşudan koşuya değişim baskın.

Kesişmeyen bir yolda aynı nokta sayısı bedavaya yakın: 4000 noktalı bir çemberin
dolgusu 6 ms'den az. Yani pahalı olan nokta sayısı değil, **kesişmeyle birlikte**
nokta sayısı.

Bir dolgu hesabı bir karelik bütçeyi (~17 ms) aşarsa iKojo çıktı paneline bir
not düşer: ne kadar sürdüğünü, kaç nokta olduğunu ve ne yapılabileceğini yazar.
Davranış değişmiyor — şekil yine çiziliyor; değişen şey, yavaşlığın artık
**sessiz olmaması**. (Stencil yolu bu makineye hiç süre yazmıyor: not
üçgenlemenin bedeli içindi, üçgenleme yoksa not da yok. İlk sürüm tampon
kurulum süresini de yazıyordu ve 40 000 noktalı gülde, büyüyen şeklin her
karede baştan kurulmasının toplamıyla, yanlış metinli bir not düşürdü —
canlıda görüldü, kojojs-dev#147.) `14-agir-dolgu.kojo`nun 2. deneyi
(`localStorage.kojoDolgu`) notu gösteriyor. Sonuç açıkça yazılsın: PIXI 5 +
stencil'li bir tarayıcıda, yani bugünün varsayılan yolunda, bu makineye süre
yazan tek yer 64 ve altı noktalı şekiller — orada libtess 1 ms'nin altında,
not pratikte hiç düşmez. Aşağıdaki not makinesi ve kuralları bugün yalnız
PIXI 4, stencil'siz bağlam ve elle geri dönüş için var.

Not **şekil başına en çok bir kez** düşer ve o şeklin **toplam** dolgu süresini
söyler. Bunun sebebi ölçülmüş: bir şekil bitmeden birkaç kez yayınlanıyor
(kaplumbağa komutları kuyrukta işleniyor), ve önceki sürüm her yayını ayrı ayrı
bildirdiği için betikte olmayan nokta sayıları yazıyordu — 250 noktalık bir gül
için "146 nokta" (kojojs-dev#125). Şekil henüz bitmemişken düşen not bunu
açıkça söyler: *"şimdilik N nokta; şekil büyüdükçe artacak"*.

Bir şeklin **bittiğini** üç şey söyleyebiliyor: kalem kalkık taşınma, boya
değişimi, ve — yenisi — **komut kuyruğunun boşalması** (kojojs-dev#134).
Üçüncüsü ancak betiği uyandırabilecek hiçbir şey kalmadığında sayılıyor:
`canlandır`, `yineleSayaçla`, `tuşaBasınca` ya da bir resim fare işleyicisi varsa
kuyruk boşalsa da şekle nokta gelebilir, ve orada not susuyor — yanlış bir
sayıyı kesin diye söylemektense. `Resim{}` içindeki kaplumbağa için bu kapı
**yok** (kojojs-dev#143): resmin gövdesi bitince şekli de bitmiştir, `çiz`/`sil`
yeniden çizer ama nokta eklemez; o yüzden `canlandır` içinde kurulan bir
`Resim{}` bütçeyi aşarsa konuşur. Karar da "kuyruk boşaldı" anında değil kare
sınırında veriliyor — yeni komut pompasında (kojojs-dev#131) kuyruk düz bir
döngüde her komuttan sonra boşalıyor, ve boşalma anında karar veren ilk sürüm,
pompa şeklin ortasında kareye teslim edince yarım şekli kesin sayıyla
bildiriyordu ("30 ms sürdü (21 nokta)", 251 noktalık gül için; kojojs-dev#148).
Üçüncüsü olmadan betiğin **son** şekli çoğu zaman hiç
"bitmiş" sayılmıyor, ve bütçeyi aşmasına rağmen sessiz kalabiliyordu:
`14-agir-dolgu.kojo` tam bu yüzden bir süre sessizdi ve örneğe elle bir
"şekli tamamla" satırı eklenmişti. Üçüncü yol canlıda doğrulanınca
(`18 ms sürdü (251 nokta)`) satır kaldırıldı. Üçüncü yolun nerede
*çalışmadığını* gösteren tuş deneyi (kuyruk tuşlar arasında boşalıyor ama
bir sonraki tuş nokta ekleyecek; not orada erken eşikle, "şu ana dek"
biçiminde konuşuyordu: 7. basış, 57 ms, "şimdilik 251 nokta") örneğin 4.
deneyi olarak duruyor — stencil yolunda sessiz, konsol anahtarıyla eski
davranış.

## `15-mesh-olcumu.kojo` bir ölçü aleti

Öteki dosyalar öğretmek için; bu dosya bir **değişikliğin öncesi ve sonrası
aynı şeyle ölçülsün** diye var (kojojs-dev#125). Kesişen bir şekli her karede
yeniden çizer ve saniyede kaç kare düştüğünü yazar.

Ölçtüğü şey şu: libtess yolunda dolgu üçgenlere ayrılıp PIXI'ye **üçgen
başına bir `drawPolygon`** ile veriliyordu (250 noktalı gülde yayın başına
2 998 çağrı). #125 bunun yerine tek bir mesh vermeyi tartışıyordu ve
**"yapılmayacak" diye kapandı**: mesh kendi diliminde 5–14 kat ucuz, ama dilim
gülün ≤%5–10'u (1000 noktada libtess %58), uçtan uca kazanç ≤%8 — bu aletin
çözünürlüğünün altında. Sonraki kaldıraç stencil dolgu oldu (kojojs-dev#147,
hiç üçgenlemeden; yukarıda). Öncesi bu aletle alındı: 1000 noktada 15–17 gül/s
bir makinede, 7–8 ötekinde (notlar 58–81 / 105–112 ms); harness'te #148 öncesi
ve sonrası aynı (7.0 / 6.9), yani gerileme değil makine farkı. **Sonrası aynı
makinede alınacak** — ve 1000'de değil: stencil'de 1000 noktalı gül 1 ms'nin
altında, alet orada el sıkışma tavanına (~30 gül/s) dayanır ve sinyal görünmez;
ölçüm **4000 × 7** ile (libtess'te ~1.8 s/gül, yani 1 gül/s'nin altı).
Betik o değişiklikleri **yapamaz** — dolgunun nasıl çizildiği kitaplığın
içinde; betiğin işi yalnız kareyi saymak.

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

Sayılan gül ile **boyanan** gül aynı mı? Yapısal olarak evet: her karede en
fazla bir gül başlıyor (`canlandır`), bir gül ancak öncekinin kuyruğu bitince
(`konumuOku`) ve bir sonraki karede başlıyor, ve karenin sonunda boyanıyor.
Bu tasarım kojojs-dev#131'in sonucu. Eski komut pompası 100 komutta bir 4 ms'lik
`setTimeout` arası veriyordu ve bir gül birkaç kareye yayılıyordu; yeni pompa bir
karede 8 ms iş yapıp kareye teslim ediyor ve 250 noktalı gül ~0.3 ms'de bitiyor.
Aletin önceki sürümü (bir sonraki gülü `konumuOku` içinden başlatan zincir) o
rejimde gülleri **boyanmadan** siliyordu — ölçüldü, on gülün sekizi hiç
yayınlanmadan bitti — ve okunan sayı gül/s değil kuyruk hızı olurdu; #125'in
kazancı en çok render tarafında olduğu için alet mesh'i kendi aleyhine ölçerdi.
Sınama savı (`kojo.MeshAletiOlcumTest`) hâlâ "sayılan her gülün en az bir boyaması
var" ve bu değişiklikte kırmızıya dönüp aleti yeniden tasarlattı.

Üst sınır bu yüzden kare hızının yarısı (~30 gül/s, iki karelik el sıkışma):
okunan sayı artık üçgenleme + çizim maliyetinin haberi — tam #125'in dokunduğu
yer. Eski pompayla alınan 250×7 → 38–41 gül/s ile **karşılaştırılamaz**; o
sayının ~%80'i pompanın bekleme süresiydi.

Canlıda ölçüldü (gerçek donanım, kojojs-dev#131): 250'de eski pompada da yeni
pompada da **30** (tavan — orada hiçbir değişiklik görülmez); 1000'de eski pompa
**4–5**, yeni pompa **15–17** gül/s. Fark yalnız pompa payının gitmesi değil: gül
tek karede bitince tek kez üçgenleniyor, eskiden ~10 karede büyüyen önek her
seferinde yeniden üçgenleniyordu. Aynı betiklerle 100 000 komutluk kalemli iş
6.9–10.4 s'den 108–165 ms'ye indi (`sil()`li varyant, yalnız pompayı ölçen:
4.56 s → 93–169 ms). O iki betik, olduğu gibi (dolgulu sürümü ayrı bir alet:
`16-yuz-bin-komut.kojo`, aşağıda):

```
sil()
hızıKur(çokHızlı)
gizle()
kalemKalınlığınıKur(1)
dez t0 = buAn
yinele(200) {
  yinele(250) { ileri(24.6); sağ(10.08) }
}
konumuOku { _ => satıryaz("100 000 komut: " + (buAn - t0) + " ms") }
```

```
sil()
hızıKur(çokHızlı)
gizle()
kalemKalınlığınıKur(1)
dez t0 = buAn
yinele(200) {
  sil()
  yinele(250) { ileri(24.6); sağ(10.08) }
}
konumuOku { _ => satıryaz("100 000 komut, sil'li: " + (buAn - t0) + " ms") }
```

İkisinin sonrasında aynı çıkması öğretici: iz 50 bin parçaya büyüyor ama bedeli
büyüklüğü değil kaç kez çizildiği — eski pompada ~500 kare, yenisinde ~8.

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

## `16-yuz-bin-komut.kojo` da bir ölçü aleti

Aynı 100 000 komut (200 tur × 250 × [ileri + sağ]), bu kez **dolguyla**: tek
bir 50 001 noktalı, kendini kesen çokgen. Yazdığı sayı komut pompasının hızı
artı dolgunun bedeli. Kalemli sürümü yukarıda (`boyamaRenginiKur` satırı
olmadan aynı döngü).

Libtess'i **zorlamıyor**: 200 tur aynı gülü üst üste çiziyor ve çakışık kenar
libtess'te yeni kesişme değil — 50 001 nokta eski yolda bile yüzlerce
milisaniye (MacBook, kojojs-dev#147 §7: **719 ms**, not "76 ms (7 993 nokta)").
Stencil'deki karşılığı yayın sonrası ölçülecek; dosyanın başındaki tabloya
yazılır. Eski yolu elle açmak yukarıdaki konsol anahtarıyla; eski yol açıkken
panelin ilk satırı "Eski dolgu yolu (libtess) elle açık …" olur, hangi yolu
ölçtüğün oradan belli.
