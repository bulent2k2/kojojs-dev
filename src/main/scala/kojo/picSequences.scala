package kojo

import com.vividsolutions.jts.geom.Geometry
import kojo.PicCache.freshPics
import kojo.doodle.Color
import pixiscalajs.PIXI

import scala.concurrent.Future

abstract class BasePicSequence(val pics: Seq[Picture]) extends Picture with ReadyPromise {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  val tnode = new PIXI.Container()

  lazy val childrenReady: Future[Unit] = {
    val futures = pics map (_.ready)
    futures.reduce { (f1, f2) => for (_ <- f1; _ <- f2) yield () }
  }

  // Note - pic sequences get ready only after draw is called on them
  def layoutChildren(): Unit
  def layout(): Unit = {
    layoutChildren()
    makeDone()
  }

  def realDraw(): Unit = {
    pics.foreach { p =>
      p.draw()
      tnode.addChild(p.tnode)
    }
    kojoWorld.addLayer(tnode)
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    // YERLEŞİM YALNIZ BİR KEZ. `çiz(g); ...; çiz(g)` kalıbı öteki resim türlerinde
    // çalışıyor, grupta çalışmıyordu: childrenReady TAMAMLANMIŞ bir future olduğu için
    // foreach ikinci çizimde de koşuyor, layout() -> makeDone() zaten tamamlanmış söze
    // ikinci kez success diyor ve IllegalStateException atıyor. Üstelik SESSİZCE: hata bir
    // future geri çağrısının içinde, betiğe hiç ulaşmıyor, yalnız konsola düşüyor (#121).
    //
    // NEDEN BURADA, makeDone'u idempotent yapmak DEĞİL: ikinci koşunun asıl zararı
    // istisna değil, layoutChildren'ın yeniden koşması. Altı alt sınıf için zararsız
    // (ölçüldü: HPics ve VPicsCentered, 3 çocuk, konumlar 1./2./3. uygulamada ~3e-15
    // içinde aynı -- offset BAĞIL ve formül bounds'u yeniden okuyup delta hesaplıyor, yani
    // bir koşuda yakınsıyor; tam sıfır değil, toplama sırası yuvarlamayı oynatıyor). Ama BatchPics'in layoutChildren'ı `pics.tail.invisible()` diyor ve
    // showNext ilerlemişse GÖRÜNÜRLÜĞÜ SIFIRLIYOR -- ölçüldü:
    //   ilk çizim            true,false,false
    //   showNext ilerletince false,true,false
    //   layoutChildren yine  false,false,false   <- hiçbiri görünmüyor, currPicIndex=1
    // Yani hiçbiri görünmeyen bir ara oluşuyor ve pics(1) atlanıyor. Bu bugün de
    // oluyordu (istisna layoutChildren'dan SONRA atılıyor); burada kapatmak ikisini
    // birden kapatıyor. makeDone'un "tam bir kez" sözleşmesi de bozulmamış kalıyor:
    // orada patlaması, beklenmedik bir yerin onu ikinci kez çağırdığının işareti.
    //
    // İki çizim ilk yerleşimden ÖNCE gelirse de doğru: iki geri çağrı da kaydolur,
    // birincisi made'i kurar, ikincisi atlar.
    childrenReady.foreach { _ =>
      if (!made) layout()
    }
  }

  def initGeom() = {
    var pg = pics(0).picGeom
    pics.tail.foreach { pic =>
      pg = pg union pic.picGeom
    }
    pg
  }

  def setFillColor(c: Color): Unit = {
    pics.foreach { p =>
      p.setFillColor(c)
    }
  }

  // Boyayı da ilet -- bkz. PicTransformer.setFillPaint'teki açıklama.
  override def setFillPaint(b: Boya): Unit = {
    pics.foreach { p =>
      p.setFillPaint(b)
    }
  }

  def setPenColor(c: Color): Unit = {
    pics.foreach { p =>
      p.setPenColor(c)
    }
  }

  def setPenThickness(t: Double): Unit = {
    pics.foreach { p =>
      p.setPenThickness(t)
    }
  }

  def erase(): Unit = {
    kojoWorld.removeLayer(tnode)
  }

  def picsCopy = pics.map { _.copy }
}

object GPics {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new GPics(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new GPics(freshPics(pics))
}

class GPics(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {}
  def copy = new GPics(picsCopy)
}

object GPicsCentered {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new GPicsCentered(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new GPicsCentered(freshPics(pics))
}

class GPicsCentered(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    var prevPic: Option[Picture] = None
    pics.foreach { pic =>
      prevPic match {
        case Some(ppic) =>
          val pbounds = ppic.bounds
          val bounds = pic.bounds
          val tx = pbounds.x - bounds.x + (pbounds.width - bounds.width) / 2
          val ty = pbounds.y - bounds.y + (pbounds.height - bounds.height) / 2
          pic.offset(tx, ty)
        case None =>
      }
      prevPic = Some(pic)
    }
  }

