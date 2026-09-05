# Scala'ya Hızlı Giriş

Scala diline hoşgeldin! Skala diye oku, olur mu? Bu kılavuz, masaüstü Koco'nun **Yardım → Scala'ya Giriş** öyküsünün tarayıcı sürümüdür. Soldaki listeden istediğin bölüme atlayabilirsin; her bölümün sonunda da bir önceki ve bir sonraki bölüme bağlantı var.

## Bu kılavuz nasıl kullanılır?

Kılavuzda pek çok yazılımcık örneği bulacaksın. Her kod bloğunun altındaki **Editörde aç →** bağlantısı örneği yeni bir sekmede iKoco düzenleyicisine taşır; oradaki **Çalıştır** düğmesine basman yeter. Sonra istediğin değişikliği yapıp tekrar çalıştır. Değiştir, boz, düzelt: en iyi böyle öğrenilir.

- Çok uzun örneklerde bağlantı yerine **Kopyala** düğmesi var. Kodu panoya alır; düzenleyiciye yapıştırıp çalıştır.
- Bazı tablolarda tek komutluk hücreler (`ileri(100)` gibi) de tıklanabilir; onlar da düzenleyicide açılır.
- Turuncu **masaüstü** rozeti taşıyan örneklerde, yalnız masaüstü Koco'da bulunan bir komut var. Rozetin yanında hangi komutun eksik olduğu ve varsa ikojo'daki karşılığı yazar. Kod yine gösteriliyor ki masaüstünde deneyebil. Farkların özeti için [Farklar](/yardim/farklar) sayfasına bak.

> Masaüstü Koco'dan iki fark daha var, bilmende fayda olacak. **Bir:** masaüstünde `1 + 2` gibi yalın bir deyiş yazınca sonucu çıktı gözünde görürsün; tarayıcıda görmek için `satıryaz(1 + 2)` yazmak gerekir, o yüzden burada örneklerin çoğu `satıryaz` içinde. **İki:** masaüstünde art arda çalıştırdığın örnekler birbirinin tanımlarını hatırlar; tarayıcıda her çalıştırma temiz başlar. Onun için birbirine dayanan örneklerin tanımlarını her blokta yineledik. Ayrıca ikojo'da `satıryaz(a, b)` ikiliyi `(a,b)` biçiminde tek parça yazar.

## Bölümler

| # | Bölüm | Ne öğreneceğiz? |
|---|---|---|
| 2 | [Kaplumbağacığın Kullanılışı](#b02) | Kaplumbağayı yürüten ve çizdiren komutlar |
| 3 | [Anahtar Sözcükler](#b03) | Türkçe anahtar sözcükler ve İngilizceleri |
| 4 | [Başlayalım](#b04) | Deyişler, `dez` ve `den`, açıklamalar, yazılar, parçacık işlemleri |
| 5 | [Yazılım Akışı: Eğer, Yoksa ve İçin](#b05) | Karar çatalları ve döngüler |
| 6 | [Yalın Değerler, Sayılar, Kesirler ve Yazılar](#b06) | Temel türler |
| 7 | [İşlevler](#b07) | `tanım` ile işlev tanımlamak, özyineleme |
| 8 | [Nesneler ve Sınıflar](#b08) | `sınıf`, `yeni`, `durum sınıf`, `yayar`, `baskın` |
| 9 | [Desen Eşleme: Eşle ve Durum](#b09) | `eşle` / `durum` yapısı |
| 10 | [İleri Eşleme: İkil Ağaç](#b10) | Sınıf hiyerarşisi ve özyineli arama |
| 11 | [Dingin Türleme ve Tür Çıkarımı](#b11) | Derleyici türleri nasıl bulur? |
| 12 | [İşlevler de Birer Nesnedir](#b12) | İşlev girdileri, adsız işlevler, genelleyici işlevler, `işle`/`ele`/`katla` |
| 13 | [Sıralamalar (Tuple)](#b13) | Çoklu değerler ve çözümleme |
| 14 | [Matematiksel İşlevler](#b14) | Trigonometri, üst, yuvarlama, rastgele |
| 15 | [İşlem Önceliği ve Birleşmeliği](#b15) | Kendi işlemlerini tanımlamak |
| 16 | [Yazıların (String) Kullanılışı](#b16) | Yazı yöntemleri |
| 17 | [Dizinlerin (List) Kullanılışı](#b17) | Dizin yöntemleri |
| 18 | [Çizim ve Oyun](#b18) | `Resim`, `canlandır`, klavye, saat, Yaşam Oyunu, Planarity |
| 19 | [Daha Çok Öğrenelim](#b19) | Kitaplar ve bağlantılar |

Kojo'nun kaplumbağacığını zaten iyi tanıyorsan ikinci bölümü atlayıp doğrudan [Başlayalım](#b04) ile başlayabilirsin. İlk iki üç bölümü okuduktan sonra sona yakın olan [Çizim ve Oyun](#b18) bölümüne göz atmanda da fayda var: orada çok şey öğreneceksin.

Not: Bu kılavuz Anthony Bagwell'in simplyscala.com sitesinden Kojo'ya uyarlanarak yazılmıştır (Phil Bagwell; Kojo örneklerinin bir kısmı Lalit Pant'tan). O site ne yazık ki artık çalışmıyor, ama arşivde bulabilirsin: [web.archive.org'dan simplyscala.com](http://web.archive.org/web/20130305041026/http://www.simplyscala.com). Türkçesi Bülent Başaran'ın sevgiyle çevirisidir.
