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

    tuşlar içindeki her snake_case `val`/`def`, @deprecated taşımalı.

  - Annotation silinirse: o snake_case tanım artık işaretsiz -> kırmızı (#76).
  - Yeni birincil snake_case eklenirse: işaretsiz -> kırmızı (#77).

Meşru olan tek şey, bilerek bırakılmış eskitilmiş takma adlar; onlar zaten
annotation taşıyor.

#78'in (çapraz depo ayrışması) burayla ilgisi yok: onu uretecler.yml'deki
"adlar anlık görüntüsü masaüstüyle güncel mi" adımı yakalıyor (#104).

Kullanım:
  araclar/tus-yazim-denetle.py [kojo-dizini]   # kojo verilirse iki depoyu da
                                               # denetler; verilmezse yalnız iKojo
"""
import io
import os
import re
import sys

IKOJO_YOL = 'src/main/scala/kojo/tr/klavye.scala'
KOJO_YOL = 'src/main/scala/net/kogics/kojo/lite/i18n/tr/klavye.scala'

# Türkçe harfler de ad karakteri: sayfa_aşağı, noktalı_virgül, satır_başı...
AD = r'[a-zçğıöşü][A-Za-zÇĞİÖŞÜçğıöşü0-9]*'
# `val` kadar `def` de: snake_case bir def aynı biçem gerilemesi olurdu.
# Satır başı kadar `;` sonrası da: dosyanın kendi üslubu bu (bkz. rakam ve
# harf kodlarının yazıldığı `val n0 = 0x30; val n1 = 0x31; ...` satırları),
# yani oraya eklenecek bir ad yalnız satır başına bakan bir deyişe görünmezdi.
YILAN = re.compile(r'(?:^|;)\s*(?:val|def)\s+(%s(?:_%s)+)\s*[=:]' % (AD, AD))
ESKİ = re.compile(r'^\s*@deprecated\b')


def eskitilmişMi(satırlar, i):
    """i. satırdaki tanımın üstünde @deprecated var mı.

    "Hemen üstteki boş olmayan satır" yetmiyor, çünkü annotation birkaç
    satıra sarabiliyor:

        @deprecated(
          "camelCase yazıma geçildi: silGeri kullanın",
          "Eylül 2026")
        val sil_geri = silGeri

    Bugünkü en uzun annotation 89 karakter; birkaç harf daha uzun bir ad
    sarmayı kendiliğinden davet ediyor. O yüzden parantez dengesini sayıp
    MANTIKSAL satır başlarına bakıyoruz. Bir önceki tanıma (ya da süslü
    paranteze) çarpınca duruyoruz -- yoksa komşunun annotation'ını
    kendimize sayardık.
    """
    denge = 0
    for j in range(i - 1, -1, -1):
        s = satırlar[j]
        çıplak = s.strip()
        if not çıplak or çıplak.startswith('//'):
            continue
        # Denge sayımından ÖNCE: ileti metnindeki dengesiz bir parantez
        # (`@deprecated("bkz. sayfa 3)", ...)`) annotation'ı atlatmasın.
        if ESKİ.match(s):
            return True
        denge += s.count(')') - s.count('(')
        if denge > 0:
            continue          # sarmış bir yapının ortasındayız, başı yukarıda
        denge = 0
        if çıplak.startswith('@'):
            continue          # başka bir annotation (@inline gibi) -- geç
        return False          # tanım, süslü parantez, ne olursa: blok bitti
    return False


def işaretsizYılanlar(yol):
    """(satır, ad) -- @deprecated taşımayan snake_case val/def'ler."""
    satırlar = io.open(yol, encoding='utf-8').read().split('\n')
    kötü, yılanSayısı = [], 0
    for i, s in enumerate(satırlar):
        for m in YILAN.finditer(s):
            yılanSayısı += 1
            # Satır içinde İKİNCİ bir tanıma annotation yazılamaz; oradaki
            # snake_case ad tanımı gereği işaretsizdir.
            satırBaşında = not s[:m.start()].strip()
            if satırBaşında and eskitilmişMi(satırlar, i):
                continue
            kötü.append((i + 1, m.group(1)))
    if yılanSayısı == 0:
        # Bugün on tane var. Sıfıra düşmesi ya göç tamamlandı (güzel) ya da
        # deyiş bozuldu (kötü) demek -- ikisi ayırt edilemediği için duruyoruz.
        sys.exit('%s içinde hiç snake_case tanım bulunamadı -- dosyanın biçimi\n'
                 'değişmiş olabilir; bu gözcünün deyişini gözden geçirin.' % yol)
    return kötü, yılanSayısı


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    hedefler = [('iKojo', os.path.join(kök, IKOJO_YOL))]
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
                print('    %s:%d  %s' % (os.path.relpath(yol, kök), satır, ad),
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
