# Çizim ve Oyun

Kojo'nun Scala üzerine yaptığı katkılardan biri kaplumbağacıklarla çizimler yapmak ve bilgisayar oyunları yazmak. Ama Kojo'nun bir de Resim adlı bir birimi var ki, onunla daha da kolaylıkla bilgisayar grafikleri çizip onları canlandırabilir ve istersen oyunlar programlayabilirsin. Kojo'ya bu becerileri kazandıranlardan birisi de Peter Lewerin adlı bilgisayar aşığı. Lalit ve Peter sayesinde bilgisayar oyunları yazman ve güzel bilgisayar çizimleri yapman çok daha kolay. Resim biriminin becerilerinin tarihçesi Processing adlı bir Java projesine kadar uzanır. Processing birimini de Kojo'ya Peter eklemişti. Bu Resim birimi o kadar becerikli ki sırf onun üzerine büyük bir kılavuz bile yazılabilir. Burada bir kaç örnekle neler yapılabilir görelim ve tadında bırakalım istersen. [Kojo ile öğren](/kojo-ile-ogren) ve [Benzetim savı](/benzetim) sayfalarında başka pek çok örnek bulacaksın. Buradakileri deneyip anladıktan sonra onlara da bakmanda fayda var.

Eğer ilgilenip daha çok örnek görmek istersen, [Kojo'nun web sayfasına](https://docs.kogics.net) da bak. Henüz sadece İngilizce. Resim yerine Picture diyecek. Ama oradaki örnekler de sana bir fikir verecektir.

Resim adlı birim birçok komut, işlem ve bir de çizim döngüsü sunuyor bize. Bunları kullanarak epey gelişmiş şekiller, resimler ve grafikler çizebiliriz. Çizim döngüsünü kullanarak grafikleri canlandırabilir ve istersek fare ve tuşlarla kontrol ekleyerek oyuna çevirebiliriz.

İlk örnekle başlayalım. Önce ekranı temizliyoruz. Sonra bir top çiziyor ve canlandırma döngüsünün içinde topun nasıl hareket edeceğini tanımlıyoruz. Resim birimi, bu döngüyü yaklaşık yirmibeş milisaniyede bir yineleyerek çalıştırıyor. Bilgisayarın hızına göre saniyede yaklaşık olarak 40 kere tekrar tekrar resim çizebiliyor yani. Bu da bizim gözümüzün değişiklikleri görme hızından fazla olduğu için bize canlı ve hareketli gibi görünüyor. Bu temel kavramları ve komutları kullanarak pek çok ilginç canlı, hareketli grafik çizebilirsin.

```scala
silVeSakla()
dez yç = 10 // topumuzun yarıçapı
dez top = Resim.daire(yç) // resmi
çiz(top) // bu da tuvale çiziveriyor topu
// topun içinde kalmasını istediğimiz sahayı da tanımlayıp çizelim
dez en = 400; dez boy = 200 // çizmesek de çalışır ama görmek faydalı:
çiz(kalemRengi(siyah) * götür(-en / 2, -boy / 2) -> Resim.dikdörtgen(en, boy))
// topun şu ufak ve iri sayılar arasında kalmasını istiyoruz
dez (ufakX, iriX) = (-en/2 + yç, en/2 - yç)
dez (ufakY, iriY) = (-boy/2 + yç, boy/2 - yç)
den x = 0; den y = 0 // topun konumunu bu değişkenlerle belirleyeceğiz
den dx = 2; den dy = 4 // topun hızını da x ve y'deki değişikliklerle belirleyeceğiz
// Bu dx ve dy'i birazcık değiştirip tekrar çalıştır. Hemen anlarsın ne oluyor.
// Bize bir de döngü gerek. Bu komut çok becerikli:
canlandır { // İçindeki komutlar saniyede yaklaşık 40 kere yinelenir.
    top.kondur(x, y) // bu yöntem resimlerin yani burda bizim topun yerini değiştirir
    // topun konumunu yani x ve y'yi günceliyoruz:
    // Zıplama alanının dışına çıkmasın diye sınıra gelince
    // hızının doğrultusunu tersine çeviriyoruz:
    dx = eğer (x <= ufakX || x >= iriX) -dx yoksa dx
    dy = eğer (y <= ufakY || y >= iriY) -dy yoksa dy
    x += dx
    y += dy
}
```

Masaüstünde bu örnek `silipSakla(); ızgarayıGöster(); eksenleriGöster()` ile başlıyordu; ikojo'da `silVeSakla` var, ızgara ve eksen komutları henüz yok.

İkinci örneğimiz küçük ve basit bir oyun. Duymuş olabilirsin: en eski bilgisayar oyunlarından duvara karşı pinpon (İngilizce adıyla Pong) oyunu. Tek kişilik bir oyun. Yapmamız gereken topa raketle vurup geri yollamak. Raketi fareyle yönetiyoruz. Top kaçarsa bir puan kaybediyorsun. İyi eğlenceler!

```scala
silipSakla()
çiz(götür(-200, -100) -> Resim.düz(0, 200)) // Üç duvar çizelim. Bu dik ön duvar
çiz(götür(-200, -100) -> Resim.düz(400, 0)) // Bu alt duvar
çiz(götür(-200,  100) -> Resim.düz(400, 0)) // Bu da üst duvar
dez rb = 80 // raketin boyu
dez raket = kalemRengi(mavi) -> Resim.düz(0, rb)
dez top = kalemRengi(mavi) -> Resim.daire(5)
dez skor = kalemRengi(siyah) * götür(-50, 150) -> Resim.yazı("Raketi fareyle yönet")
çiz(raket, top, skor)
den x = 0.0; den y = 0 // topun konumu
den dy = 8; den dx = -8.0 // topun hızı: d delta yani değişim ya da derivative yani türev demek
den rx = 0.0; den ry = 0.0 // raketin konumu
den ıska = 0 // top kaç kere kaçtı, saymak için
den vuruş = 0 // kaç kere raketle vurduğumuzu da sayalım
canlandır {
    raket.kondur(fareKonumu)  // raketi fareyle kontrol ediyoruz
    top.kondur(x, y) // topun yerini değiştirelim
    // top rakete çarpıyor mu?
    rx = fareKonumu.x; ry = fareKonumu.y
    dx = eğer ((dx > 0) && (mutlakDeğer(rx - x) < 10) &&
        (y > ry) && (y < ry + rb)) {vuruş += 1; -1.1*dx} yoksa dx
    // topun konumunu güncelleyelim, duvarlara bakalım
    dx = eğer (x + dx < -200) -dx yoksa dx  // ön duvardan sekti mi?
    dy = eğer ((y + dy < -100) || (y + dy > 100)) -dy yoksa dy // üst ve alt duvarlardan sekti mi?
    eğer (x + dx > 200) { x = -200; dx = 8; ıska += 1 } // ıskaladı
    x += dx; y += dy // topun gideceği noktayı hesaplayalım
    eğer (ıska > 0 || vuruş > 0) {  // skoru güncelleyelim
        dez mesaj = eğer (vuruş == 0) "Fareyi tuvale getir!" yoksa s"$vuruş kere vurdun"
        skor.güncelle(s"$mesaj\n$ıska kere ıskaladın")
    }
}
oyunSüresiniGeriyeSayarakGöster(60, "Süre bitti", yeşil) // oyun 60 saniye sürsün
```
<!-- masaüstü: Resim.düz→Resim.çizgi, fareKonumu, oyunSüresiniGeriyeSayarakGöster→oyunSüresiniGöster -->

Temel kavramlar ve komutlarla artık tanıştın. Bir kaç tane daha top eklemeye ne dersin? Topların hızını rastgele değiştirerek oyunu daha eğlenceli kılabilirsin istersen. Raketin boyunu kısaltabilir ya da uzatabilirsin. Bir de programımızın bir (belki kimbilir daha çok) hatası var! İngilizce'de bug yani böcek denir. Ama nerden çıktı deme. Programı yazanın hatası de. Sen de yapacaksın bol bol hata. Hiç dert etme. Her hata aslında daha usta olmak için bir fırsat. Fırsatları hiç kaçırma! Hatayı farkettin mi? Bazen top raketin içinden geçiyor sanki elektron tünelleme yapıyormuş gibi (kuvantum mekaniği değil ki bu)! Bazen de ıskalaması gerekirken geri yolluyor.. Bakalım yazılımcığın içinde nerede? Sen bulup düzeltebilecek misin?

### Klavye ve tuşlarla komut girişi

Pek çok oyun klavyeden komut bekler. Minecraft oynadın mı hiç? Hem fare hem de klavye komutları oyunu iyice eğlenceli kılar. Bak bu küçük oyun sağ/sol/yukarı ve aşağı ok olan tuşlarla kaplumbağacığa yön veriyor.

Her zamanki gibi aşağıdaki yazılımcığı çalıştır. Sonra da tuvale tıkla ki klavyeye bastığın zaman farketsin kaplumbağa ve söz dinlesin, senin komutlarını yerine getirsin.

```scala
sil()
görünür()
canlandırmaHızınıKur(100)
den (zıpladı, bilmem) = (yanlış, yanlış)
tuşaBasınca { t =>
    t eşle {
        durum tuşlar.sol    => açıyaDön(180)  // sola git
        durum tuşlar.sağ    => açıyaDön(0)    // sağa git
        durum tuşlar.yukarı => açıyaDön(90)   // yukarı
        durum tuşlar.aşağı  => açıyaDön(270)  // aşağı
        durum tuşlar.z      => zıpla(20); zıpladı = doğru
        durum tuşlar.a      => atla(fareKonumu.x,fareKonumu.y); zıpladı = doğru
        durum tuşlar.n      => noktayaGit(fareKonumu.x,fareKonumu.y); zıpladı = doğru
        durum tuşlar.boşluk => // büyük boşluk tuşu sadece ilerletsin
        durum _             => bilmem = doğru // diğer tuşlar
    }
    eğer (bilmem) { bilmem = yanlış } yoksa {
        eğer (zıpladı) { zıpladı = yanlış } yoksa { ileri(20)}
    }
}
tuvaliEtkinleştir()
```
<!-- masaüstü: tuşaBasınca→onKeyPress, fareKonumu -->

Hayal gücünü kullan, yazılımcığı değiştir (örneğin 45 derece döndürmek, yay ya da çember çizmek için komutlar ekleyebilirsin), tekrar çalıştır. Masaüstü düzenleyicisinde `tuşlar.` yazdıktan sonra (ama tırnak işaretleri olmadan!) kontrol tuşunu basık tutup büyük boşluk tuşuna bas ki başka hangi tuşları kullanabileceğini gör. ikojo'da tuşları `tuşBasılıMı(tuşlar.sol)` gibi bir `canlandır` döngüsü içinde sorgulayabilirsin; [Kojo ile öğren](/kojo-ile-ogren) sayfasındaki klavye oyununa bak.

### Saat

Bir saat yapalım mı? BuAn adlı birimi kullanacağız. O da içinde Java'nın takvim, tarih ve zaman adlı kütüphane birimlerini kullanıyor.

```scala
silipSakla
dez yç = 150 //             saatin yarıçapı
dez pi2 = 2.0 * piSayısı // 2*pi radyan tam 360 derece dönüş demek

// saatin her unsuru için değişik bir renk kullanalım:
//   pembe, kırmızı, mavi, yeşil, turuncu ve siyah
// böylece hem renkli olur hem de programı anlamak kolaylaşır
tanım renk(r: Renk) = kalemRengi(r)

tanım saat = { // bu işlev saatin değişmeyen kısımlarını çizer
    için (i <- 0 |-| 59) { // dakika ve saat çentikleri
        dez ra = pi2 * i / 60
        dez (x, y) = (yç * sinüs(ra), yç * kosinüs(ra))
        dez çentikBoyu = eğer (i % 5 == 0) 0.9 yoksa 0.95
        dez (llx, lly, urx, ury) = (çentikBoyu * x, çentikBoyu * y, x, y)
        dez (en, boy) = (urx - llx, ury - lly)
        çiz(renk(pembe) * götür(llx, lly) -> Resim.düz(en, boy))
    }
    çiz(renk(kırmızı) -> Resim.daire(yç))
}

canlandır { //     bu döngü her saniyede yaklaşık 40 kere yinelenir
    Resim.sil() // bütün resimleri silelim
    saat //        tekrar çizelim
    dez buan = BuAn()
    dez s = pi2 * buan.saniye / 60 //         saniye kolu
    dez m = pi2 * buan.dakika / 60 //         dakika kolu
    dez h = pi2 * buan.saat / 12 + m / 12 //  saat kolu
    çiz(
        renk(mavi) -> Resim.düz(0.9 * yç * sinüs(s), 0.9 * yç * kosinüs(s)),
        renk(yeşil) -> Resim.düz(0.8 * yç * sinüs(m), 0.8 * yç * kosinüs(m)),
        renk(turuncu) -> Resim.düz(0.6 * yç * sinüs(h), 0.6 * yç * kosinüs(h)),
        renk(siyah) * götür(-yç - 50, -yç - 20) -> Resim.yazı(buan)
    )
}
```
<!-- masaüstü: BuAn→buSaniye, Resim.düz→Resim.çizgi, Resim.sil→resimleriSil -->

Saatin kaç tane çizimden oluştuğunu hesabedebilir misin? Yanıt pek çok ülkedeki emeklilik yaşı! Neyse ki bu 65 çizimi yapmak hem de saniyede 40 kere tekrar tekrar bizim bilgisayarımızı hiç yormuyor! Peki belki de yok, yok 63 tane çizgi, bir çember ve bir kaç da yazı diyerek itiraz edeceksin. Çok haklısın. Ve tebrikler!

### Conway'in Yaşam Oyunu

İngilizce adıyla "The Game of Life" o kadar meşhur ki, bilgisayarcılar arasında basitçe Life yani Yaşam adıyla tanınır! Aslında o bir hücresel otomaton yani basit hücrelerden oluşan ve onların yerel etkileşimleri sayesinde kendi kendine devinen en basit program türlerinden biri. İngiltere doğumlu Amerika'da Princeton üniversitesinde matematik araştırmaları yapan John Horton Conway tarafından 1970 yılında icat edilmiş. Belki de keşfedilmiş demek lazım. Kimbilir. Sen ne dersin? [Vikipedi ansiklopedisinden bakabilirsin](https://tr.wikipedia.org/wiki/Conway%27in_Hayat_Oyunu).

Ana fikir çok basit. Basit bir avuç kurala göre hücreler canlanır ya da can verir. Her hücrenin sekiz komşusu var. Doğru mu? Canlı olanlara arkadaş diyelim. Bakın dört kural var:

- Canlı bir hücrenin ikiden az arkadaşı varsa canı çıkar. Canı sıkılmış sanki.
- Eğer iki ya da üç arkadaşı varsa hayatta kalır.
- Eğer başına üçten fazla arkadaş toplanırsa canı çıkar! Bilmem neden. Sanki çok kalabalık olmuş gibi.
- Cansız bir hücrenin tam üç tane arkadaşı varsa kendisi de canlanır. Allah'ın hakkı üç denir ya!

Bu yazılımcık `soldanKatla` adlı yöntemi kullanarak çok önemli bir kavram olan üst derece işlevlere örnek oluyor. Ne demek üst derece işlev? Başka işlevleri girdi olarak kabul eden onları kullanarak akıl almaz derecede becerikli olan komutlar. En başlarda gördüğümüz `için` yapısı yerine `soldanKatla` kullanarak bütün dünyayı baştan çiziveriyoruz. Bu arada bir de sağdanKatla var. O da esaslı bir işlev. Dünyayı temsil eden kümenin hücrelerinin hepsini işleyiveriyor.

Bu yaşam ya da hayat oyunu sıfır oyuncuyla oynanıyor! Çok sıkıcı mı dedin? Yok, çok ilginç aslında. Aslında sen çok önemlisin. Çünkü bu oyunun başlaması için en başta canlı hücrelere gerek var. Bunları sen belirleyebilirsin. Ama önce hazır bazı desenlerle başlamak daha kolay olur. `başlangıç` adlı komudu bul. Onun ikinci girdisi `desen`. Deseni seçmek için yapman gereken tek şey `seç` adındaki değeri değiştirmek. Birinciyle başlıyoruz. Ama sıfırdan ona kadar hepsini deneyebilirsin. Sonra hatta kendin de yeni desenler ekleyebilirsin. Bu simulasyonun hızını `oran` değerini değiştirerek ayarlayabilirsin.

```scala
silipSakla(); kalemRenginiKur(mavi)
// bu oyunun dünyası yani tahtası büyük bir kare. Kenarı KU uzunluğunda olsun
// Nasıl satranç tahtası 8x8, bu tahta da 128x128 kare.
dez KU = 128
// karenin kenarı kaplumbağanın on adımına denk

// ilk önce, bütün kareler cansız olmalı
den dünya = (0 |- KU * KU).soldanKatla(Sayılar())((x, y) => x :+ 0)
satıryaz(s"Dünyamızda $KU'in karesi yani ${dünya.boyu} tane hane var.")
yaz(s"Ekranımız ${(tuvalAlanı.eni / 10).sayıya} kare eninde ")
satıryaz(s"ve ${(tuvalAlanı.boyu / 10).sayıya} kare boyunda.")

dez oran = 5 // canlandırmayı yavaşlatmak için bunu arttır.
// En hızlısı 1. 40'a eşitlersen saniyede bir nesil ilerliyor yaklaşık olarak.
// Nasıl mı? Aşağıdaki canlandır adlı döngü komutu saniyede yaklaşık 40 kere yineleniyor.

dez gösterVeDur = yanlış // bunu doğru yaparsan deseni gösterip dururuz
dez sonundaDur = doğru // her desenin bir durağı var. Ondan sonra fazla bir şey değişmiyor.
// Ama, yine de çalışmaya devam etsin isterse, bunu yanlışa çevir.

// deseni seçelim:
dez seç = 1
// blok1 ve blok2 bir kaç füze yolluyor ve sonra 1000. nesil civarı gibi duruyor.
dez (desen, adı, durak) = seç eşle {
    durum 0 => (üçlüler, "üçlüler", 20)
    durum 1 => (kayGit, "kayGit", 500) /* makineli tüfek gibi */
    durum 2 => (esaslı, "esaslı", 1111) /* Yaklaşık 1000 nesil canlı sonra peryodik */
    durum 3 => (dokuzcanlı, "dokuzcanlı", 130) /* 131 nesil sonra can kalmıyor */
    durum 4 => (blok1, "blok1", 1200) //
    durum 5 => (blok2, "blok2", 1200) //
    durum 6 => (küçücük, "küçücük", 700) //
    durum 7 => (ü2a, "ü2a", 60) // üçlülere ek
    durum 8 => (ü2b, "ü2b", 60) // benzeri
    durum 9 => (dörtlü, "dörtlü", 30) // üçlü üretiyor
    durum _ => (tohum, "tohum", 2200) // ne muhteşem bir meşe palamudu!
}

dünya = başlangıç(dünya, desen)

yaz(s"$seç. desende ${desen.boyu} tane canlı kare var. Adı $adı.\nNesilleri sayalım: ")

den zaman = 0
dez z0 = buSaniye // şimdiki zamanı (geçmişte bir ana göre) anımsayalım
canlandır {
    dez nesil = zaman / oran + 1
    eğer (zaman % oran == 0) {
        Resim.sil()
        çizim(dünya)
        dünya = (0 |- KU * KU).soldanKatla(Sayılar())((x, y) => x :+ yeniNesil(dünya, y))
        yaz(s"$nesil ")
        eğer (gösterVeDur) durdur
    }
    zaman += 1
    eğer (sonundaDur && nesil == durak) {
        dez z1 = buSaniye - z0
        satıryaz(s"\n${yuvarla(z1, 2)} saniye geçti. Durduk.")
        durdur()
    }
}

// deseni kuralım
tanım başlangıç(v: Sayılar, desen: Dizin[(Sayı, Sayı)]) = desen.
    soldanKatla(v) {
        (x, y) => x.değiştir((y._1 + KU / 2) * KU + y._2 + KU / 2, 1)
    }

// yeni nesli bulalım
tanım yeniNesil(v: Sayılar, ix: Sayı) = {
    dez kural = Yöney(0, 0, 0, 1, 1, 0, 0, 0, 0, 0) // oyunun kuralları
    dez x = ix / KU; dez y = ix % KU
    dez t = (0 |- 3).soldanKatla(0)((st, i) => {
        st + (0 |- 3).soldanKatla(0)((s, j) => {
            dez xt = x + i - 1; dez yt = y + j - 1
            s + (eğer ((xt < 0) || (xt >= KU) || (yt < 0) || (yt >= KU)) 0 yoksa v(xt * KU + yt))
        })
    })
    eğer (v(ix) == 1) kural(t) yoksa { eğer (t == 3) 1 yoksa 0 }
}
// canlı kareleri çizelim. Can mavi çember içi kırmızı daire. Yarıçapı 5
dez yarıçap = 5
tanım çizim(v: Sayılar) = için (i <- 0 |- KU * KU)
    eğer (v(i) == 1) çiz(götür(
        (i / KU) * 2 * yarıçap - KU * yarıçap,
        (i % KU) * 2 * yarıçap - KU * yarıçap
    ) * kalemRengi(mavi) * boyaRengi(kırmızı) -> Resim.daire(yarıçap))

// Meşhur olmuş desenlerden birkaçı
tanım esaslı = Dizin((0, 1), (1, 0), (1, 1), (1, 2), (2, 2)) // orijinal adı: fpent
// İki küçücük grup var ve kolay kolay ölmüyor
tanım dokuzcanlı = Dizin((0, 1), (1, 0), (1, 1), (5, 0), (6, 0), (7, 0), (6, 2)) // diehard
tanım tohum = Dizin((0, 0), (1, 0), (1, 2), (3, 1), (4, 0), (5, 0), (6, 0))
// glider adlı meşhur üretken desen
tanım kayGit = Dizin((-18, 3), (-18, 4), (-17, 3), (-17, 4), (-8, 2), (-8, 3), (-8, 4), (-7, 1), (-7, 5),
    (-6, 0), (-6, 6), (-5, 0), (-5, 6), (-4, 3), (-3, 1), (-3, 5), (-2, 2), (-2, 3), (-2, 4),
    (-1, 3), (2, 4), (2, 5), (2, 6), (3, 4), (3, 5), (3, 6), (4, 3), (4, 7),
    (6, 2), (6, 3), (6, 7), (6, 8), (16, 5), (16, 6), (17, 5), (17, 6))
tanım blok1 = Dizin((0, 0), (2, 0), (2, 1), (4, 2), (4, 3), (4, 4), (6, 3), (6, 4), (6, 5), (7, 4))
tanım blok2 = Dizin((0, 0), (0, 3), (0, 4), (1, 1), (1, 4), (2, 0), (2, 1), (2, 4), (3, 2), (4, 0),
    (4, 1), (4, 2), (4, 4))
tanım küçücük = Dizin((-18, 0), (-17, 0), (-16, 0), (-15, 0), (-14, 0), (-13, 0), (-12, 0), (-11, 0), (-9, 0), (-8, 0),
    (-7, 0), (-6, 0), (-5, 0), (-1, 0), (0, 0), (1, 0), (8, 0), (9, 0), (10, 0),
    (11, 0), (12, 0), (13, 0), (14, 0), (16, 0), (17, 0), (18, 0), (19, 0), (20, 0))
tanım üçlüler = Dizin((0, 2), (0, 3), (0, 4), (0, -2), (0, -3), (0, -4),
    (-2, 0), (-3, 0), (-4, 0), (2, 0), (3, 0), (4, 0))
// üçlülerden dikey olanları bağlayalım
tanım ü2a = Dizin((0, 0), (0, 1), (0, -1)) ++ üçlüler
// öbür türlü, yani yatay olanları bağlayalım
tanım ü2b = Dizin((0, 0), (1, 0), (-1, 0)) ++ üçlüler
tanım dörtlü = Dizin((0, 0), (1, 0), (-1, 0), (0, 2)) // dokuzcanlı'nın altkümesi

// sepet sepet yumurta
// sakın beni unutma
// şimdilik bu kadar
// yaşamın tadını çıkar
```
<!-- masaüstü: silipSakla→silVeSakla, tuvalAlanı→tuvalSınırları, durdur→canlandırmayıDurdur, Resim.sil→resimleriSil -->

Masaüstünde bu örnek `çıktıyıSil` ile başlıyor; ikojo'da o komut yok, her çalıştırma çıktıyı zaten temizler. Aynı oyunun ikojo için uyarlanmış sürümü [Benzetim savı](/benzetim) sayfasında var.

### Düğüm açma oyunu

Bu oyun bize iki şey gösterecek: 1) Scala'nın bize sunduğu veri yapılarından Yöney ne işlere yarıyor. 2) Fareye tıklayıp bırakmadan sürüklersek neler yapabiliyoruz. Tabii daha da güzeli eğlenceli bir oyun yazabiliyoruz bu sayede.

