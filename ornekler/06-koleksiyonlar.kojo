// Koleksiyonların Türkçesi: dizi, küme, eşlek, miskin dizin

dez sayılar = Dizi(5, 3, 8, 1, 3)

satıryaz("sıralı  : " + sayılar.sıralı.yazıYap(", "))
satıryaz("toplam  : " + sayılar.topla)
satıryaz("en irisi: " + sayılar.enİrisi)
satıryaz("çiftler : " + sayılar.ele(_ % 2 == 0))
satıryaz("iki katı: " + sayılar.işle(_ * 2))
satıryaz("tekrarsız: " + sayılar.yinelemesiz)

// küme işlemleri
dez a = Küme(1, 2, 3)
dez b = Küme(2, 3, 4)
satıryaz("kesişim : " + a.kesişim(b))
satıryaz("bileşim : " + a.bileşim(b))

// eşlek (sözlük)
dez yaşlar = Eşlek("Ayşe" -> 9, "Mehmet" -> 10)
satıryaz("Ayşe    : " + yaşlar.al("Ayşe").alYoksa(0))
satıryaz("anahtarlar: " + yaşlar.anahtarKümesi)

// miskin dizin: SONSUZ bir dizi, ama sadece istediğin kadarı hesaplanır
satıryaz("ilk 5 kare : " + MiskinDizin.sayalım(1).işle(x => x * x).al(5).dizine)
satıryaz("3'ün ilk 4 katı: " + MiskinDizin.sayalım(1).ele(_ % 3 == 0).al(4).dizine)

// Türkçe i/İ kuralı doğru çalışıyor
satıryaz("istanbul -> " + "istanbul".büyükHarfe)
satıryaz("IRMAK    -> " + "IRMAK".küçükHarfe)
