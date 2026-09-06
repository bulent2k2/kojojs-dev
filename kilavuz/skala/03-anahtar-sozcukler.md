# Anahtar Sözcükler

Türkçemizde kimbilir kaç bin kelime var? Hepsini bilmek mümkün değil elbet. Ayrıca pek çok kelimenin birden çok anlamı oluyor. Hatta bazen anlamları belirsiz bile olabiliyor ve sözlüğe bakmamız yetmiyor, ne demek istediniz diye soruyoruz. Ama yine de birbirimizi anlamakta genelde zorlanmıyoruz. Bilgisayar dillerindeyse durum çok farklı. Hem çok daha az sözcük gerekli hem de sözcüklerin anlamı çok daha belirli.

Bilgisayar dillerinin derleyicileri olduğunu ve yazdığımız yazılımları donanımın anlayacağı 0 ve 1 sayılarına çevirdiğini duymuş olabilirsin. Bunu yapabilmek için genelde derleyiciler çok küçük ama çok becerikli bir sözlükle yazılır. Bu sözlükteki kelimelere 'anahtar' ya da 'özel' sözcükler deriz. Scala dilinin anahtar sözcüklerinin en önemlilerini bu kılavuzda örnekleriyle görecek ve öğreneceğiz. Yine de bu bölümde hepsini yazdık ki ilerde İngilizcelerini bilmek gerekince gelip bakabilelim.

Bu bölümün gerisini şimdi atla istersen. Sonra merak ettikçe gelip bakarsın. İstersen hızlıca bir göz at. Zaten detaylarını görmeden aşağıdaki açıklamalar pek anlamlı olmaz. Şimdilik bilmemiz gereken tek şey, herşeyin çok küçücük bir anahtar kelimeler sözlüğüyle başladığı. Onun için bir bilgisayar dili öğrenmek yabancı bir dil öğrenmekten çok daha kolay aslında! Tabii bir de kaplumbağacığın anladığı komut sözcükler var. Bir önceki bölümde bir kısmını gördük ve öğrendik. Ama onlar da zaten bizim dilimizdeki anlamlarına çok yakın, değil mi?

Aşağıdaki her satırda bir anahtar sözcük var. İlk sütun, kılavuzdaki kullanış sırasına göre sıralı. Sonra gelip bulmak kolay olsun diye. İkinci sütunda anahtar sözcüğün İngilizce'si var. Keyword diye de bilinir. Eğer o keyword, İngilizce bir sözcüğün kısaltmasıysa o sözcüğün kendisi de üçüncü sütunda. Son sütunda da ufak bir açıklamayla yetindik. Kılavuzumuzun diğer bölümleri daha geniş açıklamalar verecek.

| Türkçe | İngilizce | Açılımı | Açıklama |
|---|---|---|---|
| `tanım` | `def` | define | İlk bölümde kaplumbağacığa üçgen çizdirmek için kullanmıştık. Bu anahtar sözcük yeni bir işlev tanımlar ve ona bir ad takar. Bu yeni ad da derleyicinin sözlüğüne eklenmiş olur! |
| `dez` | `val` | value | İlk bölümdeki yeni kaplumbağacığa ad takmamızı sağlayan anahtar sözcük. Bilinen ve değişmez bir değere bir ad takar. Bu yeni ad da yazılımcığın özel sözlüğüne eklenmiş olur. |
| `den` | `var` | variable | Yeni bir değişken tanımlar ve ona bir ad takar. Bu yeni ad da sözlüğe eklenmiş olur. |
| `eğer` | `if` | | Bir çatallanma yani karar verme durumu tanımlar. Duruma göre iki seçenekten birini seçer derleyici. |
| `yoksa` | `else` | | eğer sözcüğünden sonra kullanılır. |
| `için` | `for` | | İşlevsel bir döngü tanımlar. Çok daha iyi bir yöntemdir! |
| `ver` | `yield` | | İçin komudunun içinde teker teker nesne çıktısı vermek için kullanılır. |
| `sınıf` | `class` | | Yeni bir tür tanımlar. Nesne odaklı yazılımın ana birim taşıdır. |
| `durum` | `case` | | İki durumda kullanılır: Daha becerikli bir sınıf tanımlamaya ve örüntü/desen eşlemeye yarar. |
| `yayar` | `extends` | | Bir türün başka bir türü yaymasına yarar. İleri bir konudur. Hafifçe değindik sadece. |
| `yeni` | `new` | | Yeni bir nesne oluşturmak için gerekebilir. |
| `eşle` | `match` | | Örüntü/desen eşlemeye yarar. |
| `doğru` | `true` | | Mantıksal doğru. Eğer komuduna girdi olarak kullanılabilir. |
| `yanlış` | `false` | | Mantıksal yanlış. Eğer komuduna girdi olarak kullanılabilir. |
| `baskın` | `override` | | Daha temel bir türden (üst sınıf da denir) gelen yöntemleri yeniden tanımlarken gerekir. |

