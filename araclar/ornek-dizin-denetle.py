#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ornek-dizin-denetle.py -- örnekler sayfasının KLON İSTEMEYEN savları.

kilavuz/html/ornekler.html'i kilavuz/ornekler.py üretiyor, ama listeyi
DİZİNDEN DEĞİL ornekler/README.md'deki tablodan okuyor (ikojo_ornekleri) ve
başlıkları masaüstü klonundan alıyor. Sayfanın tamamını klonsuz yeniden
üretmek bu yüzden mümkün değil; buradaki savlar o tam yeniden üretimin
(uretecler.yml'de, yalnız master'da) YERİNE değil YANINA.

Dört ayrışma, dördü de sessiz:

  1. dosya var, tabloda yok   -> örnek sayfada HİÇ görünmez
  2. tabloda var, dosya yok   -> ölü satır
  3. tabloda var, sayfada yok -> tablo güncellendi, sayfa tazelenmedi
                                 (#138'de tam bu oldu: 14 ve 15 aylarca yoktu)
  4. tabloda var, başlığı yok -> sayfada HAM DOSYA ADIYLA görünür
                                 (IKOJO_BASLIK.get(ad, ad) geri düşüşü)

3. sav zayıf: yalnız EKSİK satırı görür. Bayat açıklamayı, değişmiş başlığı,
ya da silinmiş örneğin sayfada kalmasını göremez -- onlar için master'daki
tam yeniden üretim gerekiyor.

TABLOYU ÜRETECİN KENDİSİNE AYRIŞTIRTIYORUZ: desen buraya kopyalanmıyor.
Kopyalanmıştı ve kopya DAHA GEVŞEKTİ -- üretecin deseni `\\s*(.+?)\\s*\\|$`
ile kapanış borusunu şart koşuyor, kopya koşmuyordu. Satır sonuna görünmez
bir boşluk koymak üreteci 15'ten 14'e düşürürken denetçiyi yeşil bırakıyordu:
yakalamak için yazılmış aracın içinde, yakalaması gereken kusur (#141
incelemesi).

Kullanım:
  araclar/ornek-dizin-denetle.py        # ayrışma varsa 1 döner
"""
import importlib.util
import io
import os
import sys

KOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')


def üreteci_al():
    """kilavuz/ornekler.py'yi içe aktarır (yan etkisiz: main() __main__ korumalı)."""
    yol = os.path.join(KOK, 'kilavuz', 'ornekler.py')
    spec = importlib.util.spec_from_file_location('ornekler_uretec', yol)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def main():
    üreteç = üreteci_al()
    ornekler = os.path.join(KOK, 'ornekler')

    tabloda = {ad for ad, _ in üreteç.ikojo_ornekleri()}
    diskte = {f for f in os.listdir(ornekler) if f.endswith('.kojo')}
    sayfa_yolu = os.path.join(KOK, 'kilavuz', 'html', 'ornekler.html')
    sayfa = io.open(sayfa_yolu, encoding='utf-8').read()
    başlıklı = set(üreteç.IKOJO_BASLIK)

    sorunlar = []
    for ad, küme, başlık, çözüm in (
        ('tablosuz', diskte - tabloda, 'dosya var ama README tablosunda YOK',
         'ornekler/README.md tablosuna satır ekleyin (sayfada hiç görünmezler)'),
        ('dosyasız', tabloda - diskte, 'tabloda var ama DOSYA yok',
         'satırı kaldırın ya da dosyayı geri koyun'),
        ('sayfasız', {a for a in tabloda if a not in sayfa}, 'tabloda var ama SAYFADA yok',
         'sayfa tazelenmemiş: python3 kilavuz/ornekler.py --kojo <kojo klonu>'),
        ('başlıksız', tabloda - başlıklı, 'tabloda var ama BAŞLIĞI yok',
         'kilavuz/ornekler.py içindeki IKOJO_BASLIK\'a ekleyin '
         '(yoksa sayfada ham dosya adıyla görünür)'),
    ):
        if küme:
            sorunlar.append((başlık, sorted(küme), çözüm))

    if not sorunlar:
        print('aynı: %d örnek -- dizin, README tablosu, sayfa ve başlıklar tutuyor' % len(tabloda))
        return

    for başlık, liste, çözüm in sorunlar:
        print('%s (%d): %s' % (başlık, len(liste), ', '.join(liste)), file=sys.stderr)
        print('  -> %s' % çözüm, file=sys.stderr)
    sys.exit(1)


if __name__ == '__main__':
    main()
