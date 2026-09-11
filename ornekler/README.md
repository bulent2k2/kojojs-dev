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

## Bu örnekler test ediliyor

`ornekleri-dogrula.sh` her dosyayı gerçek derleyiciye gönderip hata dönmediğini
kontrol eder — yani bozuk bir örnek fark edilmeden kalmaz:

```sh
./ornekleri-dogrula.sh                              # canlı sunucuya karşı, bu dizindeki 10 örnek
KOCO=http://localhost:7860 ./ornekleri-dogrula.sh   # yerel konteynere karşı
./ornekleri-dogrula.sh masaustu                     # masaüstü betikleri (özyineli)
./ornekleri-dogrula.sh -g masaustu/derleme.tsv masaustu   # sonucu TSV'ye yaz
./ornekleri-dogrula.sh -b masaustu/derleme.tsv masaustu   # öncekiyle karşılaştır; yalnız gerileme hata
```

Dosya ya da dizin verilebilir; `-b` olmadan her kaldı çıkış kodu 1'dir (buradaki
10 örneğin hepsi geçmeli). `-b` ile beklenen durum dosyasına göre yalnız gerileme
(geçti → kaldı) hata sayılır; ilerleme ⬆ ile işaretlenir.

## Masaüstü betikleri (`masaustu/`)

`bulent2k2/kojo` reposundaki 112 Türkçe betiğin değiştirilmemiş kopyası ve
uçurum ölçümü (`tarama.tsv`). Ayrıntı: `masaustu/README.md`, ölçüm aracı
`../araclar/ucurum.py`.