Yukarıdaki 15 sözcükle neler neler yapabiliriz yakında göreceğiz. Bu kılavuzda hiç kullanmadığımız diğer anahtar sözcükler de aşağıda. İngilizcelerinin A'dan Z'ye sıralamasına uyduk. Örnek yazılımcıklarda ender de olsa kullanılıyorlar. Ama şu ikisini de unutmayalım:

| Türkçe | İngilizce | Açıklama |
|---|---|---|
| `yineleDoğruKaldıkça` | `while` | Bir koşul doğru kaldıkça yinelenen bir döngü tanımlar. Koşul baştan yanlışsa döngüye hiç girmez. |
| `yap` | `do` | Önce 'yineleDoğruKaldıkça' döngüsünün komutları en az bir kere çalışsın sonra da koşuluna bakılsın dersen bu sözcükle olur. |

Bunlar eski programlama dillerinde çok faydalı komutlardı. Ama Scala'da pek gerek yok. Kullanılmasalar daha iyi olur. Onun için birincisinin adını da uzun tuttuk. Kılavuzumuzda da az yer verdik.

| Türkçe | İngilizce | Açıklama |
|---|---|---|
| `soyut` | `abstract` | Ender kullanılır. [Scala kitabından soyut sınıflar](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#abstract-classes) |
| `yakala` | `catch` | Kuraldışı bir durum varsa işe yarar. 'dene' ile birlikte kullanılır. |
| `son` | `final` | Baskın komuduyla yeniden tanımlanmasını istemediğimiz nesneler ve türler için gerekir. |
| `sonunda` | `finally` | dene ve yakala komutlarıyla birlikte işe yarar. |
| `bazı` | `forSome` | Bu Scala3'te yok. Sadece Scala2'de var. Unut daha iyi! |
| `örtük` | `implicit` | İşte usta yazıcılar için çok faydalı bir komut. Ama Scala3'te daha iyisi var. |
| `getir` | `import` | Başka bir yazılımın bizim yazılımcığımız içinde kullanılmasını sağlar. |
| `miskin` | `lazy` | Miskinlik de çok faydalıdır bazı durumlarda. |
| `yok` | `null` | Bu Java'dan kalma. Hiç kullanma! |
| `nesne` | `object` | Bazen sınıfa gerek olmaz. Tek nesne yeter. Bazen de sınıfa destek olur. |
| `deste` | `package` | Büyük yazılımları deste deste düzenlemeye yarar. Sonra 'getir' ile kullanılır. |
| `gizli` | `private` | Dışarıya gizli nesneler ve türler tanımlamak için. |
| `koru` | `protected` | Alt türlere açık dışarıya gizli. |
| `geriDön` | `return` | İşlevden prematüre geri dönüş için. Hiç kullanma daha iyi. |
| `damgalı` | `sealed` | Bir dosya dışında değişiklik yapılmasını istemediğimiz türler için kullanırız. |
| `üst` | `super` | Bir üst türden bahsetmek gerekirse. |
| `bu` | `this` | Bir sınıfın tanımının içinde kendi nesnelerinden bahsetmek için kullanılabilen adıl. Ya da eski adıyla bu zamiri! |
| `bildir` | `throw` | Kuraldışı durumlarda kullanılır. 'Throw' atmak demek. Emir ya da istek kipi 'at' olacaktı. Ama bazı teknik sorunlardan ötürü olmadı. Zaten bildir sözcüğü daha kibar oldu, değil mi? |
| `özellik` | `trait` | Temel bir tür tanımlamak için çok faydalıdır. Ama onun yerine 'sınıf' da kullanılabilir. |
| `dene` | `try` | Kuraldışı durumları sınır içine alır. |
| `tür` | `type` | Bir türe yeni bir ad takar. |
| `birlikte` | `with` | Bir türün bir türü başka bir türle birlikte yaymasına yarar. İleri bir konudur. Bu kılavuzda henüz anlatmadık. |

Son olarak da alfabetik sırayla hepsini birbirine bağlayalım, bulmak kolay olsun diye. Önce Türkçe'de sıralı, sonra İngilizce sıralı:

| Türkçe | İngilizce | | İngilizce | Türkçe |
|---|---|---|---|---|
| `baskın` | `override` | | `abstract` | `soyut` |
| `bazı` | `forSome` | | `case` | `durum` |
| `bildir` | `throw` | | `catch` | `yakala` |
| `birlikte` | `with` | | `class` | `sınıf` |
| `bu` | `this` | | `def` | `tanım` |
| `damgalı` | `sealed` | | `do` | `yap` |
| `den` | `var` | | `else` | `yoksa` |
| `dene` | `try` | | `extends` | `yayar` |
| `deste` | `package` | | `false` | `yanlış` |
| `dez` | `val` | | `final` | `son` |
| `doğru` | `true` | | `finally` | `sonunda` |
| `durum` | `case` | | `for` | `için` |
| `eğer` | `if` | | `forSome` | `bazı` |
| `eşle` | `match` | | `if` | `eğer` |
| `geriDön` | `return` | | `implicit` | `örtük` |
| `getir` | `import` | | `import` | `getir` |
| `gizli` | `private` | | `lazy` | `miskin` |
| `için` | `for` | | `match` | `eşle` |
| `koru` | `protected` | | `new` | `yeni` |
| `miskin` | `lazy` | | `null` | `yok` |
| `nesne` | `object` | | `object` | `nesne` |
| `son` | `final` | | `override` | `baskın` |
| `sonunda` | `finally` | | `package` | `deste` |
| `soyut` | `abstract` | | `private` | `gizli` |
| `sınıf` | `class` | | `protected` | `koru` |
| `tanım` | `def` | | `return` | `geriDön` |
| `tür` | `type` | | `sealed` | `damgalı` |
| `ver` | `yield` | | `super` | `üst` |
| `yakala` | `catch` | | `this` | `bu` |
| `yanlış` | `false` | | `throw` | `bildir` |
| `yap` | `do` | | `trait` | `özellik` |
| `yayar` | `extends` | | `true` | `doğru` |
| `yeni` | `new` | | `try` | `dene` |
| `yineleDoğruKaldıkça` | `while` | | `type` | `tür` |
| `yok` | `null` | | `val` | `dez` |
| `yoksa` | `else` | | `var` | `den` |
| `örtük` | `implicit` | | `while` | `yineleDoğruKaldıkça` |
| `özellik` | `trait` | | `with` | `birlikte` |
| `üst` | `super` | | `yield` | `ver` |

İşte hepsi bu kadar! Yabancı dil öğrenmekten çok daha kolay değil mi ama? Türkçe ve İngilizce adların tam listesi için [Koco Sözlüğü](/yardim/sozluk)'ne de bakabilirsin.
