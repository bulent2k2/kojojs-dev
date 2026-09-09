#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
yardim-derleme-uret.py -- sözlük panellerindeki örneklerin ikojo'da da
DERLENDİĞİNİ sınayan dosyayı üretir.

Neden gerekli: sozluk/yardim.json masaüstü Koco'dan (help.scala) geliyor ve
oradaki testlerle doğrulanıyor -- ama masaüstü API'sine karşı. Sözlük ise
ikojo sitesinde sunuluyor ve her örneğin altında "Örnek sınanıyor" yazıyor.
İki API %100 örtüşmediği sürece bu söz burada da sınanmalı; yoksa sözlük
sessizce yalan söyler. (Ölçüldü: bu test ilk koşuşunda 3 örneği yakaladı --
Eşlek.anahtarlar/değerler'in Yinelenebilir sarmalayıcısı ve Yineleyici
turunun eksik kalan bellekli.başı yöntemi.)

gosteri-uret.py ile aynı kalıp; oradaki gibi anahtar sözcükler İngilizceye
çevriliyor, çünkü kojojs-dev STOK Scala.js ile derleniyor.

Kullanım:
  araclar/yardim-derleme-uret.py            # varsayılan yollar
  araclar/yardim-derleme-uret.py <yardim.json> <hedef.scala>
"""
import io
import json
import os
import re
import sys

ANAHTAR = [('dez ', 'val '), ('den ', 'var '), ('tanım ', 'def '), ('eğer ', 'if '),
           ('yoksa ', 'else '), ('durum ', 'case '), ('için ', 'for '), ('ver ', 'yield ')]

BASLIK = '''package kojo

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

'''


def ingilizce(kod):
    for tr, en in ANAHTAR:
        kod = re.sub(r'(?<![A-Za-zÇĞİIÖŞÜçğıöşü])' + tr, en, kod)
    return kod


# Beklenen alanlar. `türler` en yenisi: sözlük paneli onu basıyor ve alan
# YOKSA sessizce susuyor -- yani eksilmesi ne testte ne ekranda görünüyor.
# Eylül 2026'da tam bu oldu: alanı ekleyen kojo commit'i ile onu kullanan
# sözlük ayrı sırayla birleşti, arada kalan pencerede eski üreticiden
# yenileme yapan biri 190 alanın hepsini izsiz silerdi.
#
# Asıl sav kaynakta (kojo: YardımDışaAktarTest); burası ikinci ağ -- dosya
# bu depoya girdikten SONRA da bir kez daha bakılıyor.
ZORUNLU_ALANLAR = ('imza', 'açıklama', 'örnek', 'sonuç', 'türler')


def bicimDenetle(kaynak, yöntemler):
    """yardim.json'daki her yöntem girdisi beklenen alanları taşıyor mu."""
    eksikler = []
    for ad, g in yöntemler:
        eksik = [a for a in ZORUNLU_ALANLAR if a not in g]
        if eksik:
            eksikler.append('  %s: %s' % (ad, ', '.join(eksik)))
    if eksikler:
        sys.exit(
            '%s: %d yöntem girdisinde alan eksik.\n%s\n'
            'Dosya eski bir kojo sürümünden üretilmiş olabilir; kojo master\'dan\n'
            'yeniden üretin:\n'
            "  ./sbt.sh 'Test/runMain net.kogics.kojo.araclar.YardımDışaAktar <buradaki>/sozluk/yardim.json'"
            % (os.path.normpath(kaynak), len(eksikler), '\n'.join(eksikler[:5]))
        )


def main():
    kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    kaynak = sys.argv[1] if len(sys.argv) > 1 else os.path.join(kok, 'sozluk', 'yardim.json')
    hedef = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        kok, 'src', 'test', 'scala', 'kojo', 'YardimOrnekDerlemeDeneme.scala')

    veri = json.load(io.open(kaynak, encoding='utf-8'))
    # yalnız "yöntem" girdilerinin yapılandırılmış örneği var; "metin"
    # girdileri elle yazılmış HTML, onları masaüstü YardımÖrnekleriTest sınıyor
    yöntemler = [(ad, g) for ad, g in sorted(veri.items()) if g.get('tür') == 'yöntem']
    bicimDenetle(kaynak, yöntemler)
    örnekler = [(ad, g['örnek']) for ad, g in yöntemler]

    def adı(ad):
        return 'y_' + re.sub(r'[^A-Za-z0-9]', '_', ad)

    gövde = []
    for ad, kod in örnekler:
        satırlar = '\n'.join('    ' + s for s in ingilizce(kod).split('\n'))
        gövde.append('  // %s\n  def %s(): Any = {\n%s\n  }' % (ad, adı(ad), satırlar))

    io.open(hedef, 'w', encoding='utf-8').write(BASLIK + '\n\n'.join(gövde) + '\n}\n')
    print('%s yazıldı (%d örnek)' % (os.path.normpath(hedef), len(örnekler)))


if __name__ == '__main__':
    main()
