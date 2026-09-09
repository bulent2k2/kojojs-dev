#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ornek-dizini.py -- komut/yöntem -> onu kullanan ÇALIŞAN örnek betik dizini.

Nereden geliyor: kojojs-editor'daki yardım sayfalarında (kojoOgren,
yardimKomutlar, yardimSkala, benzetim) her örneğin yanında bir "çalıştır"
bağlantısı var: /?zrc=<url-safe base64 of gzip>. İçinde ScalaFiddle
sarmalayıcısı, $FiddleStart ile $FiddleEnd arasında da kullanıcı kodu var.

Bu betik o bağlantıları ÇÖZÜP hangi komutun hangi örnekte geçtiğini çıkarıyor
ve sözlüğün kullandığı ornekler.json'u yazıyor. zrc dizgeleri OLDUĞU GİBİ
taşınıyor -- yeniden sıkıştırma yok, dolayısıyla bozuk bağlantı üretme riski
de yok.

Kullanım:
  araclar/ornek-dizini.py <kojojs-editor dizini> [çıktı.json]
"""
import base64
import gzip
import io
import json
import os
import re
import subprocess
import sys

SAYFALAR = {
    'kojoOgren.scala.html': 'Kojo ile Öğren',
    'yardimKomutlar.scala.html': 'Komutlar',
    'yardimSkala.scala.html': 'Skala',
    'benzetim.scala.html': 'Benzetim',
}
EN_UZUN_SATIR = 12   # bundan uzun betik o adı GÖSTERMİYOR, içinde geçiyor sadece
EN_COK_ORNEK = 3
SOZCUK = re.compile('[A-Za-zÇĞİIÖŞÜçğıöşü_][A-Za-z0-9ÇĞİIÖŞÜçğıöşü_]*')


def kullaniciKodu(zrc):
    ham = base64.b64decode(zrc.replace('-', '+').replace('_', '/') + '=' * (-len(zrc) % 4))
    tam = gzip.decompress(ham).decode('utf-8')
    i = tam.index('// $FiddleStart') + len('// $FiddleStart')
    return tam[i:tam.index('// $FiddleEnd')].strip()


def betikleriTopla(editorDizini):
    kok = os.path.join(editorDizini, 'server/src/main/twirl/views')
    betikler = {}
    for dosya, sayfaAdi in SAYFALAR.items():
        yol = os.path.join(kok, dosya)
        if not os.path.exists(yol):
            print('atlandı (yok): ' + yol, file=sys.stderr)
            continue
        metin = io.open(yol, encoding='utf-8').read()
        # href içindeki gerçek bağlantılar. Düz `zrc=` araması Twirl
        # YORUMLARINDAKİ "örnekler ?zrc= ile açılıyor" cümlesini de yakalıyordu.
        for zrc in re.findall(r'href="[^"]*?[?&]zrc=([^"&]+)"', metin):
            try:
                kod = kullaniciKodu(zrc)
            except Exception as e:            # bozuk bağlantı sessizce geçilmesin
                print('çözülemedi (%s): %s' % (dosya, e), file=sys.stderr)
                continue
            betikler.setdefault(kod, (sayfaAdi, zrc))
    return betikler


def sozlukAdlari(sozlukHtml):
    """koco-sozlugu.html içindeki CATS'ten Türkçe adları çıkarır (node ile)."""
    js = ('const fs=require("fs");const t=fs.readFileSync(%s,"utf8");'
          'const m=t.match(/const CATS = \\[[\\s\\S]*?\\n\\];/);'
          'const C=eval("("+m[0].replace("const CATS = ","").replace(/;\\s*$/,"")+")");'
          'const s=new Set(); C.forEach(c=>c.rows.forEach(r=>{const a=r[1];'
          's.add(a.includes(".")?a.split(".").pop():a);}));'
          'console.log(JSON.stringify([...s]));') % json.dumps(sozlukHtml)
    cikti = subprocess.run(['node', '-e', js], capture_output=True, text=True, check=True)
    return json.loads(cikti.stdout)


def dizinKur(betikler, adlar):
    liste = sorted(((kod, sayfa, zrc, set(SOZCUK.findall(kod)), kod.count('\n') + 1)
                    for kod, (sayfa, zrc) in betikler.items()),
                   key=lambda x: (x[4], len(x[0])))      # önce kısa: en odaklı gösteri
    havuz, havuzDizin, dizin = [], {}, {}
    for ad in adlar:
        secilen = []
        for kod, sayfa, zrc, kelimeler, satir in liste:
            if satir > EN_UZUN_SATIR:
                break                                    # sıralı, gerisi daha uzun
            if ad not in kelimeler:
                continue
            # etiket: adın GEÇTİĞİ satır -- ilk satır çoğu kez "sil" gibi anlamsız
            etiket = next((l.strip() for l in kod.split('\n') if ad in SOZCUK.findall(l)),
                          kod.split('\n')[0])
            if zrc not in havuzDizin:
                havuzDizin[zrc] = len(havuz)
                havuz.append({'z': zrc, 's': sayfa, 'n': satir})
            secilen.append([havuzDizin[zrc], etiket[:52]])
            if len(secilen) == EN_COK_ORNEK:
                break
        if secilen:
            dizin[ad] = secilen
    return {'havuz': havuz, 'dizin': dizin}


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    editor = sys.argv[1]
    varsayilan = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'sozluk', 'ornekler.json')
    hedef = sys.argv[2] if len(sys.argv) > 2 else varsayilan
    sozluk = os.path.join(os.path.dirname(os.path.abspath(hedef)), 'koco-sozlugu.html')

    betikler = betikleriTopla(editor)
    adlar = sozlukAdlari(sozluk)
    veri = dizinKur(betikler, adlar)
    metin = json.dumps(veri, ensure_ascii=False, separators=(',', ':'))
    io.open(hedef, 'w', encoding='utf-8').write(metin)

    baglanti = sum(len(v) for v in veri['dizin'].values())
    print('%s yazıldı' % os.path.normpath(hedef))
    print('  çözülen betik  : %d' % len(betikler))
    print('  örneği olan ad : %d / %d' % (len(veri['dizin']), len(adlar)))
    print('  havuz/bağlantı : %d / %d   boyut: %d KB' % (len(veri['havuz']), baglanti, len(metin) // 1024))


if __name__ == '__main__':
    main()
