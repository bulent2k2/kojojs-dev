#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""kojojs-editor'deki ÜRETİLMİŞ Yardım sayfaları üreteçle aynı mı.

  python3 araclar/editor-uretilmis-denetle.py --editor <kojojs-editor kökü> [--kojo <masaüstü kojo klonu>]

NEDEN: yardimSkala, yardimKomutlar ve yardimOrnekler (kojojs-editor/server/src/main/
twirl/views) bu depodaki üreteçlerin çıktısı (kilavuz/uret.py --twirl, kilavuz/
ornekler.py --twirl) ve EDİTÖRE ELLE KOPYALANIYOR. Üreteç değişince ya da
markdown/örnek eklenince kopya yenilenmezse hiçbir şey kırmızı yanmıyor: sayfa açılıyor,
yalnız eski içerik gösteriliyor. Bu depoda üretilmiş dosyaları denetleyen adımlar yalnız
KENDİ çıktısına bakıyor (uretecler.yml "kılavuz sayfaları yeniden üret"), editördeki
kopyaya hiçbiri bakmıyordu.

Ekim 2026'da tam bu görüldü (kojojs-editor#68'in hazırlığında): üç sayfa üreteç
çıktısından geride kalmıştı -- "ikojo" yerine "iKojo" yazımı (Skala 15, Komutlar 18
satır), dört yeni örnek (14-17) ve bir cümle. Hiçbir denetim görmemişti.

NE YAPIYOR: bu deponun İZLENEN dosyalarını (çalışma ağacındaki hâliyle) geçici bir dizine
kopyalar ve üreteçleri ORADA koşturur: editör klonuna ve bu deponun ağacına dokunmaz, ve
yanında bir ../kojo klonu olması sonucu etkilemez (uret.py klon görünce masaüstü ad
listesini ondan hesaplıyor, CI'da klon yok ve commit'li önbelleği kullanıyor; yerelde
klonla CI'dan farklı çıktı alınırdı -- ölçüldü: yerel klon masaustu-adlar.txt'yi 124
satır değiştiriyordu). Üç sayfayı bayt bayt karşılaştırır; fark varsa farkı ve düzeltme
komutunu yazıp 1 ile çıkar.

