#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ornek-dizin-denetle.py -- ornekler/ dizini ile ornekler/README.md tablosu
birbirini tutuyor mu.

Neden: kilavuz/html/ornekler.html'i üreten kilavuz/ornekler.py örnek listesini
DİZİNDEN DEĞİL, README.md'deki tablodan okuyor (ikojo_ornekleri). Yani bir
örnek dizine eklenip tabloya yazılmazsa sayfada HİÇ görünmez -- ve üreteci
koşturmak da bunu göstermez, çünkü üretim tabloyla zaten tutarlıdır.

O yüzden "üretim ağaçtakini değiştirmemeli" savı (uretecler.yml'deki örnekler
sayfası adımı) bu durumu YAKALAMIYOR: o, tablo değişip sayfa tazelenmeyince
konuşuyor. Burası öteki yarısı -- tablo ile dizinin ayrışması.

Klon istemiyor, saniyenin altında koşuyor: her PR'da koşabilir.

Kullanım:
  araclar/ornek-dizin-denetle.py        # fark varsa 1 döner
"""
import io
import os
import re
import sys

KOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
SATIR = re.compile(r'^\|\s*`([^`]+\.kojo)`\s*\|', re.M)


def main():
    ornekler = os.path.join(KOK, 'ornekler')
    okuma = os.path.join(ornekler, 'README.md')
    # ikojo_ornekleri ile AYNI düzenli ifade: burada tutup orada kaçırmayalım
    tabloda = set(SATIR.findall(io.open(okuma, encoding='utf-8').read()))
    diskte = {f for f in os.listdir(ornekler) if f.endswith('.kojo')}

    yazısız = sorted(diskte - tabloda)
    dosyasız = sorted(tabloda - diskte)
    if not yazısız and not dosyasız:
        print('aynı: ornekler/ dizinindeki %d örneğin hepsi README tablosunda' % len(diskte))
        return

    if yazısız:
        print('README tablosunda YOK (%d): %s' % (len(yazısız), ', '.join(yazısız)), file=sys.stderr)
        print('  -> sayfada hiç görünmezler; ornekler/README.md tablosuna satır ekleyin,', file=sys.stderr)
        print('     sonra: python3 kilavuz/ornekler.py --kojo <kojo klonu>', file=sys.stderr)
    if dosyasız:
        print('tabloda var ama DOSYA yok (%d): %s' % (len(dosyasız), ', '.join(dosyasız)), file=sys.stderr)
        print('  -> satırı kaldırın ya da dosyayı geri koyun', file=sys.stderr)
    sys.exit(1)


if __name__ == '__main__':
    main()
