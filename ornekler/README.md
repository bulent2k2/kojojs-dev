# Koco örnekleri

Türkçe Kojo (Koco) ile yazılmış örnek programlar. Her dosya doğrudan
**https://ikojo.fly.dev** adresine yapıştırılıp çalıştırılabilir.

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

## Nasıl çalıştırılır

1. https://ikojo.fly.dev adresini aç
2. Düzenleyicideki her şeyi sil, dosyanın içeriğini yapıştır
3. **Çalıştır**

İlk derleme makine yeni başladıysa 10-15 saniye sürebilir; sonrakiler ~1 saniye.

## Hız hakkında

`hızıKur(çokHızlı)` animasyonu tamamen kapatır ve çizim anında biter.
Varsayılan hız yavaştır (adım başına ~1 saniye), bu yüzden çok şekil çizen
programlarda `çokHızlı` kullanmak gerekir. Ara değerler: `yavaş`, `orta`,
`hızlı`.

## Bu örnekler test ediliyor

`ornekleri-dogrula.sh` her dosyayı gerçek derleyiciye gönderip hata dönmediğini
kontrol eder — yani bozuk bir örnek fark edilmeden kalmaz:

```sh
./ornekleri-dogrula.sh                      # canlı sunucuya karşı
KOCO=http://localhost:7860 ./ornekleri-dogrula.sh   # yerel konteynere karşı
```
