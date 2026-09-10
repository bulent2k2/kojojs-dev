#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-renk-denetle.py -- sözlükteki renk satırları koddaki Renkler ile aynı mı.

Neden: koco-sozlugu.html'deki `key:"val"` tablosu ELLE tutulan HTML. Kodda bir
renk adı eklenir/değişir ve tabloya yansıtılmazsa hiçbir şey hata vermez;
sayfa sessizce eksik ya da yanlış veri gösterir.

Eylül 2026'da tam bu oldu: koddaki 39 renk adına karşılık sözlükte 6 satır
vardı, aradaki 33 ad (bütün açık aile, zengin adların hepsi) hiç listelenmiyordu
-- ve bunu ancak elle sayınca gördük. Bu betik o sınıfı kapatıyor.

Karşılaştırma İNGİLİZCE ad üzerinden yapılıyor, "Türkçe adı renge benziyor mu"
diye tahmin ederek değil: bir sözlük satırı, İngilizce adı doodle paletinde
(CommonColors.scala) geçiyorsa renk satırı sayılıyor.

Kullanım:
  araclar/sozluk-renk-denetle.py          # fark varsa 1 döner
"""
import io
import os
import re
import sys

# Renkler'de DRenk.<ad> ile TANIMLANMAYANLAR. Üçü de bilinçli:
#   renksiz -- palette karşılığı yok, doğrudan DRenk(0,0,0,0)
#   saydam  -- renksiz'in takma adı
#   koyuMor -- koyuMorumsu'nun eskitilmiş takma adı; sözlükte kendi satırı
#              yok, darkMagenta satırının notunda geçiyor
PALETSİZ = {'renksiz', 'saydam'}


def paletAdları(yol):
    """doodle/CommonColors.scala'daki renk adları."""
    with io.open(yol, encoding='utf-8') as d:
        return set(re.findall(r'val\s+([A-Za-z][A-Za-z0-9]*)\s*=', d.read()))


def koddakiRenkler(yol):
    """Renkler nesnesindeki {türkçe: ingilizce}.

    Yalnız `DRenk.<ad>` ile tanımlananlar; takma adlar (koyuMor -> koyuMorumsu)
    atlanıyor, çünkü sözlükte kendi satırlarını almıyorlar -- aynı renk için
    iki satır olurdu.
    """
    with io.open(yol, encoding='utf-8') as d:
        s = d.read()
    try:
        b = s[s.index('object Renkler'):]
        b = b[:b.index('// sık kullanılanlar')]
    except ValueError:
        sys.exit('%s içinde Renkler nesnesi bulunamadı' % yol)
    doğrudan = {}
    for m in re.finditer(r'val\s+([^\s:]+)\s*:\s*Renk\s*=\s*(.+)', b):
        tr, sağ = m.group(1), m.group(2).strip()
        if sağ.startswith('DRenk.'):
            doğrudan[tr] = sağ[len('DRenk.'):].strip()
    return doğrudan


def sözlükRenkleri(yol, palet):
    """koco-sozlugu.html'deki val tablosundan {ingilizce: türkçe}."""
    with io.open(yol, encoding='utf-8') as d:
        s = d.read()
    try:
        b = s[s.index('key:"val"'):]
        b = b[:b.index(']}')]
    except ValueError:
        sys.exit('%s içinde key:"val" bölümü bulunamadı' % yol)
    satırlar = re.findall(r'\["([^"]*)","([^"]*)","([^"]*)"\]', b)
    return {en: tr for en, tr, _ in satırlar if en in palet}


def main():
    kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    palet = paletAdları(os.path.join(kok, 'src/main/scala/kojo/doodle/CommonColors.scala'))
    doğrudan = koddakiRenkler(os.path.join(kok, 'src/main/scala/kojo/tr/renk.scala'))
    sözlük = sözlükRenkleri(os.path.join(kok, 'sozluk/koco-sozlugu.html'), palet)

    kod = {en: tr for tr, en in doğrudan.items() if tr not in PALETSİZ}

    eksik = sorted(set(kod) - set(sözlük))          # kodda var, sözlükte yok
    fazla = sorted(set(sözlük) - set(kod))          # sözlükte var, kodda yok
    başka = sorted(en for en in set(kod) & set(sözlük) if kod[en] != sözlük[en])

    if not (eksik or fazla or başka):
        print('aynı: sözlükteki %d renk satırı Renkler ile eşleşiyor' % len(kod))
        return

    print('FARKLI: sözlükteki renk satırları Renkler ile aynı değil', file=sys.stderr)
    for etiket, liste, biçim in (
            ('sözlükte eksik', eksik, lambda en: '%s -> %s' % (en, kod[en])),
            ('sözlükte fazla', fazla, lambda en: '%s -> %s' % (en, sözlük[en])),
            ('türkçesi farklı', başka, lambda en: '%s: kod %s / sözlük %s' % (en, kod[en], sözlük[en]))):
        if liste:
            print('  %s (%d):' % (etiket, len(liste)), file=sys.stderr)
            for en in liste:
                print('    %s' % biçim(en), file=sys.stderr)
    sys.exit('Sözlükteki key:"val" tablosunu tazeleyin: sozluk/koco-sozlugu.html\n'
             '(ve aynı dosyanın kojojs-editor\'daki kopyasını da -- ikisi birebir aynı olmalı)')


if __name__ == '__main__':
    main()
