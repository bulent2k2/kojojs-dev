#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tus-yazim-denetle.py -- tuş adlarının deve yazım göçü yerinde duruyor mu.

NEDEN: 2026-09'da tuş adları göçü (kojojs-dev#74, kojo#57) bütün BİRİNCİL
adları camelCase yaptı; on snake_case yazım @deprecated takma ada dönüştü.
O göçü koruyan hiçbir şey yoktu. İki ayrı kayıt bunu ölçtü:

  #76  bir @deprecated satırı silinirse hiçbir sınama kızarmıyor.
       (Annotation derleme zamanı bir işaret: değeri de, adın varlığını da
       değiştirmiyor. Scala.js'te yansıma yok; JVM'de de @deprecated
       varsayılan olarak CLASS saklamalı, yani çalışma anında sınanamıyor.)
  #77  yeni bir BİRİNCİL snake_case ad eklenirse hiçbir şey kızarmıyor;
       göç sessizce geri kaymaya başlıyor. Sınamalar ELLE yazılmış çift
       listeleri üstünde koşuyor, kaynak dosyayı hiç görmüyorlar.

TEK KURAL İKİSİNİ DE KAPATIYOR:

    tuşlar içindeki her snake_case `val`, @deprecated taşımalı.

  - Annotation silinirse: o snake_case val artık işaretsiz -> kırmızı (#76).
  - Yeni birincil snake_case eklenirse: işaretsiz -> kırmızı (#77).

Meşru olan tek şey, bilerek bırakılmış eskitilmiş takma adlar; onlar zaten
annotation taşıyor.

#78'in (çapraz depo ayrışması) burayla ilgisi yok: onu uretecler.yml'deki
"adlar anlık görüntüsü masaüstüyle güncel mi" adımı yakalıyor (#104).

Kullanım:
  araclar/tus-yazim-denetle.py [kojo-dizini]   # kojo verilirse iki depoyu da
                                               # denetler; verilmezse yalnız ikojo
"""
import io
import os
import re
import sys

IKOJO_YOL = 'src/main/scala/kojo/tr/klavye.scala'
KOJO_YOL = 'src/main/scala/net/kogics/kojo/lite/i18n/tr/klavye.scala'

# Türkçe harfler de ad karakteri: sayfa_aşağı, noktalı_virgül, satır_başı...
AD = r'[a-zçğıöşü][A-Za-zÇĞİÖŞÜçğıöşü0-9]*'
YILAN = re.compile(r'^\s*val\s+(%s(?:_%s)+)\s*[=:]' % (AD, AD))
DEVE = re.compile(r'^\s*val\s+(%s)\s*[=:]' % AD)
ESKİ = re.compile(r'^\s*@deprecated\b')


def işaretsizYılanlar(yol):
    """(satır, ad) -- @deprecated taşımayan snake_case val'ler."""
    satırlar = io.open(yol, encoding='utf-8').read().split('\n')
    kötü, yılanSayısı = [], 0
    for i, s in enumerate(satırlar):
        m = YILAN.match(s)
        if not m:
            continue
        yılanSayısı += 1
        # Hemen önceki BOŞ OLMAYAN satır @deprecated mı? Yorumlar araya
        # girebiliyor, o yüzden yorum satırlarını da atlıyoruz.
        j = i - 1
        while j >= 0 and (not satırlar[j].strip() or satırlar[j].lstrip().startswith('//')):
            j -= 1
        if j < 0 or not ESKİ.match(satırlar[j]):
            kötü.append((i + 1, m.group(1)))
    if yılanSayısı == 0:
        # Bugün on tane var. Sıfıra düşmesi ya göç tamamlandı (güzel) ya da
        # deyiş bozuldu (kötü) demek -- ikisi ayırt edilemediği için duruyoruz.
        sys.exit('%s içinde hiç snake_case val bulunamadı -- dosyanın biçimi\n'
                 'değişmiş olabilir; bu gözcünün deyişini gözden geçirin.' % yol)
    return kötü, yılanSayısı


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    hedefler = [('ikojo', os.path.join(kök, IKOJO_YOL))]
    if len(sys.argv) > 1:
        y = os.path.join(sys.argv[1], KOJO_YOL)
        if not os.path.exists(y):
            sys.exit('masaüstü klavye.scala bulunamadı: %s' % y)
        hedefler.append(('masaüstü', y))

    kötü = False
    for ne, yol in hedefler:
        işaretsiz, toplam = işaretsizYılanlar(yol)
        if işaretsiz:
            kötü = True
            print('::error::%s: @deprecated taşımayan snake_case tuş adı var '
                  '(%d tane)' % (ne, len(işaretsiz)), file=sys.stderr)
            for satır, ad in işaretsiz:
                print('    %s:%d  val %s' % (os.path.relpath(yol, kök), satır, ad),
                      file=sys.stderr)
        else:
            print('%-9s %d snake_case tuş adının hepsi @deprecated taşıyor' % (ne, toplam))

    if kötü:
        sys.exit('\nTuş adlarında deve yazım göçü bozulmuş.\n'
                 'Yeni bir ad ekliyorsanız camelCase yazın.\n'
                 'Eski bir yazımı takma ad olarak bırakıyorsanız @deprecated ekleyin\n'
                 '-- eskitmek silmek değil, o yazımla yazılmış yazılımcıklar\n'
                 'derlenmeye devam etmeli.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
