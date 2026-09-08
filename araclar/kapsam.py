#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
kapsam.py -- Türkçe sarmalayıcıların Scala standart kütüphanesini ne kadar
kapsadığını ölçer.

Nasıl çalışıyor
  1. araclar/Kapsam.scala'yı yamalı derleyiciyle geçici bir dizine derleyip
     koşar; her koleksiyon türünün GENEL yöntem adlarını alır (yansıma).
  2. Sarmalayıcı dosyalarındaki her `implicit class`/`case class` gövdesini
     tarar ve alıcı üstünde çağrılan İngilizce yöntemleri toplar
     (`def bul(...) = d.find(...)` -> "find" sarılmış sayılır).
  3. İkisini karşılaştırıp tür başına kapsam yüzdesi ve eksik listesi verir.

Kullanım
  araclar/kapsam.py                      # bu depoyu ölç
  araclar/kapsam.py --ayrinti            # eksik yöntemleri de yaz
  araclar/kapsam.py --tsv kapsam.tsv     # sonucu TSV olarak yaz
  araclar/kapsam.py --kaynak <dizin>     # sarmalayıcı dizinini elle ver
  araclar/kapsam.py --scala-lib <dizin>  # scala jar'larının dizini

Notlar
  - Eksiklerin bir kısmı BİLEREK sarılmıyor: teknik/iç yöntemler (aggregate,
    applyOrElse, strictOptimized*, stepper...) ve eşlik nesnesine ait olanlar
    (fill, tabulate, range, iterate, unfold). Bunlar GURULTU'da eleniyor.
  - Yüzde bir hedef değil, bir pusula: 100% olması gerekmiyor.
