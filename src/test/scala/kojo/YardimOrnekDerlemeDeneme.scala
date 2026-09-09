package kojo

/**
 * sozluk/yardim.json'daki yöntem örneklerinin ikojo API'sine karşı
 * DERLENDİĞİNİ sınar. Sözlük her örneğin altına "Örnek sınanıyor" yazıyor;
 * bu dosya o sözü ikojo tarafında da tutuyor.
 *
 * Üretilmiştir; kaynak: araclar/yardim-derleme-uret.py
 * Anahtar sözcükler burada İngilizce (bkz. o betiğin başlığı).
 */
object YardimOrnekDerlemeDeneme
    extends kojo.tr.SayıYöntemleri
    with kojo.tr.MatematikYöntemleri
    with kojo.tr.BelkiYöntemleri
    with kojo.tr.İkisindenBiriYöntemleri
    with kojo.tr.BölümselİşlevYöntemleri
    with kojo.tr.YazıYöntemleri
    with kojo.tr.HarfYöntemleri
    with kojo.tr.AralıkYöntemleri
    with kojo.tr.KümeYöntemleri
    with kojo.tr.DiziYöntemleri
    with kojo.tr.EşlemYöntemleri
    with kojo.tr.DizinYöntemleri
    with kojo.tr.YöneyYöntemleri
    with kojo.tr.KökTürYöntemleri
    with kojo.tr.DizimYöntemleri
    with kojo.tr.MiskinDizinYöntemleri
    with kojo.tr.KuyrukYöntemleri
    with kojo.tr.DizikYöntemleri {

  // adımı
  def y_ad_m_(): Any = {
    (Aralık.kapalı(1, 10) adım 3).adımı
  }

  // al
  def y_al(): Any = {
    Dizin(1, 2, 3, 4).al(2)
  }

  // alDoğruKaldıkça
  def y_alDo_ruKald_k_a(): Any = {
    Dizin(1, 2, 3, 1).alDoğruKaldıkça(_ < 3)
  }

  // alSağdan
  def y_alSa_dan(): Any = {
    Dizin(1, 2, 3, 4).alSağdan(2)
  }

  // alSırayla
  def y_alS_rayla(): Any = {
    Eşlek("a" -> 1).alSırayla(1).sayı
  }

  // alYoksa
  def y_alYoksa(): Any = {
    Eşlek("a" -> 1).alYoksa("b", 0)
  }

  // alYoksaEkle
  def y_alYoksaEkle(): Any = {
    { val e = Eşlem("a" -> 1); e.alYoksaEkle("b", 2); e.sayı }
  }

  // altKümeleri
  def y_altK_meleri(): Any = {
    Küme(1, 2, 3).altKümeleri(2).dizine.boyu
  }

  // altKümesiMi
  def y_altK_mesiMi(): Any = {
    Küme(1, 2).altKümesiMi(Küme(1, 2, 3))
  }

  // anahtarKümesi
  def y_anahtarK_mesi(): Any = {
    Eşlek("a" -> 1, "b" -> 2).anahtarKümesi.boyu
  }

  // anahtarYineleyici
  def y_anahtarYineleyici(): Any = {
    Eşlek("a" -> 1).anahtarYineleyici.dizine
  }

  // anahtarlar
  def y_anahtarlar(): Any = {
    Eşlek("a" -> 1).anahtarlar.dizine
  }

  // anahtarlarıEle
  def y_anahtarlar_Ele(): Any = {
    Eşlek("a" -> 1, "b" -> 2).anahtarlarıEle(_ == "a")
  }

  // baştanAl
  def y_ba_tanAl(): Any = {
    ÖncelikSırası(3, 9, 5).baştanAl()
  }

  // baştanAlHepsini
  def y_ba_tanAlHepsini(): Any = {
    ÖncelikSırası(3, 9, 5).baştanAlHepsini
  }

  // baştanÇıkar
  def y_ba_tan__kar(): Any = {
    { val k = Kuyruk(1, 2, 3); (k.baştanÇıkar(), k.dizine) }
  }

  // baştanÇıkarBelki
  def y_ba_tan__karBelki(): Any = {
    Kuyruk.boş[Sayı].baştanÇıkarBelki
  }

  // baştanÇıkarDoğruKaldıkça
  def y_ba_tan__karDo_ruKald_k_a(): Any = {
    { val k = Kuyruk(1, 2, 5); k.baştanÇıkarDoğruKaldıkça(_ < 3) }
  }

  // baştanÇıkarKoşulla
  def y_ba_tan__karKo_ulla(): Any = {
    { val k = Kuyruk(1, 2, 3); k.baştanÇıkarKoşulla(_ > 1) }
  }

  // başı
  def y_ba__(): Any = {
    Dizin(3, 1, 2).başı
  }

  // başıBelki
  def y_ba__Belki(): Any = {
    Dizin[Sayı]().başıBelki
  }

  // başındaMı
  def y_ba__ndaM_(): Any = {
    Dizin(1, 2, 3).başındaMı(Dizin(1, 2))
  }

  // başındanAt
  def y_ba__ndanAt(): Any = {
    "merhabaDünya".başındanAt("merhaba")
  }

  // belkiye
  def y_belkiye(): Any = {
    Sol("hata").belkiye
  }

  // bellekli
  def y_bellekli(): Any = {
    { val b = Dizin(1, 2).yineleyici.bellekli; (b.başı, b.dizine) }
  }

  // bileşim
  def y_bile_im(): Any = {
    Dizin(1, 2).bileşim(Dizin(3))
  }

  // birleştir
  def y_birle_tir(): Any = {
    Sol("hata").birleştir
  }

  // boyu
  def y_boyu(): Any = {
    Dizin(3, 1, 2).boyu
  }

  // boşMu
  def y_bo_Mu(): Any = {
    Dizin[Sayı]().boşMu
  }

  // boşalt
  def y_bo_alt(): Any = {
    { val e = Eşlem("a" -> 1); e.boşalt(); e.sayı }
  }

  // boşsaÖbürü
  def y_bo_sa_b_r_(): Any = {
    Hiçbiri.boşsaÖbürü(Biri(5))
  }

  // bul
  def y_bul(): Any = {
    Dizin(1, 2, 3).bul(_ > 1)
  }

  // bulSondan
  def y_bulSondan(): Any = {
    Dizin(1, 2, 3).bulSondan(_ < 3)
  }

  // böl
  def y_b_l(): Any = {
    Dizin(1, 2, 3, 4).böl(_ % 2 == 0)
  }

  // bölDoğruKaldıkça
  def y_b_lDo_ruKald_k_a(): Any = {
    Dizin(1, 2, 3, 1).bölDoğruKaldıkça(_ < 3)
  }

  // bölYerinden
  def y_b_lYerinden(): Any = {
    Dizin(1, 2, 3, 4).bölYerinden(2)
  }

  // bölİşle
  def y_b_l__le(): Any = {
    Dizin(1, 2, 3, 4).bölİşle(x => if (x % 2 == 0) Sağ(x * 10) else Sol(x))
  }

  // büyükHarfe
  def y_b_y_kHarfe(): Any = {
    "kojo".büyükHarfe
  }

  // dahaVarMı
  def y_dahaVarM_(): Any = {
    Dizin(1, 2).yineleyici.dahaVarMı
  }

  // devrik
  def y_devrik(): Any = {
    Dizin(Dizin(1, 2), Dizin(3, 4)).devrik
  }

  // değerYineleyici
  def y_de_erYineleyici(): Any = {
    Eşlek("a" -> 1).değerYineleyici.dizine
  }

  // değerler
  def y_de_erler(): Any = {
    Eşlek("a" -> 1).değerler.dizine
  }

  // değerleriİşle
  def y_de_erleri__le(): Any = {
    Eşlek("a" -> 1).değerleriİşle(_ * 10)
  }

  // değiştir
  def y_de_i_tir(): Any = {
    "ali ali".değiştir("ali", "veli")
  }

  // değiştirilmiş
  def y_de_i_tirilmi_(): Any = {
    Eşlek("a" -> 1).değiştirilmiş("a", 9)
  }

  // değiştirİlkini
  def y_de_i_tir_lkini(): Any = {
    "ali ali".değiştirİlkini("ali", "veli")
  }

  // dilim
  def y_dilim(): Any = {
    Dizin(1, 2, 3, 4).dilim(1, 3)
  }

  // dilimSırası
  def y_dilimS_ras_(): Any = {
    Dizin(1, 2, 3).dilimSırası(Dizin(2, 3))
  }

  // dizi
  def y_dizi(): Any = {
    Yığın(1, 2, 3).dizi
  }

  // dizime
  def y_dizime(): Any = {
    Dizin(7, 8, 9).dizime.diziye.başı
  }

  // dizine
  def y_dizine(): Any = {
    Dizin(1, 2, 3).dizine
  }

  // diziye
  def y_diziye(): Any = {
    Küme(1).diziye
  }

  // doluMu
  def y_doluMu(): Any = {
    Dizin(1).doluMu
  }

  // dönüştür
  def y_d_n__t_r(): Any = {
    Eşlek("a" -> 1).dönüştür((a, d) => a + d)
  }

  // düzleştir
  def y_d_zle_tir(): Any = {
    Dizin(Dizin(1, 2), Dizin(3)).düzleştir
  }

  // düzİşle
  def y_d_z__le(): Any = {
    Dizin(1, 2).düzİşle(x => Dizin(x, x))
  }

  // düşür
  def y_d___r(): Any = {
    Dizin(1, 2, 3, 4).düşür(2)
  }

  // düşürDoğruKaldıkça
  def y_d___rDo_ruKald_k_a(): Any = {
    Dizin(1, 2, 3, 1).düşürDoğruKaldıkça(_ < 3)
  }

  // düşürSağdan
  def y_d___rSa_dan(): Any = {
    Dizin(1, 2, 3, 4).düşürSağdan(2)
  }

  // ekle
  def y_ekle(): Any = {
    Kuyruk(1).ekle(2).dizine
  }

  // ekli
  def y_ekli(): Any = {
    Küme(1, 2).ekli(3).boyu
  }

  // ele
  def y_ele(): Any = {
    Dizin(1, 2, 3, 4).ele(_ % 2 == 0)
  }

  // eleDeğilse
  def y_eleDe_ilse(): Any = {
    Dizin(1, 2, 3, 4).eleDeğilse(_ % 2 == 0)
  }

  // elekle
  def y_elekle(): Any = {
    for (x <- Biri(5) if x > 1) yield x * 10
  }

  // enUfağı
  def y_enUfa__(): Any = {
    Dizin(3, 1, 2).enUfağı
  }

  // enUfağıBelki
  def y_enUfa__Belki(): Any = {
    Dizin[Sayı]().enUfağıBelki
  }

  // enİrisi
  def y_en_risi(): Any = {
    Dizin(3, 1, 2).enİrisi
  }

  // enİrisiBelki
  def y_en_risiBelki(): Any = {
    Dizin(3, 1, 2).enİrisiBelki
  }

  // eşEkle
  def y_e_Ekle(): Any = {
    { val e = Eşlem("a" -> 1); e.eşEkle("b" -> 2); e.sayı }
  }

  // eşitMiKüçükHarfBüyükHarfAyrımıYapmadan
  def y_e_itMiK___kHarfB_y_kHarfAyr_m_Yapmadan(): Any = {
    "Koco".eşitMiKüçükHarfBüyükHarfAyrımıYapmadan("kOCO")
  }

  // eşleme
  def y_e_leme(): Any = {
    { val e = Dizin((1, "bir")).eşleme; e(1) }
  }

  // eşlenirMi
  def y_e_lenirMi(): Any = {
    "abc123".eşlenirMi("[a-z]+[0-9]+")
  }

  // eşleğe
  def y_e_le_e(): Any = {
    { val e = Dizin((1, "bir")).eşleğe; e(1) }
  }

  // eşli
  def y_e_li(): Any = {
    Eşlek("a" -> 1).eşli("a")
  }

  // fark
  def y_fark(): Any = {
    Dizin(1, 2, 3).fark(Dizin(2))
  }

  // gösterdikleriAynıMı
  def y_g_sterdikleriAyn_M_(): Any = {
    Dizin(1, 2).yineleyici.gösterdikleriAynıMı(Dizin(1, 2))
  }

  // güncelle
  def y_g_ncelle(): Any = {
    { val e = Eşlem("a" -> 1); e.güncelle("a", 9); e("a") }
  }

  // harf
  def y_harf(): Any = {
    "merhaba".harf(0)
  }

  // hepsiDoğruMu
  def y_hepsiDo_ruMu(): Any = {
    Dizin(1, 2, 3).hepsiDoğruMu(_ > 0)
  }

  // hepsiniHesapla
  def y_hepsiniHesapla(): Any = {
    MiskinDizin.sayalım(1).al(3).hepsiniHesapla
  }

  // hepsiÇıkarılmış
  def y_hepsi__kar_lm__(): Any = {
    Eşlek("a" -> 1, "b" -> 2).hepsiÇıkarılmış(Dizin("a", "b")).sayı
  }

  // hepsiİçinDoğruMu
  def y_hepsi__inDo_ruMu(): Any = {
    Dizin(2, 4).hepsiİçinDoğruMu(_ % 2 == 0)
  }

  // herbiriİçin
  def y_herbiri__in(): Any = {
    { var t = 0; Dizin(1, 2, 3).herbiriİçin(x => t += x); t }
  }

  // herÖgeİçin
  def y_her_ge__in(): Any = {
    { var t = 0; Aralık.kapalı(1, 3).herÖgeİçin(x => t += x); t }
  }

  // herİkiliİçin
  def y_her_kili__in(): Any = {
    { var t = 0; Eşlek("a" -> 1, "b" -> 2).herİkiliİçin((a, d) => t += d); t }
  }

  // ikile
  def y_ikile(): Any = {
    Dizin(1, 2).ikile(Dizin("a", "b"))
  }

  // ikileHepsini
  def y_ikileHepsini(): Any = {
    Dizin(1, 2, 3).ikileHepsini(Dizin("a"), 0, "-")
  }

  // ikileKonumla
  def y_ikileKonumla(): Any = {
    Dizin("a", "b").ikileKonumla
  }

  // ikileSırayla
  def y_ikileS_rayla(): Any = {
    Dizin("a", "b").ikileSırayla
  }

  // ikiliyiAç
  def y_ikiliyiA_(): Any = {
    Dizin((1, "a"), (2, "b")).ikiliyiAç
  }

  // ikiyeAyır
  def y_ikiyeAy_r(): Any = {
    "merhaba".ikiyeAyır(_ == 'a')
  }

  // ikizYap
  def y_ikizYap(): Any = {
    { val (a, b) = Dizin(1, 2).yineleyici.ikizYap; (a.dizine, b.dizine) }
  }

  // ikizle
  def y_ikizle(): Any = {
    { val ö = ÖncelikSırası(1, 5); val k = ö.ikizle(); k.baştanAl(); ö.boyu }
  }

  // ilkHarfiBüyült
  def y_ilkHarfiB_y_lt(): Any = {
    "merhaba".ilkHarfiBüyült
  }

  // ilki
  def y_ilki(): Any = {
    Aralık(1, 10).ilki
  }

  // indirge
  def y_indirge(): Any = {
    Dizin(1, 2, 3).indirge(_ + _)
  }

  // indirgeBelki
  def y_indirgeBelki(): Any = {
    Dizin[Sayı]().indirgeBelki(_ + _)
  }

  // indirgeSağdan
  def y_indirgeSa_dan(): Any = {
    Dizin(1, 2, 3).indirgeSağdan(_ - _)
  }

  // indirgeSağdanBelki
  def y_indirgeSa_danBelki(): Any = {
    Dizin[Sayı]().indirgeSağdanBelki(_ + _)
  }

  // indirgeSoldan
  def y_indirgeSoldan(): Any = {
    Dizin(1, 2, 3).indirgeSoldan(_ - _)
  }

  // indirgeSoldanBelki
  def y_indirgeSoldanBelki(): Any = {
    Dizin(1, 2, 3).indirgeSoldanBelki(_ + _)
  }

  // it
  def y_it(): Any = {
    Yığın(1, 2).it(3).tepe
  }

  // itHepsini
  def y_itHepsini(): Any = {
    Yığın.boş[Sayı].itHepsini(Dizin(1, 2, 3)).tepe
  }

  // içeriyorMu
  def y_i_eriyorMu(): Any = {
    Dizin(1, 2, 3).içeriyorMu(2)
  }

  // içeriyorMuDilim
  def y_i_eriyorMuDilim(): Any = {
    Dizin(1, 2, 3).içeriyorMuDilim(Dizin(2, 3))
  }

  // içindeMi
  def y_i_indeMi(): Any = {
    Aralık(1, 10).içindeMi(5)
  }

  // işle
  def y_i_le(): Any = {
    Dizin(1, 2, 3).işle(_ * 10)
  }

  // kaldır
  def y_kald_r(): Any = {
    Eşlek("a" -> 1).kaldır("yok")
  }

  // karşılıklıMı
  def y_kar__l_kl_M_(): Any = {
    Dizin(1, 2).karşılıklıMı(Dizin(2, 4))((a, b) => b == a * 2)
  }

  // katla
  def y_katla(): Any = {
    Dizin(1, 2, 3).katla(10)(_ + _)
  }

  // kayarÖbekli
  def y_kayar_bekli(): Any = {
    Dizin(1, 2, 3).kayarÖbekli(2).dizine
  }

  // kesire
  def y_kesire(): Any = {
    "3.5".kesire
  }

  // kesişim
  def y_kesi_im(): Any = {
    Dizin(1, 2, 3).kesişim(Dizin(2, 3, 4))
  }

  // kombinasyonlar
  def y_kombinasyonlar(): Any = {
    Dizin(1, 2, 3).kombinasyonlar(2).dizine
  }

  // koy
  def y_koy(): Any = {
    Eşlem("a" -> 1).koy("a", 9)
  }

  // koşulla
  def y_ko_ulla(): Any = {
    İkisindenBiri.koşulla(3 > 2, "oldu", "olmadı")
  }

  // kuyruklar
  def y_kuyruklar(): Any = {
    Dizin(1, 2, 3).kuyruklar.dizine
  }

  // kuyruğa
  def y_kuyru_a(): Any = {
    ÖncelikSırası(3, 9).kuyruğa.boyu
  }

  // kuyruğaEkle
  def y_kuyru_aEkle(): Any = {
    Kuyruk(1, 2).kuyruğaEkle(3).dizine
  }

  // kuyruğaEkleHepsini
  def y_kuyru_aEkleHepsini(): Any = {
    Kuyruk(1).kuyruğaEkleHepsini(Dizin(2, 3)).dizine
  }

  // kuyruğu
  def y_kuyru_u(): Any = {
    Dizin(3, 1, 2).kuyruğu
  }

  // kümeye
  def y_k_meye(): Any = {
    Dizin(1, 2, 1).kümeye
  }

  // küçükHarfe
  def y_k___kHarfe(): Any = {
    "KOCO".küçükHarfe
  }

  // kısalt
  def y_k_salt(): Any = {
    "  merhaba  ".kısalt
  }

  // kıyasla
  def y_k_yasla(): Any = {
    "a".kıyasla("b") < 0
  }

  // nerede
  def y_nerede(): Any = {
    Dizin(3, 1, 2).nerede(_ < 2)
  }

  // neredeSondan
  def y_neredeSondan(): Any = {
    Dizin(1, 2, 3, 2).neredeSondan(_ == 2)
  }

  // parçası
  def y_par_as_(): Any = {
    "merhaba".parçası(0, 3)
  }

  // permütasyonlar
  def y_perm_tasyonlar(): Any = {
    Dizin(1, 2).permütasyonlar.dizine
  }

  // satırlar
  def y_sat_rlar(): Any = {
    "bir\niki".satırlar.dizine
  }

  // say
  def y_say(): Any = {
    Dizin(1, 2, 3, 4).say(_ % 2 == 0)
  }

  // sayı
  def y_say_(): Any = {
    Eşlek("a" -> 1, "b" -> 2).sayı
  }

  // sayıya
  def y_say_ya(): Any = {
    "42".sayıya
  }

  // sayıyaBelki
  def y_say_yaBelki(): Any = {
    "kırk".sayıyaBelki
  }

  // sağMı
  def y_sa_M_(): Any = {
    Sağ(5).sağMı
  }

  // sağa
  def y_sa_a(): Any = {
    Biri(1).sağa("yok")
  }

  // sağdanKatla
  def y_sa_danKatla(): Any = {
    Dizin(1, 2, 3).sağdanKatla("")((s, y) => y + s)
  }

  // seçİşle
  def y_se___le(): Any = {
    Dizin(1, 2, 3, 4).seçİşle { case x if x % 2 == 0 => x * 10 }
  }

  // seçİşleİlk
  def y_se___le_lk(): Any = {
    Dizin(1, 2, 3).seçİşleİlk { case x if x > 1 => x * 10 }
  }

  // sil
  def y_sil(): Any = {
    { val y = Yığın(1, 2); y.sil(); y.tane }
  }

  // solMu
  def y_solMu(): Any = {
    Sol("hata").solMu
  }

  // sola
  def y_sola(): Any = {
    Biri(1).sola("yok")
  }

  // soldanKatla
  def y_soldanKatla(): Any = {
    Dizin(1, 2, 3).soldanKatla("")((y, s) => y + s)
  }

  // sonu
  def y_sonu(): Any = {
    Dizin(3, 1, 2).sonu
  }

  // sonuBelki
  def y_sonuBelki(): Any = {
    Dizin(3, 1, 2).sonuBelki
  }

  // sonunaEkle
  def y_sonunaEkle(): Any = {
    Dizin(1, 2).sonunaEkle(3)
  }

  // sonunaEkleHepsini
  def y_sonunaEkleHepsini(): Any = {
    Dizin(1, 2).sonunaEkleHepsini(Dizin(3, 4))
  }

  // sonuncu
  def y_sonuncu(): Any = {
    Aralık(1, 10).sonuncu
  }

  // sonundaMı
  def y_sonundaM_(): Any = {
    Dizin(1, 2, 3).sonundaMı(Dizin(2, 3))
  }

  // sonundanAt
  def y_sonundanAt(): Any = {
    "deneme.txt".sonundanAt(".txt")
  }

  // sıradaki
  def y_s_radaki(): Any = {
    { val y = Dizin(7, 8).yineleyici; (y.sıradaki, y.sıradaki) }
  }

  // sıradakiBelki
  def y_s_radakiBelki(): Any = {
    Dizin[Sayı]().yineleyici.sıradakiBelki
  }

  // sırala
  def y_s_rala(): Any = {
    Dizin("aaa", "a", "aa").sırala(_.boyu)
  }

  // sıralar
  def y_s_ralar(): Any = {
    Dizin(3, 1, 2).sıralar.dizine
  }

  // sıralı
  def y_s_ral_(): Any = {
    Dizin(3, 1, 2).sıralı
  }

  // sırası
  def y_s_ras_(): Any = {
    Dizin(3, 1, 2).sırası(1)
  }

  // sırasıSondan
  def y_s_ras_Sondan(): Any = {
    Dizin(1, 2, 3, 2).sırasıSondan(2)
  }

  // sırayaSok
  def y_s_rayaSok(): Any = {
    Dizin(1, 3, 2).sırayaSok(_ > _)
  }

  // takasla
  def y_takasla(): Any = {
    Sağ(5).takasla
  }

  // tane
  def y_tane(): Any = {
    Yığın(1, 2, 3).tane
  }

  // tara
  def y_tara(): Any = {
    Dizin(1, 2, 3).tara(0)(_ + _)
  }

  // taraSağdan
  def y_taraSa_dan(): Any = {
    Dizin(1, 2, 3).taraSağdan(0)(_ + _)
  }

  // taraSoldan
  def y_taraSoldan(): Any = {
    Dizin(1, 2, 3).taraSoldan("")((y, s) => y + s)
  }

  // tepe
  def y_tepe(): Any = {
    Yığın(1, 2, 3).tepe
  }

  // tersi
  def y_tersi(): Any = {
    Dizin(1, 2, 3).tersi
  }

  // tersİşle
  def y_ters__le(): Any = {
    Dizin(1, 2, 3).tersİşle(_ * 10)
  }

  // topla
  def y_topla(): Any = {
    Dizin(1, 2, 3).topla
  }

  // uzat
  def y_uzat(): Any = {
    Dizin(1, 2).uzat(4, 0)
  }

  // uzunluğu
  def y_uzunlu_u(): Any = {
    Aralık(1, 10).uzunluğu
  }

  // varMı
  def y_varM_(): Any = {
    Dizin(1, 2, 3).varMı(_ > 2)
  }

  // varsayılanDeğerle
  def y_varsay_lanDe_erle(): Any = {
    Eşlek("a" -> 1).varsayılanDeğerle(0)("yok")
  }

  // yama
  def y_yama(): Any = {
    Dizin(1, 2, 3).yama(1, Dizin(8, 9), 1)
  }

  // yazı
  def y_yaz_(): Any = {
    Aralık.kapalı(1, 3).yazı()
  }

  // yazıYap
  def y_yaz_Yap(): Any = {
    Dizin(1, 2, 3).yazıYap("-")
  }

  // yinelemesiz
  def y_yinelemesiz(): Any = {
    Dizin(1, 2, 1, 3).yinelemesiz
  }

  // yinelemesizİşlevle
  def y_yinelemesiz__levle(): Any = {
    Dizin("al", "at", "be").yinelemesizİşlevle(_.harf(0))
  }

  // yineleyici
  def y_yineleyici(): Any = {
    Küme(1).yineleyici.dizine
  }

  // yokMu
  def y_yokMu(): Any = {
    Hiçbiri.yokMu
  }

  // yöneye
  def y_y_neye(): Any = {
    Dizin(1, 2).yöneye
  }

  // çarp
  def y__arp(): Any = {
    Dizin(2, 3, 4).çarp
  }

  // çek
  def y__ek(): Any = {
    { val y = Yığın(1, 2, 3); (y.çek(), y.tane) }
  }

  // çıkarılmış
  def y___kar_lm__(): Any = {
    Eşlek("a" -> 1, "b" -> 2).çıkarılmış("a")
  }

  // öbekle
  def y__bekle(): Any = {
    Dizin(1, 2, 3, 4).öbekle(_ % 2)(0)
  }

  // öbekleİşle
  def y__bekle__le(): Any = {
    Dizin(1, 2, 3, 4).öbekleİşle(_ % 2)(_ * 10)(0)
  }

  // öbekleİşleİndirge
  def y__bekle__le_ndirge(): Any = {
    Dizin(1, 2, 3, 4).öbekleİşleİndirge(_ % 2)(x => x)(_ + _)(0)
  }

  // öbekli
  def y__bekli(): Any = {
    Dizin(1, 2, 3, 4).öbekli(2).dizine
  }

  // önler
  def y__nler(): Any = {
    Dizin(1, 2, 3).önler.dizine
  }

  // önü
  def y__n_(): Any = {
    Dizin(3, 1, 2).önü
  }

  // önüneEkle
  def y__n_neEkle(): Any = {
    Dizin(2, 3).önüneEkle(1)
  }

  // önüneEkleHepsini
  def y__n_neEkleHepsini(): Any = {
    Dizin(3, 4).önüneEkleHepsini(Dizin(1, 2))
  }
}
