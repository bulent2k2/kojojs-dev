  // 50 satırda benzetim/simülasyon: üçlü yıldız sistemleri varmış. 
  // Ama genelde ikisi yakın, üçüncüsü büyük bir gezegen gibi uzak bir yörüngede oluyor.
  // Yoksa burada gördüğümüz gibi kaotik durumlar var
  dez (knm, hız, boy, ölçek, örnekle) = (50, 1.0, 5, 100, 4)
  dez fırça = 0.2 // yörünge kalınlığı. 0 çizmez
  tanım boya(renk: Renk, r: Resim): Resim = r.kalemRenkli(renk).boyalı(renk)
  dez (c1, c2, c3) = (kırmızı, mavi, yeşil)
  dez b1 = boya(c1, Resim.daire(boy))
  dez b2 = boya(c2, Resim.daire(boy))
  dez b3 = boya(c3, Resim.daire(boy))
  tanım dürt = rastgele(10)
  çiz(b1.taşınmış( knm+dürt, knm+dürt),
      b2.taşınmış(-knm+dürt,-knm+dürt),
      b3.taşınmış(-knm+dürt, knm+dürt))
  den (dx1, dy1) = (hız, 0.0) // kırmızı sağa doğru fırlıyor
  den (dx2, dy2) = (-hız, 0.0)// mavi sola
  den (dx3, dy3) = (0.0, 0.0) // yeşil duruyor başta
  tanım açısı(p1: Nokta, p2: Nokta): Kesir = tanjantınAçısı(mutlakDeğer((p1.y - p2.y) / (p1.x - p2.x)))
  tanım uzaklık(p1: Nokta, p2: Nokta): Kesir = karekökü(kuvveti(p1.x - p2.x, 2) + kuvveti(p1.y - p2.y, 2))
  tanım dikey(çapraz: Kesir, açı: Kesir) = çapraz*sinüs(açı)
  tanım yatay(çapraz: Kesir, açı: Kesir) = çapraz*kosinüs(açı)
  tanım çekim(uzaklık: Kesir) = ölçek / kuvveti(enİrisi(boy*3, uzaklık), 2)
  tanım dv(p1: Nokta, p2: Nokta) = { // hızdaki değişim, ivme
      dez a = açısı(p1, p2)
      dez f = çekim(uzaklık(p1, p2))
      dez (fx, fy) = (yatay(f,a), dikey(f,a))
      (eğer (p1.x > p2.x) -fx yoksa fx, 
       eğer (p1.y > p2.y) -fy yoksa fy)
  }
  gizle; den adım = 1 // yörüngeyi bir kaç adımda bir çizelim
  canlandır {
      dez (p1, p2, p3) = (b1.konum, b2.konum, b3.konum)
      eğer (fırça > 0 && adım % örnekle == 1) {  
        için ( (p, c) <- Diz((p1, c1), (p2, c2), (p3, c3)) ) çiz(Resim.daire(fırça).taşınmış(p.x, p.y).boyalı(c).kalemRenkli(c)) }; adım += 1
      için (p <- Diz(p2, p3)) {
          dez (fx, fy) = dv(p1, p)
          dx1 += fx; dy1 += fy
      }
      için (p <- Diz(p3, p1)) {
          dez (fx, fy) = dv(p2, p)
          dx2 += fx; dy2 += fy
      }
      için (p <- Diz(p1, p2)) {
          dez (fx, fy) = dv(p3, p)
          dx3 += fx; dy3 += fy
      }
      b1.konumuKur(p1.x + dx1, p1.y + dy1)
      b2.konumuKur(p2.x + dx2, p2.y + dy2)
      b3.konumuKur(p3.x + dx3, p3.y + dy3)
  }
