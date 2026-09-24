#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Yardım sayfalarının gezinti şeridi her yerde AYNI sırada mı.

  python3 araclar/gezinti-denetle.py [--editor <kojojs-editor kökü>] [--editorsuz]

NEDEN AYRI BİR SAV: şerit ÜÇ ayrı yerde kopyalı ve hiçbiri ötekini görmüyor --

  kilavuz/uret.py      GEZINTI listesi   -> yardimSkala, yardimKomutlar
  kilavuz/ornekler.py  gömülü HTML       -> yardimOrnekler
  kojojs-editor        elle yazılmış     -> yardim, yardimSozluk, yardimFarklar

Biri değişip ötekiler kalırsa çubuk sayfadan sayfaya farklı sıralanır. Kullanıcı
bunu hemen görür ama derleme, sınama ve dağıtım sessizce geçer: uret.py'nin
tazelik savı yalnız KENDİ ürettiğini denetliyor, ornekler.py ise CI'da hiç
koşmuyordu. Eylül 2026'da sıra değiştirilirken bu üç kopya elle hizalandı;
bu betik bir daha ayrışmasınlar diye var.

uret.py'deki GEZINTI listesi TEK DOĞRU KAYNAK sayılır; ötekiler ona uymalı.

EDİTÖR YOKSA KIRMIZI (#151): ilk sürüm editör klonunu bulamayınca uyarı basıp
0 ile çıkıyordu; CI'da klon olmadığı için üçüncü kopya hiç denetlenmiyor ve
adım yapısal olarak hep yeşildi -- "yeşil ama bakmadım". Artık editör
bulunamazsa 2 ile çıkar; yalnız bu depoyu denetlemek BİLİNÇLİ bir seçim olmalı
(--editorsuz). Bir de klonun hangi commit'te olduğu yazılır: yerel klon bayat
olabiliyor (kojojs-editor#43 öncesi bir klon yanlış alarm vermişti), yanlış
alarmı gerçek ayrışmadan ayırmanın yolu bu satır.
"""
import argparse
import os
import re
import subprocess
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
VARSAYILAN_EDITOR = os.path.join(KOK, '..', 'kojojs-editor')

# Elle tutulan sayfalar (kojojs-editor). Üretilenler buraya girmez: onlar
# zaten uret.py/ornekler.py çıktısı, yani kaynağı denetlemek yetiyor.
ELLE = ['yardim.scala.html', 'yardimSozluk.scala.html', 'yardimFarklar.scala.html']

BAG = re.compile(r'<a href="(/yardim(?:/[a-z]+)?)"[^>]*>([^<]+)</a>')


def beklenen():
    """uret.py'deki GEZINTI listesi -- tek doğru kaynak."""
    s = open(os.path.join(KOK, 'kilavuz', 'uret.py'), encoding='utf-8').read()
    m = re.search(r'^GEZINTI = \[(.*?)\]$', s, re.M | re.S)
    if not m:
        sys.exit('hata: kilavuz/uret.py içinde GEZINTI listesi bulunamadı')
    return re.findall(r"\('([^']+)', '([^']+)'\)", m.group(1))


def klon_durumu(kok):
    """Editör klonunun HEAD'i ve origin/master'ın kaç commit gerisinde olduğu
    (git yoksa ya da klon değilse None). Ağ yok: yalnız yerelde bilineni söyler,
    yani "gerisinde" son fetch'e göredir."""
    def git(*args):
        r = subprocess.run(['git', '-C', kok] + list(args), capture_output=True, text=True)
        return r.stdout.strip() if r.returncode == 0 else None
    bas = git('rev-parse', '--short', 'HEAD')
    if bas is None:
        return None
    dal = git('rev-parse', '--abbrev-ref', 'HEAD') or '?'
    geri = git('rev-list', '--count', 'HEAD..origin/master')
    durum = '%s (%s)' % (bas, dal)
    if geri is None:
        durum += ', origin/master bilinmiyor'
    elif geri != '0':
        durum += ', origin/master\'ın %s commit gerisinde (son fetch\'e göre) -- BAYAT olabilir' % geri
    return durum


def seritten(metin):
    """<nav class="gezinti"> ... </nav> içindeki (yol, etiket) çiftleri."""
    m = re.search(r'<nav class="gezinti">(.*?)</nav>', metin, re.S)
    if not m:
        return None
    return BAG.findall(m.group(1))


def main():
    p = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    p.add_argument('--editor', default=VARSAYILAN_EDITOR,
                   help='kojojs-editor kökü (varsayılan: ../kojojs-editor)')
    p.add_argument('--editorsuz', action='store_true',
                   help='editör klonu olmadan yalnız bu depoyu denetle (bilinçli seçim; '
                        'yoksa editör bulunamayınca 2 ile çıkar)')
    a = p.parse_args()

    bek = beklenen()
    print('beklenen sıra (kilavuz/uret.py): %s' % ' '.join(e for _, e in bek))

    kotu = []

    # 1) ornekler.py'deki gömülü şerit
    s = open(os.path.join(KOK, 'kilavuz', 'ornekler.py'), encoding='utf-8').read()
    bulunan = seritten(s)
    if bulunan is None:
        kotu.append(('kilavuz/ornekler.py', 'gezinti şeridi bulunamadı'))
    elif bulunan != bek:
        kotu.append(('kilavuz/ornekler.py', ' '.join(e for _, e in bulunan)))

    # 2) kojojs-editor'deki elle tutulan sayfalar
    views = os.path.join(a.editor, 'server', 'src', 'main', 'twirl', 'views')
    if a.editorsuz:
        print('editörsüz: kojojs-editor denetlenmedi (--editorsuz); yalnız bu depo')
    elif not os.path.isdir(views):
        print('hata: kojojs-editor bulunamadı (%s).\n'
              '      Üçüncü kopya denetlenmeden yeşil demek "bakmadım" demek (#151). '
              'Klonu --editor ile ver;\n'
              '      yalnız bu depoyu denetlemek istiyorsan --editorsuz de.' % views,
              file=sys.stderr)
        return 2
    else:
        durum = klon_durumu(os.path.abspath(a.editor))
        print('editör: %s%s' % (os.path.abspath(a.editor), (' @ ' + durum) if durum else ''))
        for ad in ELLE:
            yol = os.path.join(views, ad)
            if not os.path.exists(yol):
                kotu.append((ad, 'dosya yok'))
                continue
            bulunan = seritten(open(yol, encoding='utf-8').read())
            if bulunan is None:
                kotu.append((ad, 'gezinti şeridi bulunamadı'))
            elif bulunan != bek:
                kotu.append((ad, ' '.join(e for _, e in bulunan)))

    if kotu:
        print('\nhata: %d yerde şerit beklenenden farklı:' % len(kotu), file=sys.stderr)
        for nerede, ne in kotu:
            print('       %-28s %s' % (nerede, ne), file=sys.stderr)
        print('       Tek doğru kaynak kilavuz/uret.py; ötekileri ona uydurun.', file=sys.stderr)
        return 1

    print('aynı: şerit denetlenen her yerde aynı sırada%s'
          % (' (editör hariç)' if a.editorsuz else ''))
    return 0


if __name__ == '__main__':
    sys.exit(main())
