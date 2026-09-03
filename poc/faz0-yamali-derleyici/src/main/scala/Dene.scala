// Türkçe anahtar kelimelerle yazılmış duman testi. Stok derleyiciyle
// DERLENMEZ; yamalı scala-tr derleyicisi ister. Kapsananlar: nesne (object),
// tanım (def), dez (val), den (var), eğer/yoksa (if/else), doğru/yanlış
// (true/false), için (for), yineleDoğruKaldıkça (while), sınıf (class),
// yayar (extends), özellik (trait), baskın (override), yeni (new), eşle/durum
// (match/case).
nesne Dene {
  özellik Selamcı {
    tanım selam: String
  }

  sınıf TürkçeSelamcı yayar Selamcı {
    baskın tanım selam = "merhaba"
  }

  tanım main(args: Array[String]): Unit = {
    dez selamcı = yeni TürkçeSelamcı
    den toplam = 0
    için (i <- 1 to 4) toplam += i
    yineleDoğruKaldıkça (toplam > 10) toplam -= 1

    dez sonuç = eğer (doğru) selamcı.selam yoksa "olamaz"
    dez etiket = toplam eşle {
      durum 10 => "on"
      durum _  => "başka"
    }
    // Dikkat: $yanlış OLMAZ -- interpolasyonda $ sonrası tanımlayıcı beklenir,
    // yamalı derleyici yanlış'ı keyword (false) olarak lex eder. ${...} gerekir.
    println(s"$sonuç, toplam=$toplam ($etiket), yanlış=${yanlış}")
  }
}
