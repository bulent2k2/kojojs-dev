package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * NON_ZERO üçgenlemesinin DOĞRU olduğunu çiviliyor.
 *
 * Sav neden alan: beş köşeli yıldızın NON_ZERO dolgu alanı kapalı formülle
 * hesaplanabiliyor, yani "PIXI ne çiziyorsa o doğrudur" demeden bağımsız bir
 * ölçütümüz var. R için iç yarıçap r = R*cos72/cos36 ve
 *
 *   alan = (5/2)*r^2*sin72  +  5 * (1/2)*(2r*sin36)*(R - r*cos36)
 *
 * R=100 için 11225.6994. earcut aynı girdide 21266.3 veriyor (1.894x fazla) --
 * yani bu sav earcut'a dönülürse kırmızı yanar.
 */
class UcgenleyiciTest extends AnyFunSuite with Matchers {

  /** Kendini kesen beş köşeli yıldız: 144 derecelik adımlarla. */
  private def yıldız(r: Double): Array[Double] = {
    val p = Array.newBuilder[Double]
    for (i <- 0 until 5) {
      val a = math.Pi / 2 + i * 4 * math.Pi / 5
      p += r * math.cos(a); p += r * math.sin(a)
    }
    p.result()
  }

  /** Kapalı formül -- sınamanın bağımsız ölçütü. */
  private def yıldızAlanı(R: Double): Double = {
    val r = R * math.cos(math.toRadians(72)) / math.cos(math.toRadians(36))
    val beşgen = 2.5 * r * r * math.sin(math.toRadians(72))
    val uç = 0.5 * (2 * r * math.sin(math.toRadians(36))) * (R - r * math.cos(math.toRadians(36)))
    beşgen + 5 * uç
  }

  private def çember(n: Int, r: Double): Array[Double] = {
    val p = Array.newBuilder[Double]
    for (i <- 0 until n) {
      val a = i * 2 * math.Pi / n
      p += r * math.cos(a); p += r * math.sin(a)
    }
    p.result()
  }

  test("libtess sınama harnessine ULAŞIYOR") {
    // jsDependencies bağlantısının kendisini koruyor: kütüphane testlere
    // gelmezse Üçgenleyici sessizce earcut'a düşer ve alan savları da
    // anlamsızlaşır (o durumda kullanılabilir false döner, çökmez).
    withClue("build.sbt'deki ProvidedJS / \"libtess.cat.js\" bağlantısı kopmuş olabilir -- ") {
      Üçgenleyici.kullanılabilir should be(true)
    }
  }

  test("cephedeki enum adları GERÇEKTEN çözülüyor") {
    // Yanlış yazılmış bir enum adı `undefined` olur ve gluTessCallback onu
    // sessizce kabul eder: geri çağırma hiç kaydolmaz, hata yutulur, tip savı
    // hiç koşmaz. Yani cephe adları sessiz kırılma noktası.
    import libtessjs.libtess
    withClue("GLU_TESS_ERROR çözülmüyor: hatalar sessizce yutulur -- ") {
      scala.scalajs.js.isUndefined(libtess.gluEnum.GLU_TESS_ERROR) should be(false)
    }
    withClue("GL_TRIANGLES çözülmüyor: ilkel tip savı hep sessiz kalır -- ") {
      scala.scalajs.js.isUndefined(libtess.primitiveType.GL_TRIANGLES) should be(false)
    }
    withClue("GLU_TESS_WINDING_NONZERO çözülmüyor: sarım kuralı hiç kurulmaz -- ") {
      scala.scalajs.js.isUndefined(libtess.windingRule.GLU_TESS_WINDING_NONZERO) should be(false)
    }
  }

  test("kendini kesen yıldız: alan kapalı formülle uyuşuyor (earcut 1.894x fazla verirdi)") {
    val ü = Üçgenleyici.nonzero(yıldız(100))
    val beklenen = yıldızAlanı(100)
    beklenen shouldBe 11225.6994 +- 0.001 // formülün kendisi de sabitlensin
    withClue(s"NON_ZERO alanı tutmuyor; earcut'a mı düştük? üçgen=${ü.length / 6} -- ") {
      Üçgenleyici.alan(ü) shouldBe beklenen +- 0.01
    }
  }

  test("basit çokgen: alan bilinen değerle uyuşuyor") {
    // 720 kenarlı çember; alan pi*r^2'ye yakınsıyor
    val ü = Üçgenleyici.nonzero(çember(720, 50))
    Üçgenleyici.alan(ü) shouldBe (math.Pi * 50 * 50) +- 1.0
  }

  test("kare: üçgenlerin alanı tam kenar^2") {
    val kare = Array(0.0, 0.0, 100.0, 0.0, 100.0, 100.0, 0.0, 100.0)
    Üçgenleyici.alan(Üçgenleyici.nonzero(kare)) shouldBe 10000.0 +- 1e-9
  }

  test("üçgenler GERÇEKTEN üçlü: dizi uzunluğu altının katı") {
    val ü = Üçgenleyici.nonzero(yıldız(100))
    ü.length % 6 should be(0)
    ü.length should be > 0
  }

  test("alan kaplamayan girdiler boş dönüyor") {
    Üçgenleyici.nonzero(Array.empty[Double]) shouldBe empty
    Üçgenleyici.nonzero(Array(0.0, 0.0)) shouldBe empty
    Üçgenleyici.nonzero(Array(0.0, 0.0, 10.0, 10.0)) shouldBe empty
    withClue("üst üste binen noktalar alan kaplamıyor -- ") {
      Üçgenleyici.alan(Üçgenleyici.nonzero(Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))) shouldBe 0.0 +- 1e-9
    }
  }

  test("tan-theta yolu: asimptotlarda kesişen 241 nokta üçgenleniyor") {
    val p = Array.newBuilder[Double]
    var x = -12.0
    while (x <= 12 + 1e-9) { p += x; p += math.tan(x); x += 0.1 }
    val ü = Üçgenleyici.nonzero(p.result())
    withClue("kendini kesen gerçek betik yolu üçgenlenemedi -- ") {
      ü.length should be > 0
    }
    // earcut aynı yolda 25 kat fazla alan dolduruyordu (2812.9 / 112.3)
    Üçgenleyici.alan(ü) shouldBe 112.3 +- 5.0
  }
}
