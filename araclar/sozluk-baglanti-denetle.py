#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-baglanti-denetle.py -- sözlük sayfalarındaki bağlantılar iframe'den
çıkabiliyor mu.

NEDEN: koco-sozlugu.html iKojo'da TEK BAŞINA açılmıyor; /yardim/sozluk
sayfası onu bir IFRAME içine koyuyor (kojojs-editor: yardimSozluk.scala.html).
Çerçeveden çıkmayan bir dış bağlantı iframe'in KENDİSİNİ götürüyor, ve
çerçevelenmeyi reddeden bir site (claude.ai gibi) oraya düşünce kullanıcı
"refused to connect" görüyor -- sözlük ekrandan kayboluyor.

Bu kusur 2026-09'da canlıda bulundu: masthead'deki "◂ Kojo Sözlükleri"
bağlantısı target'sizdi. Sinsi yanı şu: sayfayı DOĞRUDAN açınca (assets/...
ile) aynı bağlantı düzgün çalışıyor, yani dosyaya tek başına bakan kimse
sorunu göremiyor. CI'da yakalanacak şey tam bu.

ÖLÇÜT "target VAR MI" DEĞİL, "DEĞERİ ÇERÇEVEDEN ÇIKARIYOR MU". İlk sürüm
yalnız `target=` dizgisini arıyordu; `target="_self"` -- ki tanımı gereği
BULUNULAN çerçeve demek, yani kusurun kendisi -- gözcüden geçiyordu.
Çerçeveden çıkaran iki değer var: _blank (yeni sekme, yeğlenen) ve _top
(en üst çerçeve; kusuru önler ama kullanıcıyı iKojo'dan çıkarır).

Kullanım:
  araclar/sozluk-baglanti-denetle.py        # kusur varsa 1 döner
"""
import glob
import io
import os
import re
import sys

# href'i çift YA DA tek tırnaklı olabilir: ikisi de geçerli HTML. İlk sürüm
# yalnız çift tırnağı görüyordu, tek tırnaklı kusurlu bir bağlantı sessizce
# atlanıyordu.
ETİKET = re.compile(r'<a\s[^>]*?href\s*=\s*(?P<t>["\'])(?P<url>[^"\']*)(?P=t)[^>]*>')
HEDEF = re.compile(r'\btarget\s*=\s*(?:(["\'])(?P<a>[^"\']*)\1|(?P<b>[^\s"\'>]+))')
REL = re.compile(r'\brel\s*=\s*(?:(["\'])(?P<a>[^"\']*)\1|(?P<b>[^\s"\'>]+))')
DIŞ = re.compile(r'^(?:https?:)?//')
# JS ile üretilen bağlantılar: target'ını çalışma anında kuran kod yazıyor ve
# düz metin taramasıyla güvenilir ayrıştırılamazlar. Ölçüt olarak ETİKETİN
# TÜMÜNDE değil yalnız HREF DEĞERİNDE `${...}` arıyoruz -- yoksa içinde
# `${` geçen ELLE yazılmış bir bağlantı da elenirdi.
ŞABLON = re.compile(r'\$\{')

ÇIKAN = ('_blank', '_top')


def değer(m):
    return (m.group('a') if m.group('a') is not None else m.group('b')) if m else None


def denetle(yol):
    html = io.open(yol, encoding='utf-8').read()
    kusurlu = []
    for m in ETİKET.finditer(html):
        etiket, url = m.group(0), m.group('url')
        if ŞABLON.search(url):
            continue  # JS şablonu; yukarıdaki nota bak
        if not DIŞ.match(url):
            continue  # site içi bağlantı: iframe içinde kalması sorun değil
        satır = html.count('\n', 0, m.start()) + 1
        hedef = değer(HEDEF.search(etiket))
        if hedef is None:
            kusurlu.append((satır, url, 'target yok'))
        elif hedef.lower() not in ÇIKAN:
            kusurlu.append((satır, url, 'target="%s" çerçeveden çıkarmıyor' % hedef))
        elif hedef.lower() == '_blank' and 'noopener' not in (değer(REL.search(etiket)) or ''):
            # _blank açılan sayfaya window.opener veriyor; noopener onu koparır.
            # _top'ta yeni bağlam yok, orada anlamsız -- şart koşmuyoruz.
            kusurlu.append((satır, url, 'rel="noopener" eksik'))
    return kusurlu


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    yollar = sorted(glob.glob(os.path.join(kök, 'sozluk', '*.html')))
    if not yollar:
        sys.exit('sozluk/ içinde .html bulunamadı')
    kusurlu = [(yol, s, u, n) for yol in yollar for s, u, n in denetle(yol)]
    if kusurlu:
        print('HATA: iframe içinde kırılacak dış bağlantı(lar) var --')
        print('      target="_blank" rel="noopener" eklenmeli:')
        for yol, satır, url, neden in kusurlu:
            print('  %s:%d  %s  (%s)' % (os.path.relpath(yol, kök), satır, url, neden))
        return 1
    print('aynı: %d sayfadaki elle yazılmış dış bağlantıların hepsi iframe\'den çıkıyor'
          % len(yollar))
    return 0


if __name__ == '__main__':
    sys.exit(main())
