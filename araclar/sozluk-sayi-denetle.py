#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-sayi-denetle.py -- indeks sayfasındaki Türkçe girdi sayısı sözlüğün
gerçek sayısıyla aynı mı.

NEDEN: kojo-sozlukleri.html (Kojo Sözlükleri indeksi) Türkçe kartında ve
karşılaştırma tablosunda bir "Entries" sayısı taşıyor. O sayı ELLE tutuluyor
ve hiçbir şey denetlemiyordu: sözlük büyüdükçe indeks olduğu yerde kaldı.
2026-09'da ölçüldü -- yayımdaki artifact 955, depodaki dosya 975 diyordu,
sözlüğün kendi rozeti ise 1238 basıyordu. Üç ayrı sayı, üçü de elle.

ÖLÇÜT: satır sayısı -- koco-sozlugu.html'deki CATS dizisindeki toplam satır.
Sözlük sayfasının KENDİ "girdi / entries" rozeti de bunu basıyor
(renderStats: CATS.reduce((n, c) => n + c.rows.length, 0)), yani indeksten
sözlüğe tıklayan biri aynı sayıyı görüyor. Benzersiz terim saymak da
savunulabilirdi (1216 ayrı Türkçe ad var, aynı ad birden çok türde geçiyor)
ama o, sözlüğün bastığı sayıyla çelişirdi.

Kullanım:
  araclar/sozluk-sayi-denetle.py            # uyuşmazsa 1 döner
"""
import io
import json
import os
import re
import sys

BAŞ = 'const CATS = ['
SON = '];'
# Türkçe kart:   <span class="k">Entries</span><span class="v">1238</span>
KART = re.compile(r'<span class="k">Entries</span><span class="v">(\d+)</span>')
# Tablo satırı:  <td class="lang tr">Türkçe</td><td>Kaplumbağa</td><td class="n">1238</td>
TABLO = re.compile(r'<td class="lang tr">.*?<td class="n">(\d+)</td>')


def satırSayısı(yol):
    """koco-sozlugu.html'deki CATS dizisindeki toplam satır."""
    satırlar = io.open(yol, encoding='utf-8').read().split('\n')
    try:
        b = next(i for i, s in enumerate(satırlar) if s.startswith(BAŞ))
    except StopIteration:
        sys.exit('koco-sozlugu.html içinde "%s" bulunamadı' % BAŞ)
    try:
        s = next(i for i in range(b + 1, len(satırlar)) if satırlar[i].startswith(SON))
    except StopIteration:
        sys.exit('CATS dizisinin sonu ("%s") bulunamadı' % SON)

    n = 0
    for ham in satırlar[b + 1:s]:
        d = ham.strip().rstrip(',')
        if not d.startswith('['):
            continue  # kategori başlığı ya da kapanış -- satır değil
        try:
            öge = json.loads(d)
        except ValueError as e:
            sys.exit('CATS içinde ayrıştırılamayan satır: %s\n  %s' % (e, d[:90]))
        if not (isinstance(öge, list) and len(öge) == 3):
            sys.exit('beklenmeyen satır biçimi (3 alan bekleniyordu): %s' % d[:90])
        n += 1
    if n == 0:
        sys.exit('CATS içinde hiç satır bulunamadı -- biçim değişmiş olabilir')
    return n


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    sözlük = os.path.join(kök, 'sozluk', 'koco-sozlugu.html')
    indeks = os.path.join(kök, 'sozluk', 'kojo-sozlukleri.html')

    gerçek = satırSayısı(sözlük)
    html = io.open(indeks, encoding='utf-8').read()

    bulgular = []
    for etiket, deyiş in (('Türkçe kartı', KART), ('karşılaştırma tablosu', TABLO)):
        m = deyiş.search(html)
        if not m:
            sys.exit('kojo-sozlukleri.html içinde %s sayısı bulunamadı -- '
                     'sayfanın biçimi değişmiş olabilir' % etiket)
        bulgular.append((etiket, int(m.group(1))))

    kötü = [(e, s) for e, s in bulgular if s != gerçek]
    if kötü:
        print('HATA: indeksteki Türkçe girdi sayısı sözlükle uyuşmuyor --', file=sys.stderr)
        print('      koco-sozlugu.html gerçek satır sayısı: %d' % gerçek, file=sys.stderr)
        for etiket, sayı in kötü:
            print('  kojo-sozlukleri.html %s: %d' % (etiket, sayı), file=sys.stderr)
        print('Sayıyı %d yapın (iki yerde de).' % gerçek, file=sys.stderr)
        return 1

    print('aynı: indeksteki Türkçe girdi sayısı %d, sözlükle eşleşiyor (iki yerde de)' % gerçek)
    return 0


if __name__ == '__main__':
    sys.exit(main())
