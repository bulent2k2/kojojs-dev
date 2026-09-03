// Geometri: Nokta, Dikdörtgen ve Yöney2B

dez n1 = Nokta(0, 0)
dez n2 = Nokta(3, 4)
satıryaz("uzaklık : " + n1.uzaklığı(n2))
satıryaz("açı     : " + n1.açısı(n2))
satıryaz("taşınmış: " + n2.taşınmış(1, 1).yazıya)

// desen eşleme
dez Nokta(x, y) = n2
satıryaz("x=" + x + " y=" + y)

// tuvalin sınırları bir Dikdörtgen
dez sınır = tuvalSınırları
satıryaz("tuval eni : " + sınır.eni)
satıryaz("tuval boyu: " + sınır.boyu)
satıryaz("merkez    : " + sınır.merkezi.yazıya)

// yöney (vektör)
dez h = Yöney2B(3, 4)
satıryaz("boyu       : " + h.boyu)
satıryaz("birim yöney: " + h.boyunuBirYap.boyu)
satıryaz("45 dereceden: " + Yöney2B.açıdan(45).x)
