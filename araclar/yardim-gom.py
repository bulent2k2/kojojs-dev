#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
yardim-gom.py -- sozluk/yardim.json + sozluk/yardim-ikojo.json birleşimini
sozluk/koco-sozlugu.html içindeki `const YARDIM = {...}` bloğuna gömer.

Eskiden bu adım elle yapılıyordu ("bloğu bu dosyayla değiştirin"); sozluk-denetle.py
farkı yakalıyordu ama düzeltmek için araç yoktu. Bu betik o boşluğu kapatır;
çalıştırınca sozluk-denetle.py "aynı" der.

Kullanım:
  araclar/yardim-gom.py                # varsayılan yollar
  araclar/yardim-gom.py --denetle      # yazmadan yalnız fark var mı (1 döner)
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import yardim_birlestir  # noqa: E402

BAŞ = 'const YARDIM = {'


def main():
    kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    jsonYolu = os.path.join(kok, 'sozluk', 'yardim.json')
    ikojoYolu = os.path.join(kok, 'sozluk', 'yardim-ikojo.json')
    htmlYolu = os.path.join(kok, 'sozluk', 'koco-sozlugu.html')
    yalnızDenetle = '--denetle' in sys.argv[1:]

    with io.open(jsonYolu, encoding='utf-8') as d:
        birleşik = yardim_birlestir.birlesik(d.read(), ikojoYolu)
    with io.open(htmlYolu, encoding='utf-8') as d:
        html = d.read()

    satırlar = html.split('\n')
    b = next((i for i, s in enumerate(satırlar) if s.startswith(BAŞ)), None)
    if b is None:
        sys.exit('koco-sozlugu.html içinde "%s" bulunamadı' % BAŞ)
    e = next((i for i in range(b + 1, len(satırlar)) if satırlar[i].startswith('};')), None)
    if e is None:
        sys.exit('YARDIM bloğunun sonu ("};") bulunamadı')

    # birleşik "{\n  satırlar...\n}\n": ilk ve son satır çerçeve
    gövde = birleşik.rstrip('\n').split('\n')[1:-1]
    yeni = satırlar[:b + 1] + gövde + satırlar[e:]
    if yeni == satırlar:
        print('aynı: gömülü YARDIM bloğu zaten güncel')
        return
    if yalnızDenetle:
        sys.exit('FARKLI: gömülü YARDIM bloğu eski; araclar/yardim-gom.py çalıştırın')
    with io.open(htmlYolu, 'w', encoding='utf-8', newline='') as d:
        d.write('\n'.join(yeni))
    print('yazıldı: %s (%d girdi)' % (os.path.normpath(htmlYolu), len(gövde)))


if __name__ == '__main__':
    main()
