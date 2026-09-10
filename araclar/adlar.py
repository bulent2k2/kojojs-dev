#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
adlar.py -- masaüstü Koco ile ikojo'nun aynı yüzeydeki Türkçe adlarını
karşılaştırır.

NEDEN: bugün hiçbir şey ikisini karşılaştırmıyor. ucurum.py masaüstü
BETİKLERİNİ tarıyor, yani yalnız bir örneğin KULLANDIĞI adları görüyor;
hiçbir örneğin kullanmadığı bir eksik ona görünmez. Eylül 2026'da tam bu oldu:
Renkler'de 24 ad eksikti (bütün açık aile), aynadaki hiçbir betik onları
kullanmadığı için tarama temiz görünüyordu ve eksik ancak bir kullanıcı
yazılımcığı ikojo'da patlayınca ortaya çıktı (#58).

NEDEN ELLE EŞLEŞME LİSTESİ: otomatik eşleme denendi ve UYDURMA boşluk üretti,
ölçüldü:
  - iki depo aynı yüzeye farklı kapsayıcı adı veriyor (masaüstü
    `implicit class YazıYöntemleri`, ikojo `implicit class YazıMetotları`)
    -- ad üstünden eşleyince 111 adlık sahte bir boşluk çıkıyordu;
  - `object Matematik extends tr.MatematikYöntemleri` gibi GÖVDESİZ bildirimler
    var, süslü parantez arayan ayrıştırıcı sonraki bloğu yutuyor ve onun
    üyelerini yanlış kapsayıcıya yazıyordu (54 ve 132 adlık iki sahte boşluk).
Yani her satır BİLİNÇLİ bir iddia: "bu iki kapsayıcı aynı yüzey". Yeni bir
çift eklemek ucuz; uydurma sayı üretmemek daha değerli.

Kullanım:
  araclar/adlar.py                          # ../kojo klonunu bekler
  araclar/adlar.py --kojo ~/src/kojo
  araclar/adlar.py --tsv                    # anlık görüntüyü tazele (izlenen dosya)
  araclar/adlar.py --anlik-goruntu          # kojo klonu OLMADAN: ikojo anlık
                                            # görüntüdeki her adı taşıyor mu
