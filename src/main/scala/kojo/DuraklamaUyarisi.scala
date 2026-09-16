/*
 * Copyright (C) 2026 Bülent Başaran <bulent2k2@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package kojo

import org.scalajs.dom.document

import scala.scalajs.js

/**
 * `durakla` ile resim çizimini birlikte kullanan betiğe çıktı panelinden bir
 * kez not düşer (sorun #73).
 *
 * KUSUR: masaüstünde `durakla(n)` HER ŞEYİ bekletir (`Thread.sleep`).
 * Tarayıcıda ana iş parçacığını bloklamak yok, o yüzden `durakla` yalnız
 * KAPLUMBAĞA KOMUT KUYRUĞUNA bir bekleme koyuyor (`Turtle.pause` ->
 * `sıraya(Pause)`). Resim çağrıları kuyruğa hiç girmiyor: `Picture.draw`
 * doğrudan `realDraw` çağırıyor. Yani baştan sona resimle çizen bir masaüstü
 * betiği burada DERLENİYOR, hata vermiyor, ama bütün adımlar tek karede olup
 * bitiyor -- ekranda yalnız son hâl kalıyor.
 *
 * NEDEN DAVRANIŞI DEĞİL DE SESSİZLİĞİ DÜZELTİYORUZ: davranış bilinçli ve
 * tarayıcıda başka türlüsü yapılamaz. Resim çağrılarını da kuyruğa almak
 * `çiz`i eşzamansız yapardı ve var olan bütün yazılımcıkları etkilerdi
 * (sorun #73'teki 3. seçenek; Issue'nun kendisi de riskini kazancından fazla
 * buluyor). Kusurun asıl zararı SESSİZ olması: derleyici susuyor, CI yeşil,
 * yalnız çıktı yanlış. Not onu sessiz olmaktan çıkarıyor.
 *
 * ÖLÇÜT: aynı koşuda hem resim çizilmiş hem `durakla` çağrılmış olması.
 * Sırası önemli değil -- `angles.kojo` çiz/durakla/çiz diye gidiyor,
 * `robot.kojo` önce gövdeyi çizip sonra durakla/götür döngüsüne giriyor;
 * ikisi de aynı kusurdan vuruluyor. Bilinen üç betikte ölçüldü:
 *   angles.kojo          7 durakla + resim  -> not düşüyor   (doğru)
 *   robosim/robot.kojo   3 durakla + resim  -> not düşüyor   (doğru)
 *   sprite-animation.kojo 2 durakla, resim YOK -> susuyor    (doğru -- orada
 *     konumuDeğiştir/giysiyiBüyült/birsonrakiGiysi kuyruğa giriyor, yani
 *     durakla düzgün çalışıyor; not düşmek yanlış alarm olurdu)
 *
 * `forPic` kaplumbağalar sayılmıyor: `Resim { durakla(1) }` gövdesindeki
 * duraklama O RESMİN kendi kuyruğunda ve orada gerçekten geciktiriyor.
 */
object DuraklamaUyarısı {
  private[kojo] val metin =
    "Not: durakla resim çizimini geciktirmez -- yalnız kaplumbağa komutlarını " +
      "bekletir, resimler hemen çizilir. Resimlerle adım adım ilerlemek için " +
      "fareyeTıklayınca ile bir düğme ya da canlandır kullanın."

  private var resimÇizildi = false
  private var duraklandı = false
  private var notDüşüldü = false

  private[kojo] def uyarıldıMı: Boolean = notDüşüldü

  /** `Picture.draw` çağırıyor. */
  private[kojo] def resimÇizimi(): Unit = {
    resimÇizildi = true
    belkiNotDüş()
  }

  /** `Turtle.pause` çağırıyor -- yalnız gerçek (forPic olmayan) kaplumbağa için. */
  private[kojo] def duraklama(): Unit = {
    duraklandı = true
    belkiNotDüş()
  }

  /**
   * Yeni koşum. `Turtle.clear` çağırıyor, yani `sil()`/`silVeSakla()` diye
   * başlayan betikler her koşuda notu yeniden görüyor. EŞZAMANLI kol
   * (`realClear` değil): `clear()` betiğin gövdesinde, kuyruk işlemeye
   * başlamadan koşuyor -- `realClear`e bağlasaydık bayraklar koşumun
   * ORTASINDA sıfırlanır ve aynı koşuda ikinci bir not çıkabilirdi.
   */
  private[kojo] def unut(): Unit = {
    resimÇizildi = false
    duraklandı = false
    notDüşüldü = false
  }

  private def belkiNotDüş(): Unit =
    if (resimÇizildi && duraklandı && !notDüşüldü) {
      notDüşüldü = true
      paneleYaz(metin)
    }

  /**
   * Çıktı paneline yazar; panel yoksa (tarayıcı dışı koşum, sınamalar, dev
   * harness'ı) konsola düşer. DOM sözleşmesi TurkishTurtle.paneleYaz ile aynı
   * -- id="output" -- ama onun "açık satır" durumuna DOKUNMUYORUZ: kendi
   * div'imizi ekliyoruz, ki oradaki kural (son çocuk benim değilse yeni div
   * aç) bunu zaten bekliyor.
   */
  private def paneleYaz(metin: String): Unit = {
    val panel = if (js.typeOf(js.Dynamic.global.document) == "undefined") null else document.getElementById("output")
    if (panel == null) {
      js.Dynamic.global.console.warn(metin)
    }
    else {
      val satır = document.createElement("div")
      satır.appendChild(document.createTextNode(metin))
      panel.appendChild(satır)
      panel.scrollTop = panel.scrollHeight - panel.clientHeight
    }
  }
}
