#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
pin-denetle.py -- araclar/kojo-cevirmen-surumu.txt (bu depo) ile kojojs-core'un
router/CEVIRMEN_SURUMU'ndaki çevirmen commit'i kopmuş mu.

NEDEN: çevirmen iki yerde SABİT bir kojo commit'ine bağlı. Burada `cevir-derle` ölçüm kapısı
kendi pinini kullanıyor; sitedeki Çevir düğmesi ise kojojs-core'daki router'ın kopyasını. Kopunca
hiçbir şey kırmızı yanmıyordu: bu depo düzeltilmiş çevirmeni ölçüp "geçti" derken canlı site eski
kopyayla `Picture.eskizden` üretti (kojojs-core#80).

KARAR (iki depo tek PR'da değişemiyor):
  aynı                       geçer
  bu depo GERİDE             kırmızı: pini yükseltin (kojo-cevirmen-surumu.txt başındaki adımlar)
  bu depo ÖNDE               geçer, UYARI: kojojs-core de yükseltilmeli (yükseltme PR'ı kendi
                             kendini kırmızıya çevirmesin; karşı yönü kojojs-core'daki
                             router/pin-denetle.sh kapatıyor)
  ayrışmış                   kırmızı (biri öbürünün atası değil)

Kullanım:
  araclar/pin-denetle.py --core <kojojs-core klonu> --kojo <kojo klonu, geçmişiyle>
"""
import argparse
import os
import re
import subprocess
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
SURUM = os.path.join(BURASI, 'kojo-cevirmen-surumu.txt')


def sha_oku(yol):
    for s in open(yol, encoding='utf-8'):
        s = s.strip()
        if s and not s.startswith('#') and re.fullmatch(r'[0-9a-f]{40}', s):
            return s
    return None


def git(kojo, *a):
    return subprocess.run(['git', '-C', kojo] + list(a), capture_output=True, text=True)


def ata_mi(kojo, ata, torun):
    return git(kojo, 'merge-base', '--is-ancestor', ata, torun).returncode == 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--core', required=True)
    ap.add_argument('--kojo', required=True)
    a = ap.parse_args()
    benim = sha_oku(SURUM)
    onun = sha_oku(os.path.join(a.core, 'router', 'CEVIRMEN_SURUMU'))
    if not benim:
        sys.exit('HATA: %s içinde 40 haneli SHA yok' % SURUM)
    if not onun:
        sys.exit('HATA: kojojs-core/router/CEVIRMEN_SURUMU içinde 40 haneli SHA yok')
    if benim == onun:
        print('aynı: kojojs-dev ve router çevirmeni kojo@%s' % benim[:12])
        return 0
    for s in (benim, onun):
        if git(a.kojo, 'cat-file', '-e', s + '^{commit}').returncode != 0:
            sys.exit('HATA: %s kojo klonunda yok (git fetch)' % s)
    if ata_mi(a.kojo, benim, onun):
        print('::error::kojojs-dev çevirmeni (kojo@%s) router\'dakinden (kojo@%s) GERİDE.' % (benim[:12], onun[:12]))
        print('Ölçüm kapısı (cevir-derle) sitenin kullandığından eski çevirmeni ölçüyor.')
        print('Düzeltme: araclar/kojo-cevirmen-surumu.txt\'yi %s yapın, ' % onun)
        print('araclar/cevir-derle.py --kojo ../kojo --guncelle koşun, ikisini aynı commit\'te gönderin.')
        return 1
    if ata_mi(a.kojo, onun, benim):
        print('::warning::kojojs-dev çevirmeni (kojo@%s) router\'dakinden (kojo@%s) ÖNDE. '
              'kojojs-core de yükseltilmeli: router/CEVIRMEN_SURUMU + router/cevirmen-esitle.sh.' % (benim[:12], onun[:12]))
        return 0
    print('::error::kojojs-dev (kojo@%s) ve router (kojo@%s) çevirmen commit\'leri AYRIŞMIŞ (biri öbürünün atası değil).' % (benim[:12], onun[:12]))
    return 1


if __name__ == '__main__':
    sys.exit(main())
