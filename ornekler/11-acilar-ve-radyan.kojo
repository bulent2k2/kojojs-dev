// Açı nedir, radyan ne demek? Devinimli çizimle gözümüzle görerek anlayalım.
// Tam bir dönüş = 360 derece = 2π radyan.
//
// Bu, masaüstündeki samples/tr/angles.kojo'nun ikojo (tarayıcı) sürümü.
//
// NEDEN AYNISI DEĞİL: masaüstü sürüm adımları durakla(1.3) ile ayırıyor.
// ikojo'da durakla YALNIZ kaplumbağa komut kuyruğuna bekleme ekler; bu
// yazılımcık ise baştan sona RESİM çiziyor (çiz / .sil / .döndür), ve resim
// çağrıları kuyruğa girmeden hemen çalışır. Yani masaüstü betiği burada
// derleniyor ama duraklamaların hiçbiri işlemiyor: bütün adımlar tek karede
// olup bitiyor, ekranda yalnız son hâl kalıyor.
//
// Çözüm: geçişleri ZAMANA değil bir DÜĞMEye bağlamak. Aşağıdaki "Sonraki"
// düğmesine tıkla -- ya da boşluk / sağ ok tuşuna bas.
//
// Esin kaynakları:
// (1) C. K. Raju, Sınırsız Kalkülüs (Calculus without limits)
// (2) http://1ucasvb.tumblr.com/

dez yçBoyu = 200.0          // yarıçapın uzunluğu
dez çeyrekYÇ = yçBoyu / 4
dez renk = mavi
dez açıRengi = Renk(0, 204, 51)
dez eskiAçıRengi = açıkGri
dez yayRengi = gri

// ---- Şekiller (masaüstü sürümdekilerle aynı) ------------------------------

tanım yarıçapıÇiz(açı: Sayı) = kalemRengi(renk) * döndür(açı) -> Resim.dizi(
    boyaRengi(renk) -> Resim.daire(3),
    Resim.yatay(yçBoyu),
    götür(yçBoyu, 0) * boyaRengi(renk) -> Resim.daire(3)
)

tanım eğriYçÇiz(başı: Kesir, açı: Kesir) = kalemRengi(renk) * döndür(başı) -> Resim.dizi(
    götür(yçBoyu, 0) * boyaRengi(renk) -> Resim.daire(3),
    Resim.yay(yçBoyu, açı),
    döndür(açı) * götür(yçBoyu, 0) * boyaRengi(renk) -> Resim.daire(3)
)

tanım yayÇiz(açı: Sayı) = kalemRengi(yayRengi) -> Resim.yay(yçBoyu, açı)

tanım açıÇiz(başı: Kesir, açı: Kesir) = döndür(başı) -> Resim.dizi(
    Resim.yatay(yçBoyu),
    döndür(açı) -> Resim.yatay(yçBoyu),
    Resim.yay(yçBoyu / 4, açı)
)

tanım doğruÇiz(x1: Kesir, y1: Kesir, x2: Kesir, y2: Kesir) = {
    dez uzunluk = karekökü(karesi(x2 - x1) + karesi(y2 - y1))
    dez açı = tanjantınAçısı((y2 - y1) / (x2 - x1))
    götür(x1, y1) * döndür(dereceye(açı)) -> Resim.yatay(uzunluk)
}

tanım sayıdanDereceye(s: Sayı) = s.kesire.dereceye
tanım dereceye(k: Kesir) = k.dereceye

// ---- Tuvaldeki resimler ---------------------------------------------------

den birYay = yayÇiz(0)
den birYarıçap = yarıçapıÇiz(0)
den yçYazısı = götür(yçBoyu / 2, -5) -> Resim.yazı("yarıçap", 20)
den birAçı: Resim = Resim.yay(0, 0)
den açıYazısı: Resim = Resim.yazı("", 20)
den işaret: Resim = Resim.yatay(0)
den işaret2: Resim = Resim.yatay(0)

tanım radyanAçıÇiz(kaçRadyan: Sayı) {
    eğer (kaçRadyan != 1) {
        birYarıçap = eğriYçÇiz(sayıdanDereceye(kaçRadyan - 1), sayıdanDereceye(1))
        çiz(birYarıçap)
    }
    birAçı.kalemRenginiKur(eskiAçıRengi)
    birAçı = kalemRengi(açıRengi) -> açıÇiz(0, sayıdanDereceye(kaçRadyan))
    çiz(birAçı)
    açıYazısı.sil()
    işaret.sil()
    işaret2.sil()
    açıYazısı = götür(-18, -yçBoyu / 4) -> Resim.yazı(s"$kaçRadyan radyan", 20)
    çiz(açıYazısı)
    eğer (kaçRadyan < 4) {
        işaret = doğruÇiz(0, -çeyrekYÇ, çeyrekYÇ * kosinüs(30.radyana), çeyrekYÇ * sinüs(30.radyana))
        çiz(işaret)
        işaret2 = doğruÇiz(0, -çeyrekYÇ, yçBoyu * kosinüs(30.radyana), yçBoyu * sinüs(30.radyana))
        çiz(işaret2)
    }
}