"""
import argparse
import io
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
IKOJO = os.path.dirname(BURASI)
ANLIK = os.path.join(BURASI, 'masaustu-adlar.tsv')

# (kapsayıcı, masaüstü dosyası, ikojo dosyası, ne olduğu)
# Masaüstü yolları kojo klonunun köküne, ikojo yolları bu deponun köküne göreli.
EŞLEŞMELER = [
    ('Renkler', 'src/main/scala/net/kogics/kojo/lite/i18n/tr/renk.scala',
     'src/main/scala/kojo/tr/renk.scala', 'renk adları'),
    ('tuşlar', 'src/main/scala/net/kogics/kojo/lite/i18n/tr/klavye.scala',
     'src/main/scala/kojo/tr/klavye.scala', 'tuş adları'),
]

ÜYE = re.compile(r'\b(?:def|val|var|lazy\s+val|type)\s+(`[^`]+`|[^\s(\[:=,)]+)', re.U)
# Masaüstü renkleri DEMETLE tanımlıyor: `val (koyuMavi, koyuCamgöbeği, ...) = (...)`.
# Bunu görmezsek masaüstü tarafı olduğundan çok küçük çıkar (ölçüldü: 61 yerine 16).
DEMET = re.compile(r'\bval\s*\(([^)]*)\)\s*=', re.U | re.S)


def soy(s):
    """Yorumları ve dizgeleri at; geriye yalnız kod kalsın."""
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    s = re.sub(r'"""(?:.|\n)*?"""', '""', s)
    s = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\\n])'", "''", s)
    return s


def üyeler(yol, kapsayıcı):
    """Bir object/trait gövdesindeki üye adları."""
    try:
        with io.open(yol, encoding='utf-8') as d:
            s = soy(d.read())
    except OSError as e:
        sys.exit('okunamadı: %s (%s)' % (yol, e))
    m = re.search(r'\bobject\s+%s\b' % re.escape(kapsayıcı), s)
    if not m:
        sys.exit('%s içinde "object %s" bulunamadı' % (yol, kapsayıcı))
    i = s.find('{', m.end())
    # Gövdesiz bildirim (`object X extends Y`) sonraki bloğu yutar: araya
    # bir bildirim girmişse gövde yok demektir.
    if i < 0 or re.search(r'\b(?:object|class|trait|def|val)\b', s[m.end():i]):
        sys.exit('%s: "object %s" gövdesiz görünüyor; bu araç gövdeli nesne bekliyor'
                 % (yol, kapsayıcı))
    d, j = 0, i
    while j < len(s):
        if s[j] == '{':
            d += 1
        elif s[j] == '}':
            d -= 1
            if d == 0:
                break
        j += 1
    gövde = s[i:j]
    adlar = {u.group(1).strip('`') for u in ÜYE.finditer(gövde)}
    for m in DEMET.finditer(gövde):
        for ad in m.group(1).split(','):
            ad = ad.strip().strip('`')
            if ad and ad != '_':
                adlar.add(ad)
    # demet yakalanınca "val (" kalıbı ÜYE'ye de "(" gibi düşebiliyor: ayıkla
    return {a for a in adlar if re.match(r'^[^\W\d]\w*$', a, re.U)}


def anlıkGörüntüyüOku():
    """{kapsayıcı: {durum: {ad}}} -- durum 'var' ya da 'boşluk'."""
    if not os.path.exists(ANLIK):
        sys.exit('anlık görüntü yok: %s (önce --tsv ile üretin)' % ANLIK)
    out = {}
    with io.open(ANLIK, encoding='utf-8') as d:
        for satır in d:
            satır = satır.rstrip('\n')
            if not satır.strip() or satır.startswith('#'):
                continue
            k, ad, durum = satır.split('\t')
            out.setdefault(k, {}).setdefault(durum, set()).add(ad)
    return out


def anlıkGörüntüyeGöre():
    """kojo klonu OLMADAN denetim (CI burayı koşuyor).

    Yakaladığı: ikojo'nun elindeki bir adı KAYBETMESİ (gerileme).
    Yakalayamadığı: masaüstünün YENİ bir ad eklemesi -- o, anlık görüntüde de
    olmadığından buradan görünmez; onun için tam karşılaştırma (--kojo) gerek.
    Bu sınır teknik: koşucuda masaüstü klonu yok.
    """
    beklenen = anlıkGörüntüyüOku()
    kötü = False
    for kapsayıcı, _, ikYol, ne in EŞLEŞMELER:
        var = üyeler(os.path.join(IKOJO, ikYol), kapsayıcı)
        bek = beklenen.get(kapsayıcı, {})
        eksik = sorted(bek.get('var', set()) - var)
        boşluk = bek.get('boşluk', set())
        if eksik:
            kötü = True
            print('::error::%s (%s): ikojo\'da olması beklenen %d ad kaybolmuş'
                  % (kapsayıcı, ne, len(eksik)), file=sys.stderr)
            for ad in eksik:
                print('    %s' % ad, file=sys.stderr)
        else:
            ek = ', bilinen boşluk %d' % len(boşluk) if boşluk else ''
            print('%-10s %-14s beklenen %d adın hepsi var%s'
                  % (kapsayıcı, '(%s)' % ne, len(bek.get('var', set())), ek))
    if kötü:
        sys.exit('ikojo, masaüstünün anlık görüntüsünün gerisine düştü.\n'
                 'Ya kaybolan adları geri getirin, ya da (masaüstü onları BİLEREK\n'
                 'kaldırdıysa) araclar/adlar.py --tsv ile anlık görüntüyü tazeleyin.')


def karşılaştır(kojo):
    sonuç = []
    for kapsayıcı, msYol, ikYol, ne in EŞLEŞMELER:
        masa = üyeler(os.path.join(kojo, msYol), kapsayıcı)
        ik = üyeler(os.path.join(IKOJO, ikYol), kapsayıcı)
        sonuç.append((kapsayıcı, ne, masa, ik))
    return sonuç


def yazdır(sonuç):
    print('%-10s %-14s %8s %8s  %s' % ('kapsayıcı', '', 'masaüstü', 'ikojo', 'ikojo\'da eksik'))
    top = 0
    for kapsayıcı, ne, masa, ik in sonuç:
        eksik = sorted(masa - ik)
        top += len(eksik)
        print('%-10s %-14s %8d %8d  %d' % (kapsayıcı, '(%s)' % ne, len(masa), len(ik), len(eksik)))
        for i in range(0, len(eksik), 6):
            print('    ' + '  '.join('%-20s' % a for a in eksik[i:i + 6]))
        fazla = sorted(ik - masa)
        if fazla:
            print('    (ikojo\'da fazla: %s)' % ', '.join(fazla))
    print('\ntoplam eksik: %d' % top)
    return top


def tsvYaz(sonuç):
    """Anlık görüntü: masaüstündeki her ad, ikojo'da olup olmadığıyla.

    Üçüncü sütun BUGÜNKÜ gerçeği yazıyor: 'var' ya da 'boşluk'. CI yalnız
    'var' satırlarını zorunlu tutuyor, yani bugünkü boşluklar işi kırmızı
    yakmıyor ama İZLENEN bir dosyada, göz önünde duruyorlar.
    """
    with io.open(ANLIK, 'w', encoding='utf-8') as d:
        d.write('# Masaüstü Koco\'daki adların anlık görüntüsü. Üretim: araclar/adlar.py --tsv\n')
        d.write('#\n')
        d.write('# sütunlar: kapsayıcı, ad, durum ("var" = ikojo\'da da var, "boşluk" = yok)\n')
        d.write('# CI (adlar.py --anlik-goruntu) yalnız "var" satırlarını zorunlu tutar:\n')
        d.write('# ikojo elindeki bir adı kaybederse kırmızı yanar. Bugünkü boşlukları\n')
        d.write('# kapatmak ayrı bir karar; burada görünür kalsınlar diye yazılıyorlar.\n')
        for kapsayıcı, ne, masa, ik in sonuç:
            boşluk = len(masa - ik)
            d.write('#\n# %s (%s): %d ad, %d boşluk\n' % (kapsayıcı, ne, len(masa), boşluk))
            for ad in sorted(masa):
                d.write('%s\t%s\t%s\n' % (kapsayıcı, ad, 'var' if ad in ik else 'boşluk'))
    print('anlık görüntü yazıldı: %s' % ANLIK)


def main():
    p = argparse.ArgumentParser(description=__doc__.split('\n')[1])
    p.add_argument('--kojo', default=os.path.join(IKOJO, '..', 'kojo'),
                   help='masaüstü kojo klonu (varsayılan: ../kojo)')
    p.add_argument('--tsv', action='store_true', help='anlık görüntüyü tazele')
    p.add_argument('--anlik-goruntu', action='store_true',
                   help='kojo klonu olmadan anlık görüntüye göre denetle (CI)')
    a = p.parse_args()

    if a.anlik_goruntu:
        anlıkGörüntüyeGöre()
        return

    kojo = os.path.abspath(a.kojo)
    if not os.path.isdir(os.path.join(kojo, 'src/main/scala/net/kogics/kojo')):
        sys.exit('kojo klonu bulunamadı: %s  (--kojo ile göster)' % kojo)
    sonuç = karşılaştır(kojo)
    top = yazdır(sonuç)
    if a.tsv:
        tsvYaz(sonuç)
    sys.exit(1 if top and not a.tsv else 0)


if __name__ == '__main__':
    main()
