#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-baglanti-denetle.py -- sözlük sayfasındaki bağlantılar iframe'den
çıkabiliyor mu.

NEDEN: koco-sozlugu.html ikojo'da TEK BAŞINA açılmıyor; /yardim/sozluk
sayfası onu bir IFRAME içine koyuyor (kojojs-editor: yardimSozluk.scala.html).
`target` taşımayan bir dış bağlantı iframe'in KENDİSİNİ götürüyor, ve
çerçevelenmeyi reddeden bir site (claude.ai gibi) oraya düşünce kullanıcı
"refused to connect" görüyor -- sözlük ekrandan kayboluyor.

Bu kusur 2026-09'da canlıda bulundu: masthead'deki "◂ Kojo Sözlükleri"
bağlantısı target'sizdi. Sinsi yanı şu: sayfayı DOĞRUDAN açınca (assets/...
ile) aynı bağlantı düzgün çalışıyor, yani dosyaya tek başına bakan kimse
sorunu göremiyor. CI'da yakalanacak şey tam bu.

Kullanım:
  araclar/sozluk-baglanti-denetle.py        # kusur varsa 1 döner
"""
import io
import os
import re
import sys

# Elle yazılmış <a ...> etiketleri. JS ile üretilenler (şablon dizgisi içinde,
# `${...}` taşıyanlar) burada değil -- onları çalışma anında kuran kod kendi
# target'ını yazıyor ve düz metin taramasıyla güvenilir ayrıştırılamazlar.
ETİKET = re.compile(r'<a\s[^>]*href="(?P<url>[^"]*)"[^>]*>')
ŞABLON = re.compile(r'\$\{')
DIŞ = re.compile(r'^(?:https?:)?//')


def denetle(yol):
    html = io.open(yol, encoding='utf-8').read()
    kusurlu = []
    for m in ETİKET.finditer(html):
        etiket = m.group(0)
        if ŞABLON.search(etiket):
            continue  # JS şablonu; aşağıdaki nota bak
        url = m.group('url')
        if not DIŞ.match(url):
            continue  # site içi bağlantı: iframe içinde kalması sorun değil
        if 'target=' not in etiket:
            satır = html.count('\n', 0, m.start()) + 1
            kusurlu.append((satır, url))
    return kusurlu


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    yol = os.path.join(kök, 'sozluk', 'koco-sozlugu.html')
    kusurlu = denetle(yol)
    if kusurlu:
        print('HATA: iframe içinde kırılacak dış bağlantı(lar) var --')
        print('      target="_blank" rel="noopener" eklenmeli:')
        for satır, url in kusurlu:
            print('  %s:%d  %s' % (os.path.relpath(yol, kök), satır, url))
        return 1
    print('aynı: sözlükteki elle yazılmış dış bağlantıların hepsi iframe\'den çıkıyor')
    return 0


if __name__ == '__main__':
    sys.exit(main())