"""
import argparse
import os
import re
import subprocess
import sys
import tempfile

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)

# Sarılması anlamsız ya da eşlik nesnesine ait yöntemler
GURULTU = re.compile(r'''^(strictOptimized|filterImpl|mapResult|sizeHint|ensureSize|requireBounds
    |klone|ioob|prefix1|vectorSlice|size0|DefaultInitialSize|applyPreferredMaxLength|endIndex
    |startIndex|initIterator|slice0|appendedAll0|prependedAll0|fixUp|fixDown|ord$|orderedCompanion
    |mapFromIterable|keyStepper|valueStepper|charStepper|codePointStepper|copySliceToArray
    |copyToArray|aggregate$|search$|segmentLength|prefixLength|sameElements$|reverseIterator
    |toIndexedSeq|trimToSize|clearAndShrink|to$|lift$|orElse$|applyOrElse$|isDefinedAt$|from$
    |iterate$|fill$|range$|tabulate$|unfold$|size$|ofArray|chars$|codePoints$|option2Iterable
    # koleksiyon altyapısı: kullanıcıya değil, kütüphaneye ait
    |iterator$|iterator[A-Z]|view$|seq$|repr$|coll$|companion$|stringPrefix|className
    |collectionClassName|newBuilder|newSpecificBuilder|fromSpecific|iterableFactory
    |sortedIterableFactory|mapFactory|evidenceIterableFactory|knownSize|sizeCompare
    |lengthCompare|sizeIs|lengthIs|hasDefiniteSize|isTraversableAgain|occCounts|stepper
    |toIterable|toTraversable|toIterator|toStream|toBuffer|copyToBuffer|addString|withFilter
    |lazyZip|unzip3|zipped|reversed|elementWise|runWith|compose|andThen|tapEach|toDeferrer
    |canEqual|product[A-Z]|productArity|writeReplace|readResolve|underlying|wrapped|value$
    |array$|unsafeArray|elemTag|tag$|par$|parUnsafe|sequential$|mutable$|immutable$
    |apply$|unapply$|unapplySeq$|empty$|concat$|clone$|result$)''',
    re.X)

# (Türkçe tür adı, dosya, sınıf adı) -- depoya göre iki takım
MASAUSTU = ('src/main/scala/net/kogics/kojo/lite/i18n/tr', [
    ('Diz', 'dizi.scala', 'colSeqYöntemleri'),
    ('Dizi', 'dizi.scala', 'SeqYöntemleri'),
    ('SıralıDizi', 'dizi.scala', 'IndexedSeqYöntemleri'),
    ('Dizin', 'dizin.scala', 'ListYöntemleri'),
    ('Yöney', 'yoney.scala', 'YöneyYöntemleri'),
    ('Dizik', 'dizik.scala', 'ArrayMethods'),
    ('EsnekDizik', 'dizik.scala', 'ArrayBufferMethods'),
    ('Eşlek', 'eslem.scala', 'EşlekYöntemleri'),
    ('Eşlem', 'eslem.scala', 'Eşlem'),
    ('Küme', 'kume.scala', 'SetYöntemleri'),
    ('Kuyruk', 'kuyruk.scala', 'mutQueueMethods'),
    ('ÖncelikSırası', 'kuyruk.scala', 'mutPriQueMethods'),
    ('Aralık', 'aralik.scala', 'RangeYöntemleri'),
    ('Yazı', 'yazi.scala', 'YazıYöntemleri'),
    ('EsnekYazı', 'yazi.scala', 'EsnekYazıYöntemleri'),
    ('MiskinDizin', 'miskindizin.scala', 'LazyListYöntemleri'),
    ('Belki', 'belki.scala', 'BelkiYöntemleri'),
])
IKOJO = ('src/main/scala/kojo/tr', [
    ('Diz', 'dizi.scala', 'DizMetotları'),
    ('Dizi', 'dizi.scala', 'DiziMetotları'),
    ('Dizin', 'dizin.scala', 'DizinMetotları'),
    ('Yöney', 'yoney.scala', 'YöneyMetotları'),
    ('Dizik', 'dizik.scala', 'DizikMetotları'),
    ('Eşlek', 'eslem.scala', 'EşlekMetotları'),
    ('Eşlem', 'eslem.scala', 'Eşlem'),
    ('Küme', 'kume.scala', 'KümeMetotları'),
    ('Kuyruk', 'kuyruk.scala', 'KuyrukMetotları'),
    ('ÖncelikSırası', 'kuyruk.scala', 'ÖncelikSırasıMetotları'),
    ('Yığın', 'kuyruk.scala', 'YığınMetotları'),
    ('Yazı', 'yazi.scala', 'YazıMetotları'),
    ('EsnekYazı', 'yazi.scala', 'EsnekYazıMetotları'),
    ('MiskinDizin', 'miskindizin.scala', 'MiskinDizinMetotları'),
    ('Belki', 'belki.scala', 'BelkiMetotları'),
])


def scala_jar_dizini(elle):
    """scala-library/compiler/reflect jar'larının bulunduğu dizin."""
    if elle:
        return elle
    adaylar = [
        os.path.join(KOK, 'scala-tr/build/pack/lib'),            # masaüstü kojo
        os.path.join(KOK, '../kojo/scala-tr/build/pack/lib'),    # yan yana klon
        os.path.expanduser('~/src/kojo/git/master/scala-tr/build/pack/lib'),
    ]
    for d in adaylar:
        if os.path.isfile(os.path.join(d, 'scala-compiler.jar')):
            return d
    sys.exit('scala jar dizini bulunamadı; --scala-lib ile verin\n  denenenler: '
             + '\n  '.join(adaylar))


def api_cikar(jar_dizini):
    """Kapsam.scala'yı derleyip koşar; {tür: {yöntem, ...}} verir."""
    jars = [os.path.join(jar_dizini, a) for a in
            ('scala-compiler.jar', 'scala-library.jar', 'scala-reflect.jar')]
    with tempfile.TemporaryDirectory() as tmp:
        derle = ['java', '-Dfile.encoding=UTF-8', '-cp', os.pathsep.join(jars),
                 'scala.tools.nsc.Main', '-usejavacp', '-d', tmp,
                 os.path.join(BURASI, 'Kapsam.scala')]
        s = subprocess.run(derle, capture_output=True, text=True)
        if s.returncode != 0:
            sys.exit('Kapsam.scala derlenemedi:\n' + s.stdout + s.stderr)
        kos = ['java', '-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8',
               '-cp', os.pathsep.join([jars[1], tmp]), 'Kapsam']
        s = subprocess.run(kos, capture_output=True, text=True, encoding='utf-8')
        if s.returncode != 0:
            sys.exit('Kapsam koşturulamadı:\n' + s.stdout + s.stderr)
    api = {}
    for satır in s.stdout.splitlines():
        if '\t' in satır:
            ad, yöntemler = satır.split('\t', 1)
            api[ad] = set(yöntemler.split())
    return api


