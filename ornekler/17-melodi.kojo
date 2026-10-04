// MELODİ: notaÇal ile bir ezgi, notaSus ile es, kalanNotaSüresi ile toplam süre
// https://ikojo.fly.dev adresine yapıştırıp Çalıştır'a bas.
//
// Notalar (MIDI perdesi, süre milisaniye) bir listede duruyor; melodiyiÇal
// listeyi sırayla çalıyor. 60 orta do, 72 bir oktav yukarısı.
//
// NEDEN notaÇal'dan sonra BEKLEMİYORUZ: notaÇal (masaüstündeki playNote gibi)
// notayı bir ZAMAN ÇİZELGESİNE yazıp hemen döner. Döngü birkaç milisaniyede
// biter, melodi ise yarım dakika çalar. Bu yüzden melodinin ne kadar süreceğini
// `buAn - başlangıç` ile ölçemezsin: o, notaları sıraya koyma süresidir.
// Çalacak notaların toplam süresini `kalanNotaSüresi` söyler (milisaniye;
// tarayıcı sesi açtıktan sonra birkaç ms oynayabilir, o yüzden "yaklaşık").
//
// TEKRAR ÇALIŞTIRMA: notalar sıraya yazıldığı için melodi çalarken Çalıştır'a
// yeniden basarsan ikinci melodi birincinin ARDINA eklenir. Betik bu yüzden
// başta stopNotePlayer() ile sıradaki notaları siler.
//
// ES: perde olarak -1 yazılan (es, süre) çifti notaSus'a gidiyor: ses
// çıkarmadan o kadar bekler.
//
// AKOR: aynı anda birkaç nota çalmak için akorÇal ve beraberÇal var; melodinin
// altına bas eklemek gibi. Masaüstünde yok, yalnız iKojo'da:
//
//     akorÇal(Dizi(48, 52, 55), 1000)                       // do majör, 1 sn
//     beraberÇal(Dizi((67, 500), (43, 1000)))               // melodi + bas
//
// Sonraki nota, grubun EN UZUN notası bitince başlar.

tanım melodi(): Diz[(Sayı, Sayı)] = {
  dez bir = 500
  dez iki = 2 * bir
  dez v8 = bir / 2
  dez v16 = v8 / 2
  dez es = -1
  Diz(
    (60, iki), (64, iki),
    //
    (67, iki - v8), (66, v16), (67, v16),
    (69, v8), (67, v8),
    (65, v8), (64, v8),

    (64, iki - v8), (63, v16), (64, v16),
    (65, v8), (64, v8), (62, v8), (60, v8),

    (59, bir - v8), (60, v16), (62, v16), (60, bir),
    (72, bir), (67, v8 + v16), (67, v16),
    (67, bir), (64, v8), (es, v8),

    (60, v8), (64, v8), (67, v8), (72, v8),
    (72, v8), (69, v8), (65, bir + v8),
    (65, v8), (67, v8), (69, v8),
    (69, v8), (67, v8), (64, bir + v8),
    (64, bir - v16), (62, v16), (60, bir),
    (76, bir - v16), (76, v16), (76, iki + bir + v8),
    (74, v16), (72, v16), (71, v16), (72, v16), (71, v16), (69, v16), (67, v16),
    (69, v16), (67, v16), (65, v16),
    (64, v8), (65, v16), (67, v16), (69, v16), (71, v16), (72, v16), (69, v16),
    (69, v16), (67, v16), (65, v16), (64, v16),
    (64, v16), (62, v16), (60, v16), (59, v16),
    (60, bir), (64, iki), (65, bir), (61, bir),
    (62, iki - v8), (62, v16), (61, v16), (62, v8), (64, v8),
    (65, bir), (65, iki - v8), (69, v8), (67, v8), (65, v8),
    (63, bir), (64, iki), (67, iki + v8), (69, v8), (71, v8), (72, v8),
    //
    (72, v8), (69, v8), (66, iki), (76, bir),
    // arada majör gam aşağı (altıncıdan tabana, yedinci, taban)
    (74, v8), (72, v8), (71, v8), (69, v8), (67, v8), (66, v8),
    (67, bir)
  )
}

tanım melodiyiÇal(parça: Diz[(Sayı, Sayı)]): Birim = {
  için ((nota, süre) <- parça) {
    eğer (nota < 0) notaSus(süre) yoksa notaÇal(nota, süre)
  }
}

stopNotePlayer()
notaÇalgısınıKur(Çalgı.Piyano)
melodiyiÇal(melodi())
satıryaz(s"Melodi yaklaşık ${kalanNotaSüresi / 1000.0} saniye sürecek")
