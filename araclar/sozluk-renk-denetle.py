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

# Bazı adlar karşılaştırmaya HİÇ girmiyor, çünkü `DRenk.<ad>` biçiminde
# tanımlanmıyorlar -- ayrı bir eleme listesine gerek yok, ayrıştırıcı onları
# zaten toplamıyor (ölçüldü):
#   renksiz -- DRenk(0, 0, 0, 0), paletten gelmiyor
#   saydam  -- renksiz'in takma adı
#   koyuMor -- koyuMorumsu'nun eskitilmiş takma adı; sözlükte kendi satırı yok,
#              darkMagenta satırının notunda geçiyor
# (Önceki sürümde bunun için bir PALETSİZ kümesi vardı; hiçbir şey elemiyordu
#  -- inceleme ölçtü, kaldırıldı.)


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
    """koco-sozlugu.html'deki val tablosundan {ingilizce: [türkçe, ...]}.

    Liste, çünkü aynı renge iki satır düşmesi de bir kusur: sözlükte tek bir
    dict'e sıkıştırılsa biri sessizce kaybolurdu.
    """
    with io.open(yol, encoding='utf-8') as d:
        s = d.read()
    try:
        b = s[s.index('key:"val"'):]
        b = b[:b.index(']}')]
    except ValueError:
        sys.exit('%s içinde key:"val" bölümü bulunamadı' % yol)
    satırlar = re.findall(r'\["([^"]*)","([^"]*)","([^"]*)"\]', b)
    out = {}
    for en, tr, _ in satırlar:
        if en in palet:
            out.setdefault(en, []).append(tr)
    return out


def main():
    kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    palet = paletAdları(os.path.join(kok, 'src/main/scala/kojo/doodle/CommonColors.scala'))
    doğrudan = koddakiRenkler(os.path.join(kok, 'src/main/scala/kojo/tr/renk.scala'))
    sözlük = sözlükRenkleri(os.path.join(kok, 'sozluk/koco-sozlugu.html'), palet)

    # Kod tarafı KÜME: bir renge birden çok Türkçe ad verilebilir. Düz bir
    # {ingilizce: türkçe} sözlüğüne çevirmek ikinci adı SESSİZCE düşürüyordu --
    # üstelik hangisinin düştüğü dosyadaki sıraya bağlıydı, yani aynı değişiklik
    # bazen kırmızı bazen yeşil yanıyordu (inceleme ölçtü, #59).
    kod = {}
    for tr, en in doğrudan.items():
        kod.setdefault(en, set()).add(tr)

    eksik = sorted(set(kod) - set(sözlük))          # kodda var, sözlükte yok
    fazla = sorted(set(sözlük) - set(kod))          # sözlükte var, kodda yok
    ortak = set(kod) & set(sözlük)
    çift = sorted(en for en in ortak if len(sözlük[en]) > 1)
    başka = sorted(en for en in ortak if not set(sözlük[en]) & kod[en])
    # Bir renge iki ad verilip yalnız birinin satırı varsa: öteki ad sözlükte yok.
    satırsız = sorted(en for en in ortak
                      if set(sözlük[en]) & kod[en] and kod[en] - set(sözlük[en]))

    if not (eksik or fazla or başka or çift or satırsız):
        print('aynı: sözlükteki %d renk satırı Renkler ile eşleşiyor' % len(kod))
        return

    def adlar(k):
        return ', '.join(sorted(k))

    print('FARKLI: sözlükteki renk satırları Renkler ile aynı değil', file=sys.stderr)
    for etiket, liste, biçim in (
            ('sözlükte eksik', eksik, lambda en: '%s -> %s' % (en, adlar(kod[en]))),
            ('sözlükte fazla', fazla, lambda en: '%s -> %s' % (en, adlar(sözlük[en]))),
            ('türkçesi farklı', başka,
             lambda en: '%s: kod %s / sözlük %s' % (en, adlar(kod[en]), adlar(sözlük[en]))),
            ('aynı renge birden çok satır', çift, lambda en: '%s -> %s' % (en, adlar(sözlük[en]))),
            ('tek renge birden çok ad, satırı olmayan', satırsız,
             lambda en: '%s: %s (satırı olan: %s)'
                        % (en, adlar(kod[en] - set(sözlük[en])), adlar(set(sözlük[en]) & kod[en])))):
        if liste:
            print('  %s (%d):' % (etiket, len(liste)), file=sys.stderr)
            for en in liste:
                print('    %s' % biçim(en), file=sys.stderr)
    sys.exit('Sözlükteki key:"val" tablosunu tazeleyin: sozluk/koco-sozlugu.html\n'
             '(ve aynı dosyanın kojojs-editor\'daki kopyasını da -- ikisi birebir aynı olmalı)')


if __name__ == '__main__':
    main()