  def copy = new GPicsCentered(picsCopy)
}

object HPics {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new HPics(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new HPics(freshPics(pics))
}

class HPics(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    var prevPic: Option[Picture] = None
    pics.foreach { pic =>
      prevPic match {
        case Some(ppic) =>
          val pbounds = ppic.bounds
          val bounds = pic.bounds
          val tx = pbounds.x + pbounds.width - bounds.x
          pic.offset(tx, 0)
        case None =>
      }
      prevPic = Some(pic)
    }
  }

  def copy = new HPics(picsCopy)
}

object HPicsCentered {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new HPicsCentered(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new HPicsCentered(freshPics(pics))
}

class HPicsCentered(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    var prevPic: Option[Picture] = None
    pics.foreach { pic =>
      prevPic match {
        case Some(ppic) =>
          val pbounds = ppic.bounds
          val bounds = pic.bounds
          val tx = pbounds.x + pbounds.width - bounds.x
          val ty = pbounds.y - bounds.y + (pbounds.height - bounds.height) / 2
          pic.offset(tx, ty)
        case None =>
      }
      prevPic = Some(pic)
    }
  }

  def copy = new HPicsCentered(picsCopy)
}

object VPics {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new VPics(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new VPics(freshPics(pics))
}

class VPics(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    var prevPic: Option[Picture] = None
    pics.foreach { pic =>
      prevPic match {
        case Some(ppic) =>
          val pbounds = ppic.bounds
          val bounds = pic.bounds
          val ty = pbounds.y + pbounds.height - bounds.y
          pic.offset(0, ty)
        case None =>
      }
      prevPic = Some(pic)
    }
  }

  def copy = new VPics(picsCopy)
}

object VPicsCentered {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new VPicsCentered(freshPics(pics))
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new VPicsCentered(freshPics(pics))
}

class VPicsCentered(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    var prevPic: Option[Picture] = None
    pics.foreach { pic =>
      prevPic match {
        case Some(ppic) =>
          val pbounds = ppic.bounds
          val bounds = pic.bounds
          val tx = pbounds.x - bounds.x + (pbounds.width - bounds.width) / 2
          val ty = pbounds.y + pbounds.height - bounds.y
          pic.offset(tx, ty)
        case None =>
      }
      prevPic = Some(pic)
    }
  }

  def copy = new VPicsCentered(picsCopy)
}

object BatchPics {
  def apply(pics: collection.Seq[Picture])(implicit kojoWorld: KojoWorld) = new BatchPics(pics.toSeq)
  def apply(pics: Picture*)(implicit kojoWorld: KojoWorld) = new BatchPics(pics)
}

class BatchPics(pics: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(pics) {
  def layoutChildren(): Unit = {
    pics.tail.foreach { p =>
      p.invisible()
    }
  }

  var currPicIndex = 0
  var lastDraw = System.currentTimeMillis

  def currentPicture = pics(currPicIndex)

  override def showNext(gap: Long) = {
    val currTime = System.currentTimeMillis
    if (currTime - lastDraw > gap) {
      pics(currPicIndex).invisible()
      currPicIndex += 1
      if (currPicIndex == pics.size) {
        currPicIndex = 0
      }
      pics(currPicIndex).visible()
      lastDraw = currTime
    }
  }

  override def picGeom: Geometry = {
    if (!made) {
      return null
    }
    pgTransform.transform(pics(currPicIndex).picGeom)
  }

  def copy = new BatchPics(picsCopy)
}

class PicScreen {
  import scala.collection.mutable.ArrayBuffer

  val pics = ArrayBuffer.empty[Picture]
  var drawn = false
  var showCmd: Option[() => Unit] = None
  var hideCmd: Option[() => Unit] = None

  def add(ps: Picture*): Unit = {
    ps.foreach { pics.append(_) }
  }

  def add(ps: Iterable[Picture]): Unit = {
    ps.foreach { pics.append(_) }
  }

  private def draw(): Unit = {
    pics.foreach { _.draw() }
  }

  def hide(): Unit = {
    pics.foreach { _.invisible() }
    hideCmd.foreach { c =>
      c()
    }
  }

  private def unhide(): Unit = {
    pics.foreach { _.visible() }
  }

  def show(): Unit = {
    if (!drawn) {
      draw()
      drawn = true
    }
    else {
      unhide()
    }

    showCmd.foreach { c =>
      c()
    }
  }

  def erase(): Unit = {
    pics.foreach { _.erase() }
  }

  def onShow(cmd: => Unit): Unit = {
    showCmd = Some(() => cmd)
  }

  def onHide(cmd: => Unit): Unit = {
    hideCmd = Some(() => cmd)
  }
}