Bu oyunu internetteki eski bir oyundan esinlenerek yazdık. Oyunun adı Planarity yani düzlemsellik. Mavi toplardan herhangi birinin üzerine tıklayıp bırakmadan tuvalde başka bir yere taşı. Göreceksin ki bağlı olduğu iki çizgi sanki lastik gibi hareket ediyor ve topu bırakmıyor. Bu bulmacanın amacı topları güzelce yerleştirerek çizgilerin birbirini kesmesine engel olmak. Yani bu düğümü çözmek. Çok zor sayılmaz. Biraz dene kolaylaşacak. Kırmızı kareye tıklarsan yeni bir düğüm oluşur.

Yazılımcığa bakarsan en başta KS adında bir değişmez (dez) göreceksin. Onu değiştirerek oyunun zorluğunu ayarlayabilirsin. Ne kadar büyütürsen o kadar zorlaşır! Bize ilham veren oyunun adı Planarity. [Daha çok bilgi için buna tıkla](https://en.wikipedia.org/wiki/Planarity).

```scala
// KS arttıkça oyun zorlaşır. Bir kenarda kaç tane nokta olsun?
dez KS = 4; dez AS = KS * KS
dez YÇ = 20 // bu da noktaların yarıçapı
durum sınıf Çizgi(n1: Nokta, n2: Nokta) { // her çizgi iki noktayı bağlar
    den çizgi = birDoğruÇiz(n1.x, n1.y, n2.x, n2.y) // bir doğru çizer
}
tanım birDoğruÇiz(llx: Kesir, lly: Kesir, urx: Kesir, ury: Kesir) = {
    dez (en, boy) = (urx - llx, ury - lly)
    dez r = götür(llx, lly) -> Resim.düz(en, boy)
    r.çiz
    r
}
// bütün çizgiler. boş küme olarak başlarız
den çizgiler = Yöney[Çizgi]()
// Noktayı tuvalde kaydıracağız. Yeri değişince ona bağlı çizgileri tekrar çizmemiz gerek
durum sınıf Nokta(den x: Kesir, den y: Kesir) {
    dez n = götür(x, y) * boyaRengi(mavi) -> Resim.daire(YÇ)
    n.çiz
    tanım yeniKonum(yeniX: Kesir, yeniY: Kesir) {
        x = yeniX; y = yeniY
        n.kondur(yeniX, yeniY)
    }
    // fareye tıklayıp çekince bu çalışacak
    n.fareyiSürükleyince { (mx, my) => { n.kondur(mx, my); x = mx; y = my; çizelim(çizgiler) } }
}
// Bütün noktaları (0,0) yani orijine üstüste koyalım. Merak etme birazdan dağıtacağız
silipSakla()
dez noktalar = (0 |- AS).soldanKatla(Yöney[Nokta]())((v, i) => { v :+ Nokta(0, 0) })

// çizgileri tanımlar ve noktalara bağlarız. Bir balık ağı gibi. KS * KS düğümlü
çizgiler = (0 |- AS).soldanKatla(Yöney[Çizgi]())(
    (çv, i) => {
        dez (x, y) = (i / KS, i % KS)
        dez çzg = eğer (y < KS - 1) { çv :+ Çizgi(noktalar(i), noktalar(i + 1)) } yoksa çv
        eğer (x < KS - 1) { çzg :+ Çizgi(noktalar(i), noktalar(i + KS)) } yoksa çzg
    })
serpiştir(noktalar) // noktaları yerleştir ve çizgileri çiz

// noktaları rastgele yerleştir
tanım serpiştir(hepsi: Yöney[Nokta]) {
    hepsi.herbiriİçin(nkt => nkt.yeniKonum(KS * YÇ * 6 * (rasgele - 0.5), KS * YÇ * 6 * (rasgele - 0.5)))
    çizelim(çizgiler)
}

// noktalar arasındaki çizgileri çizelim. Her çizgi, iki noktasının çemberine kadar gelsin
tanım çizelim(hepsi: Yöney[Çizgi]) {
    hepsi.herbiriİçin(çzg => {
        dez (x1, y1) = (çzg.n1.x, çzg.n1.y)
        dez (x2, y2) = (çzg.n2.x, çzg.n2.y)
        dez boy = karekökü(karesi(x2 - x1) + karesi(y2 - y1))
        dez (xr, yr) = (YÇ / boy * (x2 - x1), YÇ / boy * (y2 - y1))
        çzg.çizgi.sil
        çzg.çizgi = birDoğruÇiz(x1 + xr, y1 + yr, x2 - xr, y2 - yr)
    })
}

// kırmızı kareye basınca yeni bir düğümle baştan başlarız
tanım kare(x: Kesir, y: Kesir, en: Kesir) = {
    dez k = götür(x, y) * boyaRengi(kırmızı) -> Resim.dikdörtgen(en, en)
    k.çiz
    k
}
dez b = kare(-KS * 35, -KS * 35, 20)
b.fareyeTıklayınca { (x, y) => serpiştir(noktalar) }
```
<!-- masaüstü: silipSakla→silVeSakla, Resim.düz→Resim.çizgi -->
