#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-kapsam-denetle.py -- dict.scala'daki her terim sözlük sayfasında var mı.

NEDEN: 2026-09'da bildirildi -- sözlükte `to` (|-|) vardı ama `until` (|-)
yoktu. İkisi de dict.scala'da duruyordu; eksiklik yalnız sözlük sayfasındaydı
ve elle fark edildi. Üstelik sinsiydi: "1 until 4" DEYİMİ sözlükte olduğu için
arama "until" yazınca bir sonuç veriyor, işlecin eksikliği göze çarpmıyordu.

KURAL: bir dict.scala girişi, İKİ YANINDAN BİRİ sözlük satırlarının herhangi
bir sütununda (ya da bir "alt: ..." notunda) geçiyorsa kapsanmış sayılır.

Neden iki yan birden:
  - EN->TR haritalarında Türkçe değer çoğu zaman bir CÜMLEdir, ad değil
    ("until" -> "|- anlamı: ilkSayıdan..."), yani yalnız Türkçe yanı aramak
    yanlış alarm verir.
  - TR->EN haritalarında (type2en, method2en) anahtar Türkçedir; yalnız
    İngilizce sütunu aramak bu haritaların TAMAMINI eksik gösterir.
  - Aynı kavram sözlükte başka bir kanonik adla durabiliyor: dict.scala
    "union" der, sözlük satırı ["concat","bileşim"]; "toUpper" der, satır
    ["toUpperCase","büyükHarfe"]. Türkçe yan üzerinden eşleşiyorlar.

Harf katlama Türkçe i/İ tuzağına düşmeyecek biçimde elle yapılıyor
(str.lower() "İ" -> "i̇" verip eşleşmeyi bozuyor).

Bulanık (önek/sonek) eşleme BİLEREK yok: bir CI gözcüsünde bulanıklık,
öngörülemeyen yeşil demektir. Kapsanmayanlar aşağıda TEK TEK, gerekçesiyle
çivileniyor. Yeni bir boşluk açılırsa gözcü kırmızı yanar.

Kullanım:
  araclar/sozluk-kapsam-denetle.py [kojo-dizini]     # varsayılan ../kojo
"""
import io
import json
import os
import re
import sys

# Bugün kapsanmayanlar. Hepsi ya çevirmen dağarcığı (sözlük satırı değil)
# ya da bilerek yalnız Türkçede var. Bir boşluk KAPANIRSA da gözcü uyarır --
# o zaman bu listeden silin.
ÇİVİLİ = {
    'hesap': 'çevirmen dağarcığı: ölçüm/hesap ayrımı, sözlük satırı değil',
    'cebir': 'çevirmen dağarcığı: ölçülüm/cebir ayrımı, sözlük satırı değil',
    'programlama': 'çevirmen dağarcığı: "yazılım" karşılığı, API adı değil',
    'kelime': 'çevirmen dağarcığı: "sözcük" karşılığı, API adı değil',
    'align': 'çevirmen dağarcığı; API tarafı sözlükte "alignment" olarak var',
    'strip': 'çevirmen dağarcığı; API tarafı "stripPrefix"/"stripMargin" olarak var',
    'context-menu': 'arayüz terimi, API adı değil',
    'BaskınYazıyaYöntemiyle': 'dict.scala "Only in Turkish" diyor -- İngilizce karşılığı yok',
    'Eşsizlik': 'dict.scala "Only in Turkish" diyor -- İngilizce karşılığı yok',
}

ÇEVRİM = str.maketrans('İIıiĞğÜüŞşÖöÇç', 'iiiigguussoocc')


def katla(s):
    return s.translate(ÇEVRİM).lower()


def dictGirişleri(yol):
    """dict.scala'daki bütün "a" -> "b" ikilileri, harita adıyla."""
    girişler, ad = [], None
    for s in io.open(yol, encoding='utf-8').read().split('\n'):
        m = re.match(r'\s*val (\w+) = Map\(', s)
        if m:
            ad = m.group(1)
            continue
        if ad is None:
            continue
        if re.match(r'^\s*\)\s*$', s):
            ad = None
            continue
        p = re.match(r'\s*"([^"]*)"\s*->\s*"([^"]*)"', s)
        if p and (p.group(1).strip() or p.group(2).strip()):
            girişler.append((ad, p.group(1), p.group(2)))
    if not girişler:
        sys.exit('dict.scala içinde hiç giriş bulunamadı -- biçim değişmiş olabilir')
    return girişler