KLONSUZ İKİ SAYFA, KLONLU ÜÇÜNCÜ: yardimSkala ve yardimKomutlar klonsuz üretiliyor, her
koşuda denetlenir. yardimOrnekler (kilavuz/ornekler.py) masaüstü kojo klonunu İSTİYOR
(örnek başlıkları Bundle_tr.properties'ten); klon yalnız --kojo verilince denetlenir, yoksa
"denetlenmedi" yazılır (sessizce atlanmaz). CI'da klon yalnız push'ta çekiliyor: PR'larda
yardimOrnekler denetlenmez.

BEDELİ (gezinti-denetle.py ile aynı): iş ikinci bir depoya bağlı ve onun varsayılan dalının
BAŞINI alıyor. Bu depoda üreteç ya da kaynak değişince, editördeki kopya yenilenene kadar bu
adım kırmızı yanar -- istenen budur; sıra: önce bu depodaki değişiklik, ardından editöre
yeniden üretilmiş dosyalarla bir PR, ardından bu adım yeşile döner.
"""
import argparse
import difflib
import os
import shutil
import subprocess
import sys
import tempfile

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
KLONSUZ = ['yardimSkala.scala.html', 'yardimKomutlar.scala.html']
KLONLU = ['yardimOrnekler.scala.html']


def temiz_kopya(hedef):
    """İzlenen dosyalar, çalışma ağacındaki hâliyle (commit'lenmemiş düzenlemeler dahil)."""
    r = subprocess.run(['git', '-C', KOK, 'ls-files', '-z'], capture_output=True)
    if r.returncode != 0:
        print('hata: git ls-files koşmadı (bu bir git deposu mu?)', file=sys.stderr)
        return False
    for g in r.stdout.decode('utf-8').split('\0'):
        if not g:
            continue
        kaynak = os.path.join(KOK, g)
        if not os.path.exists(kaynak):  # silinmiş ama henüz commit'lenmemiş
            continue
        yol = os.path.join(hedef, g)
        os.makedirs(os.path.dirname(yol), exist_ok=True)
        shutil.copy2(kaynak, yol)
    return True


def uret(kopya, hedef, kojo):
    """Üreteçleri kopyada koşturur. HOME kopyanın içinde: ~/kojo, ~/src/kojo aranmasın."""
    env = dict(os.environ, HOME=kopya)
    isler = [[sys.executable, 'kilavuz/uret.py', '--twirl', hedef]]
    if kojo:
        isler.append([sys.executable, 'kilavuz/ornekler.py', '--kojo', os.path.abspath(kojo), '--twirl', hedef])
    for komut in isler:
        r = subprocess.run(komut, cwd=kopya, env=env, capture_output=True, text=True, encoding='utf-8')
        if r.returncode != 0:
            print('hata: %s koşmadı (çıkış %d):\n%s' % (komut[1], r.returncode, (r.stderr or r.stdout)[-1500:]),
                  file=sys.stderr)
            return False
    return True


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--editor', required=True, help='kojojs-editor klonu')
    ap.add_argument('--kojo', help='masaüstü kojo klonu (yardimOrnekler için; yoksa o sayfa denetlenmez)')
    a = ap.parse_args()
    views = os.path.join(a.editor, 'server', 'src', 'main', 'twirl', 'views')
    if not os.path.isdir(views):
        print('hata: kojojs-editor bulunamadı (%s). Klonsuz yeşil demek "bakmadım" demek (#151).' % views,
              file=sys.stderr)
        return 2
    sayfalar = KLONSUZ + (KLONLU if a.kojo else [])
    if not a.kojo:
        print('uyarı: %s denetlenmedi (masaüstü kojo klonu yok; --kojo ile ver)' % ', '.join(KLONLU))
    kotu = []
    with tempfile.TemporaryDirectory() as tmp:
        kopya = os.path.join(tmp, 'kojojs-dev')   # ../kojo bu dizinin yanında YOK
        cikti = os.path.join(tmp, 'cikti')
        os.makedirs(kopya)
        os.makedirs(cikti)
        if not temiz_kopya(kopya) or not uret(kopya, cikti, a.kojo):
            return 2
        for ad in sayfalar:
            yeni = os.path.join(cikti, ad)
            eski = os.path.join(views, ad)
            if not os.path.exists(yeni):
                print('hata: üreteç %s üretmedi' % ad, file=sys.stderr)
                return 2
            if not os.path.exists(eski):
                kotu.append((ad, None, None))
                continue
            e = open(eski, encoding='utf-8').read()
            y = open(yeni, encoding='utf-8').read()
            if e != y:
                kotu.append((ad, e, y))
    if not kotu:
        print('aynı: editördeki %d üretilmiş sayfa üreteç çıktısıyla bayt bayt aynı' % len(sayfalar))
        return 0
    print('hata: editördeki üretilmiş sayfaların %d tanesi üreteç çıktısından FARKLI:' % len(kotu), file=sys.stderr)
    for ad, e, y in kotu:
        if e is None:
            print('  %s: editörde yok' % ad, file=sys.stderr)
            continue
        fark = [l for l in difflib.unified_diff(e.split('\n'), y.split('\n'), lineterm='', n=0)
                if l[:1] in '+-' and l[:3] not in ('+++', '---')]
        print('  %s: %d satır farklı (editör -, üreteç +)' % (ad, len(fark) // 2 if fark else 0), file=sys.stderr)
        for l in fark[:4]:
            print('      ' + l[:140], file=sys.stderr)
    print('\nDüzeltmek için (bu depoda):\n'
          '  python3 kilavuz/uret.py --twirl <kojojs-editor>/server/src/main/twirl/views\n'
          '  python3 kilavuz/ornekler.py --kojo <kojo klonu> --twirl <kojojs-editor>/server/src/main/twirl/views\n'
          've çıkan farkı kojojs-editor\'de commit\'leyin.', file=sys.stderr)
    return 1


if __name__ == '__main__':
    sys.exit(main())
