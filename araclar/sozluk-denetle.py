#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-denetle.py -- sözlük sayfasındaki gömülü yardım bloğu yardim.json ile
aynı mı.

Neden: koco-sozlugu.html, yardim.json'un içeriğini `const YARDIM = {...}`
olarak GÖMÜLÜ taşıyor (sayfa hem iframe'de hem file:// ile açılıyor, fetch
çalışmazdı). Yani aynı veri iki yerde duruyor ve gömme adımı ELLE yapılıyor
-- json tazelenip gömme unutulursa sayfa eski veriyi gösterir, hiçbir şey
hata vermez.

Bu, akşam boyunca kovaladığımız "üretilmiş dosya kaynağından ayrıldı"
sınıfının son açık halkasıydı.

Kullanım:
  araclar/sozluk-denetle.py            # varsayılan yollar, fark varsa 1 döner
"""
import io
import json
import os
import sys

BAŞ = 'const YARDIM = {'


def gömülüBlok(html):
    """koco-sozlugu.html içindeki YARDIM bloğunu JSON metni olarak verir."""
    satırlar = html.split('\n')
    try:
        b = next(i for i, s in enumerate(satırlar) if s.startswith(BAŞ))
    except StopIteration:
        sys.exit('koco-sozlugu.html içinde "%s" bulunamadı' % BAŞ)
    try:
        s = next(i for i in range(b + 1, len(satırlar)) if satırlar[i].startswith('};'))
    except StopIteration:
        sys.exit('YARDIM bloğunun sonu ("};") bulunamadı')
    return '{\n' + '\n'.join(satırlar[b + 1:s]) + '\n}\n'


def main():
    kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    jsonYolu = sys.argv[1] if len(sys.argv) > 1 else os.path.join(kok, 'sozluk', 'yardim.json')
    htmlYolu = sys.argv[2] if len(sys.argv) > 2 else os.path.join(kok, 'sozluk', 'koco-sozlugu.html')

    with io.open(jsonYolu, encoding='utf-8') as d:
        kaynak = d.read()
    with io.open(htmlYolu, encoding='utf-8') as d:
        gömülü = gömülüBlok(d.read())

    if gömülü == kaynak:
        print('aynı: gömülü YARDIM bloğu == %s' % os.path.basename(jsonYolu))
        return

    # Farkı anlamlı anlatmak için anahtar düzeyinde karşılaştır.
    try:
        a, b = json.loads(kaynak), json.loads(gömülü)
    except ValueError as e:
        sys.exit('gömülü blok ayrıştırılamadı: %s' % e)

    eksik = sorted(set(a) - set(b))
    fazla = sorted(set(b) - set(a))
    başka = sorted(k for k in set(a) & set(b) if a[k] != b[k])
    print('FARKLI: gömülü YARDIM bloğu %s ile aynı değil' % os.path.basename(jsonYolu), file=sys.stderr)
    for etiket, liste in (('sayfada eksik', eksik), ('sayfada fazla', fazla), ('içeriği farklı', başka)):
        if liste:
            kesik = ' (ilk 8)' if len(liste) > 8 else ''
            print('  %s (%d)%s: %s' % (etiket, len(liste), kesik, ', '.join(liste[:8])),
                  file=sys.stderr)
    if not (eksik or fazla or başka):
        print('  anahtarlar aynı, yalnız biçim/sıra farkı', file=sys.stderr)
    sys.exit('Gömmeyi tazeleyin: yardim.json içeriğini koco-sozlugu.html\'deki\n'
             '`const YARDIM = {...}` bloğunun yerine koyun (bkz. sozluk/README.md).')


if __name__ == '__main__':
    main()