tanım piAçısınınKatınıÇiz(katı: Sayı) {
    dez pi = piSayısı
    birYarıçap = eğer (katı == 1)
        eğriYçÇiz(sayıdanDereceye(3), dereceye(pi - 3))
    yoksa
        eğriYçÇiz(sayıdanDereceye(6), dereceye(2 * pi - 6))
    çiz(birYarıçap)
    birAçı.kalemRenginiKur(eskiAçıRengi)
    birAçı = kalemRengi(açıRengi) -> açıÇiz(0, dereceye(pi * katı))
    çiz(birAçı)
    açıYazısı.sil()
    açıYazısı = götür(-18, -yçBoyu / 4) -> Resim.yazı(s"${katı}π radyan", 20)
    çiz(açıYazısı)
}

// ---- Düğme ----------------------------------------------------------------
// ikojo'da hazır bir düğme aracı yok; düğme de bir RESİM, tıklaması
// fareyeTıklayınca ile bağlanıyor.

dez düğmeEni = 170.0
dez düğmeBoyu = 46.0
dez düğmeX = -yçBoyu - 120
dez düğmeY = -yçBoyu - 40

den adım = 0
den süpürmeAçısı = 0
den süpürüyor = yanlış

dez düğme = götür(düğmeX, düğmeY) -> Resim.dizi(
    kalemRengi(gri) * boyaRengi(Renk(238, 238, 238)) -> Resim.dikdörtgen(düğmeEni, düğmeBoyu),
    götür(22, 15) -> Resim.yazı("Sonraki  >", 20)
)

den anlatım = götür(düğmeX, düğmeY - 34) -> Resim.yazı("", 18)

tanım anlat(metin: Yazı) {
    anlatım.sil()
    anlatım = götür(düğmeX, düğmeY - 34) -> Resim.yazı(metin, 18)
    çiz(anlatım)
    düğme.öneAl()
}

// ---- Adımlar --------------------------------------------------------------

tanım sonrakiAdım() {
    // Süpürme sürerken tıklamayı yutuyoruz. (Erken çıkış YOK: Scala'da
    // return kullanmıyoruz, bütün gövdeyi eğer'in içine alıyoruz.)
    eğer (!süpürüyor) {
    adım eşle {
        durum 1 => {
            süpürmeAçısı = 0
            süpürüyor = doğru
            anlat("Yarıçapı çember boyunca döndürüyoruz...")
        }
        durum 2 => {
            yçYazısı.kondur(yçBoyu + 5, yçBoyu / 2 + 10)
            birYarıçap.döndürMerkezli(-90, yçBoyu, 0)
            anlat("Yarıçapı çemberin üstüne yatırdık.")
        }
        durum 3 => {
            birYarıçap.sil()
            birYarıçap = eğriYçÇiz(0, dereceye(1))
            çiz(birYarıçap)
            anlat("Yay uzunluğu = yarıçap olan açı: işte 1 RADYAN.")
        }
        durum 4 => { radyanAçıÇiz(1); anlat("1 radyan ≈ 57.3 derece.") }
        durum 5 => { radyanAçıÇiz(2); anlat("2 radyan.") }
        durum 6 => { radyanAçıÇiz(3); anlat("3 radyan -- π'ye az kaldı.") }
        durum 7 => { piAçısınınKatınıÇiz(1); anlat("π radyan = yarım dönüş = 180 derece.") }
        durum 8 => { radyanAçıÇiz(4); anlat("4 radyan.") }
        durum 9 => { radyanAçıÇiz(5); anlat("5 radyan.") }
        durum 10 => { radyanAçıÇiz(6); anlat("6 radyan -- tam dönüşe az kaldı.") }
        durum 11 => {
            piAçısınınKatınıÇiz(2)
            anlat("2π radyan = tam dönüş = 360 derece. Bitti!")
            düğme.sil()
        }
        durum _ => {}
    }
    adım += 1
    }
}

// ---- Kurulum --------------------------------------------------------------

silVeSakla()
yakınlaştırmayıKapat()

çiz(birYarıçap)
çiz(yçYazısı)
çiz(düğme)
adım = 1
anlat("Bu bir yarıçap. Devam etmek için Sonraki'ye tıkla.")

düğme.fareyeTıklayınca { (x, y) => sonrakiAdım() }
tuşaBasınca { t =>
    eğer (t == tuşlar.boşluk || t == tuşlar.sağ) sonrakiAdım()
}

// Süpürme adımı zamanla değil KAREYLE ilerliyor: canlandır her karede bir
// çalışır, tarayıcıda doğru olan yol bu.
canlandır {
    eğer (süpürüyor) {
        süpürmeAçısı += 2
        birYarıçap.döndür(2)
        birYay.sil()
        birYay = yayÇiz(süpürmeAçısı)
        çiz(birYay)
        düğme.öneAl()
        eğer (süpürmeAçısı >= 360) {
            süpürüyor = yanlış
            anlat("Bir tam dönüş: 360 derece.")
        }
    }
}