def gövde(kaynak, sinif):
    """Dosyadaki implicit/case class gövdesini (alıcı değişken, gövde) verir."""
    s = open(kaynak, encoding='utf-8').read()
    m = re.search(r'(?:implicit\s+class|case\s+class)\s+' + re.escape(sinif) +
                  r'\b[^(]*\(\s*(?:protected\s+val\s+|val\s+)?(\w+)\s*:', s)
    if not m:
        return None, None
    açılış = s.index('{', m.end())
    derinlik = 0
    for i in range(açılış, len(s)):
        if s[i] == '{':
            derinlik += 1
        elif s[i] == '}':
            derinlik -= 1
            if derinlik == 0:
                return m.group(1), s[açılış:i]
    return None, None


def sarılanlar(gövde_metni, alıcı):
    """Gövdede alıcı üstünde çağrılan İngilizce yöntem adları."""
    return set(re.findall(r'\b' + re.escape(alıcı) + r'\.(\w+)', gövde_metni))


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--kaynak', help='sarmalayıcı dizini (varsayılan: depodan bulunur)')
    p.add_argument('--scala-lib', help='scala jar dizini')
    p.add_argument('--ayrinti', action='store_true', help='eksik yöntemleri de yaz')
    p.add_argument('--tsv', help='sonucu bu dosyaya TSV olarak yaz')
    a = p.parse_args()

    if a.kaynak:
        kaynak_dizin, harita = a.kaynak, (MASAUSTU[1] if 'i18n' in a.kaynak else IKOJO[1])
    else:
        for dizin, harita in (MASAUSTU, IKOJO):
            if os.path.isdir(os.path.join(KOK, dizin)):
                kaynak_dizin = os.path.join(KOK, dizin)
                break
        else:
            sys.exit('sarmalayıcı dizini bulunamadı; --kaynak ile verin')
    print('kaynak:', kaynak_dizin)

    api = api_cikar(scala_jar_dizini(a.scala_lib))
    satırlar = []
    for tür, dosya, sinif in harita:
        yol = os.path.join(kaynak_dizin, dosya)
        if not os.path.isfile(yol):
            continue
        alıcı, g = gövde(yol, sinif)
        if alıcı is None:
            print(f'  uyarı: {dosya} içinde {sinif} bulunamadı', file=sys.stderr)
            continue
        tam = {y for y in api.get(tür, set()) if not GURULTU.match(y)}
        sarılı = tam & sarılanlar(g, alıcı)
        satırlar.append((tür, sinif, len(tam), len(sarılı), sorted(tam - sarılı)))

    print(f"\n{'tür':16s} {'sınıf':24s} {'API':>4s} {'sarılı':>6s} {'%':>4s} {'eksik':>5s}")
    for tür, sinif, n, k, eksik in satırlar:
        print(f'{tür:16s} {sinif:24s} {n:4d} {k:6d} {100 * k // max(n, 1):3d}% {len(eksik):5d}')
    toplam_api = sum(r[2] for r in satırlar)
    toplam_sarılı = sum(r[3] for r in satırlar)
    print(f"\nTOPLAM: {toplam_sarılı}/{toplam_api} "
          f"({100 * toplam_sarılı // max(toplam_api, 1)}%), eksik {toplam_api - toplam_sarılı}")

    if a.ayrinti:
        for tür, _, _, _, eksik in satırlar:
            if eksik:
                print(f'\n--- {tür}: {len(eksik)} eksik\n  ' + ' '.join(eksik))
    if a.tsv:
        with open(a.tsv, 'w', encoding='utf-8') as f:
            f.write('tür\tsınıf\tapi\tsarılı\tyüzde\teksik\n')
            for tür, sinif, n, k, eksik in satırlar:
                f.write(f'{tür}\t{sinif}\t{n}\t{k}\t{100 * k // max(n, 1)}\t{" ".join(eksik)}\n')
        print('\nyazıldı:', a.tsv)


if __name__ == '__main__':
    main()