def sözlükHavuzu(yol):
    """Sözlük satırlarının iki sütunu + alt: notlarındaki adlar, katlanmış."""
    h = io.open(yol, encoding='utf-8').read()
    i = h.find('const CATS')
    if i < 0:
        sys.exit('koco-sozlugu.html içinde "const CATS" bulunamadı')
    blok = h[h.index('[', i):h.index('\n];', i)]
    havuz, n = set(), 0
    for ham in blok.split('\n'):
        d = ham.strip().rstrip(',')
        if not d.startswith('['):
            continue
        try:
            r = json.loads(d)
        except ValueError:
            continue
        if not (isinstance(r, list) and len(r) == 3):
            continue
        n += 1
        for k in (0, 1):
            havuz.add(katla(r[k]))
            havuz.add(katla(r[k].split('.')[-1]))
        for a in re.findall(r'alt:\s*([^,()]+)', r[2]):
            for q in a.split('/'):
                q = q.strip()
                if q:
                    havuz.add(katla(q))
                    havuz.add(katla(q.split('.')[-1]))
    if n == 0:
        sys.exit('sözlükte hiç satır bulunamadı -- biçim değişmiş olabilir')
    return havuz, n


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    kojo = sys.argv[1] if len(sys.argv) > 1 else os.path.join(kök, '..', 'kojo')
    dictYolu = os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/i18n/tr/dict.scala')
    if not os.path.exists(dictYolu):
        sys.exit('dict.scala bulunamadı: %s\n'
                 'kojo deposunu yanına klonlayın ya da yolunu argüman verin.' % dictYolu)

    havuz, satır = sözlükHavuzu(os.path.join(kök, 'sozluk', 'koco-sozlugu.html'))
    girişler = dictGirişleri(dictYolu)

    def geçiyor(t):
        t = t.strip()
        return bool(t) and (katla(t) in havuz or katla(t.split('.')[-1]) in havuz)

    açık, kapanmış = [], []
    for harita, a, b in girişler:
        if geçiyor(a) or geçiyor(b):
            if a in ÇİVİLİ:
                kapanmış.append(a)
        elif a not in ÇİVİLİ:
            açık.append((harita, a, b))

    if açık:
        print('HATA: dict.scala\'da olup sözlükte karşılığı bulunmayan terim(ler):', file=sys.stderr)
        for harita, a, b in açık:
            print('  %-20s %-26s -> %s' % (harita, a, b[:52]), file=sys.stderr)
        print('\nSözlüğe satır ekleyin; gerçekten girmemesi gerekiyorsa\n'
              'araclar/sozluk-kapsam-denetle.py içindeki ÇİVİLİ listesine\n'
              'gerekçesiyle yazın.', file=sys.stderr)
        return 1

    if kapanmış:
        print('HATA: şu terimler artık sözlükte var, ÇİVİLİ listesinden silin:', file=sys.stderr)
        for a in kapanmış:
            print('  %s  (gerekçe: %s)' % (a, ÇİVİLİ[a]), file=sys.stderr)
        return 1

    print('aynı: dict.scala\'daki %d terimin %d\'i sözlükte (%d satır); %d tanesi '
          'gerekçesiyle çivili' % (len(girişler), len(girişler) - len(ÇİVİLİ), satır, len(ÇİVİLİ)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
