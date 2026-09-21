#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ucurum.py -- masaüstü Koco betikleri ile iKojo'nun Türkçe API yüzeyi arasındaki
uçurumu ölçer.

Her .kojo betiğini yorum ve dizgelerden arındırıp tanımlayıcılarına ayırır, her
adı üç kümeyle karşılaştırır:
  * masaüstü TR API'si  (kojo: lite/i18n/trInit.scala + lite/i18n/tr/*.scala)
  * iKojo TR API'si     (kojojs-dev: kojo/TurkishTurtle.scala + kojo/tr/*.scala)
  * iKojo İngilizce yüzeyi (kojojs-dev: kojo/*.scala, kojo/syntax, kojo/doodle)
ve platform engellerini (Swing arayüzü, #yükle, ses, öykü, dosya...) düzenli
ifadelerle işaretler. Sonuç: betik başına durum + eksik adların sıklık listesi.

Adın VARLIĞI yetmiyor, BİÇİMİ de tutmalı: masaüstünde bazı adlar hem Resim
YÖNTEMİ hem DÖNÜŞTÜRÜCÜ (`*` ile zincirlenip `->` ile uygulanan) olarak var.
İkojo'da yalnız yöntemi varsa ad taramada "var" görünür ama betik derlenmez.
Eylül 2026'da tam bu oldu: `döndürMerkezli` yöntem olarak vardı, unit-circle.kojo
ise onu dönüştürücü olarak kullanıyordu; tarama temiz diyordu. Bu yüzden tarayıcı
artık iKojo'nun `implicit class` GÖVDESİNDEKİ tanımlarını ayrı tutuyor (onlar
ancak `r.ad(...)` diye çağrılabilir) ve betiklerdeki dönüştürücü biçimli
kullanımlarla karşılaştırıyor -> yeni durum: "biçim".

Bu bir TANIMLAYICI TARAMASIDIR, derleme değil: imza farklarını (parametre
türü/sayısı) göremez. Gerçek derleme denetimi için ornekler/ornekleri-dogrula.sh.

Kullanım:
  araclar/ucurum.py                       # ../kojo klonu, ornekler/masaustu
  araclar/ucurum.py --kojo ~/src/kojo --tsv ornekler/masaustu/tarama.tsv
  araclar/ucurum.py --json rapor.json --en-sik 40
"""
import argparse
import collections
import glob
import json
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
IKOJO = os.path.dirname(BURASI)

# def/val/var/object/class/trait/type tanımlarından ad çıkarır (geri tırnaklılar dahil)
TANIM = re.compile(r'\b(?:def|val|var|object|class|trait|type|lazy\s+val)\s+(`[^`]+`|[^\s(\[:=,)]+)')
# betiklerdeki tanımlar Türkçe anahtar kelimelerle (scala-tr): tanım/dez/den/nesne/sınıf/...
TANIM_TR = re.compile(r'\b(?:def|val|var|object|class|trait|type|lazy\s+val|tanım|dez|den|nesne|sınıf|özellik|tür|miskin\s+dez)\s+(`[^`]+`|[^\s(\[:=,)]+)', re.U)
TANIMLAYICI = re.compile(r'[^\W\d]\w*', re.U)
# Betiğin kendi PARAMETRE adları: `tanım ejder(derinlik: Sayı, açı: Kesir)`.
# TANIM_TR yalnız def/val/var/... yakalıyor, parametreleri değil; masaüstü API'si
# büyüdükçe sıradan bir parametre adı (derinlik, kurallar) API'de de belirince
# betik sahte "eksik-ad" oluyordu. Ölçüldü (Eylül 2026, kojo cd1e2a9):
# dragon-curve ve l-systems tam böyle kırmızıya döndü, ikisi de yanlış.
PARAMETRE = re.compile(r'[(,]\s*(?:örtük\s+|implicit\s+|dez\s+|den\s+|val\s+|var\s+)?([^\W\d]\w*)\s*:', re.U)

# Yamalı scala-tr derleyicisinin tanıdığı Türkçe anahtar kelimeler
ANAHTAR = {
    'dez', 'den', 'tanım', 'eğer', 'yoksa', 'yineleDoğruKaldıkça', 'için', 'ver', 'eşle', 'durum',
    'sınıf', 'nesne', 'özellik', 'yayar', 'birlikte', 'yeni', 'bu', 'üst', 'baskın', 'soyut', 'son',
    'damgalı', 'örtük', 'miskin', 'gizli', 'koru', 'deste', 'getir', 'geriDön', 'dene', 'yakala',
    'sonunda', 'bildir', 'yap', 'tür', 'doğru', 'yanlış', 'yok',
}

# Tarayıcıda karşılığı olmayan / henüz taşınmamış özellik aileleri.
# Ad -> (düzenli ifade, açıklama)
PLATFORM = collections.OrderedDict([
    ('yükle',        (re.compile(r'#\s*(?:yükle|include)\b'),
                      'başka dosya içe alır; iKojo tek dosya derler')),
    ('arayüz',       (re.compile(r'\bay\.'),
                      'Swing arayüz nesneleri (ay.*)')),
    # Mp3 çalma (howler) ve notaÇal (Web Audio) iKojo'da var; engel yalnız MIDI
    # partisyon çalma (MusicScore/playMusic, jfugue)
    ('ses',          (re.compile(r'\b(?:MusicScore|playMusic|müzikÇal|Müzik\b|Nota\b|Ritim|Enstrüman)'),
                      'MIDI partisyon (MusicScore); tarayıcıda karşılığı yok')),
    ('öykü',         (re.compile(r'\b(?:hikaye|Story|öykü\w*|Öykü\w*|stPlayStory|Sayfa|Page)\b'),
                      'öykü anlatıcı (Story/Page)')),
    ('dosya',        (re.compile(r'\b(?:Dosya|dosya\w*|satıroku|sayıOku|kesirOku|readln|readInt|scala\.io|java\.io|Source\.from)\b'),
                      'dosya okuma / klavye girdisi')),
    ('imge',         (re.compile(r'\b(?:Staging|İmgeİşlemi|BufferedImage|Bellekteİmge|ImageOp)\b'),
                      'Java2D imge işleme')),
    ('canlandırma',  (re.compile(r'\b(?:Canlandırma|Geçiş|canlandırmaDizisi|Transition|animSeq)\b'),
                      'Transition/Canlandırma API')),
    ('tuvalçizim',   (re.compile(r'\b(?:Grafik2B|canlandırTuvalÇizimle|tuvalÇizim\w*)\b'),
                      'Graphics2D tuval çizimi')),
    ('çini',         (re.compile(r'\b(?:ÇiniDünyası|TileWorld|BirSayfaKostüm|SpriteSheet)\b'),
                      'Çini dünyası (TileWorld)')),
    ('geo',          (re.compile(r'\b(?:GeoYol|GeoNokta|geo\.)\b'),
                      'JTS geometri')),
    ('url',          (re.compile(r'\b(?:BKK|URL)\b'),
                      'ağ erişimi')),
])

# Çok genel Scala/Java adları -- eksik diye sayılmaz
GENEL = {'apply', 'toString', 'length', 'size', 'map', 'foreach', 'filter', 'to', 'until', 'by', 'scala', 'java',
         'x', 'y', 'z', 'a', 'b', 'n', 'i', 'j', 'k'}


YEREL_TANIM = re.compile(r'\b(?:lazy\s+val|val|var)\b')


def ifade_blogu(s, baş, girinti):
    """`=`den sonraki İFADE gövdesinin ilk dengeli `{` bloğunu bulur; yoksa -1.

    Neden gerekli: gövde `= {` ile başlamak zorunda değil --
    `def f = if (...) { ... }`, `def f = x match { ... }` biçimlerinde blok
    daha ileride. Onları kaçırınca içlerindeki yereller API sayılmaya devam
    ediyordu (#120 incelemesi; ölçüldü: 14 ad daha).

    NEREDE DURUR: ilk `{`e kadar, ama İFADE BİTİNCE durur -- girintisi def'in
    girintisinden büyük OLMAYAN dolu bir satır görülünce. O sınır olmadan
    ifade gövdeli bir def ilerideki bambaşka bir bloğu (bir sonraki def'in ya
    da object'in gövdesini) kendi gövdesi sanar ve GERÇEK API adlarını
    gizlerdi -- tehlikeli yön bu, sınır onun için var.
    """
    i, paren = baş, 0
    while i < len(s):
        c = s[i]
        if c in '([':
            paren += 1
        elif c in ')]':
            paren -= 1
        elif c == '{':
            # Parantez İÇİNDEKİ bloğu da alıyoruz: `getOrElseUpdate(k, { val x = ... })`
            # gibi blok-argümanların gövdesi de yerel kapsam. İfadenin dışına
            # taşma tehlikesini paren değil, aşağıdaki girinti sınırı kesiyor.
            return i
        elif c == '\n' and paren == 0:
            j = i + 1
            while j < len(s) and s[j] in ' \t':
                j += 1
            if j < len(s) and s[j] not in '\r\n' and (j - (i + 1)) <= girinti:
                return -1  # ifade bitti
        i += 1
    return -1


def def_govdeleri(s):
    """`def ... = { ... }` gövdelerinin (başlangıç, bitiş) aralıkları.

    NEDEN: `tanimlar` bir dosyadaki BÜTÜN val/var/def tanımlarını topluyordu,
    yerel olanları da. Yani bir yöntemin içindeki `val eski = ...` iKojo'nun
    "var olan adlar" kümesine giriyor ve bir betiğin gerçekten eksik olan adını
    SAKLIYORDU. Gerçekten oldu (#117 turu): Picture.fade'e `val eski` yazınca
    scala-tutorial.kojo'nun eksik listesinden `eski` düştü -- API'ye hiçbir şey
    eklenmediği halde.

    Gövdenin nerede başladığı KESİN olarak bulunuyor, tahminle değil: def
    adından sonra parantez/köşeli derinliği 0'da gelen ilk `=` ya da `{`
    hangisiyse o. `=` ise ardından gelen ilk karakter `{` değilse gövde tek
    ifadedir ve içinde yerel tanım olamaz -- atlanıyor. Bu sınır olmadan
    ifade gövdeli bir def, ilerideki bambaşka bir bloğu kendi gövdesi sanıp
    gerçek API adlarını gizlerdi (tehlikeli yön bu).
    """
    araliklar = []
    for m in re.finditer(r'\bdef\s+', s):
        i, paren = m.end(), 0
        gövde = -1
        satırBaşı = s.rfind('\n', 0, m.start()) + 1
        girinti = len(s[satırBaşı:m.start()]) - len(s[satırBaşı:m.start()].lstrip())
        while i < len(s):
            c = s[i]
            if c in '([':
                paren += 1
            elif c in ')]':
                paren -= 1
            elif paren == 0:
                if c == '{':
                    gövde = i
                    break
                if c == '=' and s[i + 1:i + 2] != '=':
                    gövde = ifade_blogu(s, i + 1, girinti)
                    break
                if c == '\n' and s[i + 1:i + 2] not in (' ', '\t'):
                    break  # bildirim (gövdesiz soyut def)
            i += 1
        if gövde < 0:
            continue
        j, derinlik = gövde, 0
        while j < len(s):
            if s[j] == '{':
                derinlik += 1
            elif s[j] == '}':
                derinlik -= 1
                if derinlik == 0:
                    break
            j += 1
        araliklar.append((gövde, j))
    return araliklar


def tanimlar(yollar, rx=TANIM):
    adlar = set()
    for y in yollar:
        try:
            with open(y, encoding='utf-8') as f:
                s = f.read()
        except OSError:
            continue
        s = soy(s)  # yorumdaki tanımlar ("// def çıktıyıSil ...") var sayılmasın
        govdeler = def_govdeleri(s)
        for m in rx.finditer(s):
            ad = m.group(1).strip('`')
            if not ad or ad == '_' or ad.startswith('$'):
                continue
            # Bir YÖNTEM GÖVDESİ içindeki val/var yereldir: betikten çağrılamaz,
            # yani iKojo'nun yüzeyi değil. def/object/class/trait/type'a
            # dokunmuyoruz -- onlar bu depoda yerel olmuyor ve fazladan eleme
            # gerçek API adlarını gizleme riskini taşır.
            if YEREL_TANIM.match(s, m.start()) and any(a <= m.start() <= b for a, b in govdeler):
                continue
            adlar.add(ad)
    return adlar


ORTUK_SINIF = re.compile(r'\bimplicit\s+(?:final\s+)?class\b')


def ortuk_sinif_govdeleri(s):
    """`implicit class ... { ... }` gövdelerinin (başlangıç, bitiş) aralıkları.

    Oradaki tanımlar ancak BİR ALICI ÜSTÜNDE çağrılabilir (`r.döndür(30)`),
    çıplak `döndür(30)` olarak değil. Ayrımın bütün derdi bu.
    """
    araliklar = []
    for m in ORTUK_SINIF.finditer(s):
        # Yapıcı parametrelerini ve `extends ... with ...` kısmını atla; gövdeyi
        # açan `{`, parantez derinliği 0'dayken gelen ilk `{`.
        i, paren = m.end(), 0
        while i < len(s):
            c = s[i]
            if c == '(':
                paren += 1
            elif c == ')':
                paren -= 1
            elif c == '{' and paren == 0:
                break
            i += 1
        # Gövde açan `{` bulunamadıysa atla. (Eskiden burada ayrıca "sonraki
        # implicit class'a taşma" koruması vardı; hiç tetiklenmiyordu -- newline
        # sonrasına sabitlenmişti, bu depoda ise bütün bildirimler trait içinde
        # girintili. Gövdesiz `implicit class` zaten geçerli Scala değil, ve
        # `{` bulunamayınca döngü dosya sonunda durup aynı yere geliyor.)
        if i >= len(s):
            continue
        j, derinlik = i, 0
        while j < len(s):
            if s[j] == '{':
                derinlik += 1
            elif s[j] == '}':
                derinlik -= 1
                if derinlik == 0:
                    break
            j += 1
        araliklar.append((i, j))
    return araliklar


def tanimlar_bicimli(yollar, rx=TANIM):
    """Tanımları ikiye ayırır: (serbest, yalnız_yöntem).

    serbest      -- çıplak çağrılabilir (trait/object gövdesi)
    yalnız_yöntem -- SADECE bir `implicit class` içinde tanımlı
    Bir ad iki yerde birden geçiyorsa serbest sayılır (döndür/büyüt/götür böyle).
    """
    serbest, uye = set(), set()
    for y in yollar:
        try:
            with open(y, encoding='utf-8') as f:
                kod = soy(f.read())
        except OSError:
            continue
        govdeler = ortuk_sinif_govdeleri(kod)
        for m in rx.finditer(kod):
            ad = m.group(1).strip('`')
            if not ad or ad == '_' or ad.startswith('$'):
                continue
            icinde = any(a <= m.start() <= b for a, b in govdeler)
            (uye if icinde else serbest).add(ad)
    return serbest, uye - serbest


ADLI_CAGRI = re.compile(r'([^\W\d]\w*)\s*\(', re.U)
# Çağrının solunda/sağında bakılan karakter sayısı. Aradığımız `*` ya da `->`
# bitişik ya da bir-iki boşluk ötede; 8 fazlasıyla yetiyor.
SOL_PENCERE = SAG_PENCERE = 8


def donusturucu_kullanimlari(kod):
    """Betikte DÖNÜŞTÜRÜCÜ biçiminde kullanılan adlar.

    İki imzadan biri yeter: çağrının solunda `*` var (zincirin içinde) ya da
    sağında `*` / `->` var (zincirleniyor veya bir resme uygulanıyor):
        götür(-30, -200) * döndürMerkezli(-90, 0, 0) -> Resim.yazı(...)
    YANLIŞ-POZİTİF: çarpma (`3 * sin(x)`) de eşleşir. Zararsız, çünkü rapora
    yalnız iKojo'da SADECE yöntem olarak tanımlı adlar giriyor.

    YANLIŞ-NEGATİF: dönüştürücü bir ADA bağlanırsa iki imza da yok --
    `dez d = döndürMerkezli(45, 0, 0)` satırında ne solda `*` var ne sağda `->`;
    tarama onu göremez. Bu tarayıcının BİLİNEN kör noktası (commit'in bütün
    derdi ne görüp ne göremediğini bilmek olduğu için burada yazılı).
    """
    bulunan = set()
    for m in ADLI_CAGRI.finditer(kod):
        i, derinlik = m.end() - 1, 0
        while i < len(kod):
            if kod[i] == '(':
                derinlik += 1
            elif kod[i] == ')':
                derinlik -= 1
                if derinlik == 0:
                    break
            i += 1
        else:
            continue
        # Pencereli bakış: `kod[:m.start()]` ve `kod[i+1:]` tam kopya çıkarıyordu,
        # yani çağrı başına O(dosya). Bakılan tek şey komşu birkaç karakter.
        sol = kod[max(0, m.start() - SOL_PENCERE):m.start()].rstrip()
        sag = kod[i + 1:i + 1 + SAG_PENCERE].lstrip()
        if sol.endswith('*') or sag.startswith('*') or sag.startswith('->'):
            bulunan.add(m.group(1))
    return bulunan


def soy(s):
    """Yorumları ve dizgeleri at; geriye yalnız kod kalsın."""
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    s = re.sub(r'"""(?:.|\n)*?"""', '""', s)
    s = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\\n])'", "''", s)
    return s


def betikler(kok):
    out = []
    for d, _, dosyalar in os.walk(kok):
        for f in sorted(dosyalar):
            if f.endswith('.kojo') or f.endswith('.kojo.installed'):
                out.append(os.path.join(d, f))
    return sorted(out)


def olc(kojo, ikojo, kok):
    if not os.path.isdir(os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/i18n')):
        sys.exit(f'kojo klonu bulunamadı: {kojo}  (--kojo ile göster)')
    i18n = os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/i18n')
    # dict/help/templates/translate/data* API değil (sözlük, yardım metni, şablon,
    # çıktı çevirisi); onlardaki adlar tarama gürültüsü yapar
    api_degil = re.compile(r'/(?:dict|help|templates|translate|data\w*)\.scala$')
    tr_dosyalar = [os.path.join(i18n, 'trInit.scala')] + [
        f for f in glob.glob(os.path.join(i18n, 'tr/*.scala')) if not api_degil.search(f)]
    masa_tr = tanimlar(tr_dosyalar)
    src = os.path.join(ikojo, 'src/main/scala/kojo')
    tr_kaynak = [os.path.join(src, 'TurkishTurtle.scala')] + glob.glob(os.path.join(src, 'tr/*.scala'))
    en_kaynak = (glob.glob(os.path.join(src, '*.scala'))
                 + glob.glob(os.path.join(src, 'syntax/*.scala'))
                 + glob.glob(os.path.join(src, 'doodle/*.scala')))
    ikojo_tr = tanimlar(tr_kaynak)
    ikojo_en = tanimlar(en_kaynak)
    # Biçim ayrımı: çıplak çağrılabilenler mi, yoksa yalnız `r.ad(...)` mı.
    tr_serbest, tr_yalnız_yöntem = tanimlar_bicimli(tr_kaynak)
    en_serbest, _ = tanimlar_bicimli(en_kaynak)
    serbest = tr_serbest | en_serbest
    yalnız_yöntem = tr_yalnız_yöntem - serbest

    dosyalar = collections.OrderedDict()
    eksik_dosya = collections.Counter()
    eksik_kez = collections.Counter()
    bicim_dosya = collections.Counter()
    engel_dosya = collections.Counter()
    for y in betikler(kok):
        ham = open(y, encoding='utf-8', errors='replace').read()
        kod = soy(ham)
        tokenler = set(TANIMLAYICI.findall(kod))
        kullanilan = tokenler & (masa_tr | ANAHTAR)
        # betiğin kendi tanımladığı adlar (ve parametreleri) eksik sayılmaz
        kendi = tanimlar([y], TANIM_TR) | set(PARAMETRE.findall(kod))
        eksik = sorted(t for t in kullanilan
                       if t not in ikojo_tr and t not in ikojo_en and t not in ANAHTAR
                       and t not in GENEL and t not in kendi)
        # Adı VAR ama biçimi tutmuyor: betik dönüştürücü olarak kullanıyor,
        # iKojo'da yalnız Resim yöntemi olarak tanımlı.
        bicim = sorted(donusturucu_kullanimlari(kod) & yalnız_yöntem & masa_tr - kendi)
        # #yükle/#include satırları yorum içindedir; ham metinde ara
        engeller = [ad for ad, (rx, _) in PLATFORM.items() if rx.search(ham if ad == 'yükle' else kod)]
        if engeller:
            durum = 'platform'
        elif eksik:
            durum = 'eksik-ad'
        elif bicim:
            durum = 'biçim'
        else:
            durum = 'çalışır'
        gorelli = os.path.relpath(y, kok)
        dosyalar[gorelli] = {
            'satır': ham.count('\n') + 1,
            'kullanılan': len(kullanilan),
            'eksik': eksik,
            'biçim': bicim,
            'engel': engeller,
            'durum': durum,
        }
        for t in eksik:
            eksik_dosya[t] += 1
            eksik_kez[t] += len(re.findall(r'\b' + re.escape(t) + r'\b', kod))
        for t in bicim:
            bicim_dosya[t] += 1
        for e in engeller:
            engel_dosya[e] += 1

    ozet = collections.Counter(v['durum'] for v in dosyalar.values())
    return {
        'kök': os.path.relpath(kok, ikojo),
        'masaüstü_tr_ad': len(masa_tr),
        'ikojo_tr_ad': len(ikojo_tr),
        'betik': len(dosyalar),
        'satır': sum(v['satır'] for v in dosyalar.values()),
        'özet': dict(ozet),
        'engeller': [(e, n, PLATFORM[e][1]) for e, n in engel_dosya.most_common()],
        'eksik': [(t, n, eksik_kez[t]) for t, n in eksik_dosya.most_common()],
        'biçim': bicim_dosya.most_common(),
        'dosyalar': dosyalar,
    }


def yazdir(r, en_sik):
    print(f"betik: {r['betik']}   satır: {r['satır']}   "
          f"masaüstü TR ad: {r['masaüstü_tr_ad']}   iKojo TR ad: {r['ikojo_tr_ad']}")
    o = r['özet']
    print(f"durum: çalışır {o.get('çalışır', 0)}   eksik-ad {o.get('eksik-ad', 0)}   "
          f"biçim {o.get('biçim', 0)}   platform {o.get('platform', 0)}")
    print("\n== dizin bazında (betik / çalışır / eksik-ad / biçim / platform)")
    dizin = collections.defaultdict(collections.Counter)
    for yol, v in r['dosyalar'].items():
        dizin[os.path.dirname(yol)][v['durum']] += 1
    for d in sorted(dizin):
        c = dizin[d]
        print(f"  {d:44s} {sum(c.values()):3d}  {c['çalışır']:3d}  {c['eksik-ad']:3d}  "
              f"{c['biçim']:3d}  {c['platform']:3d}")
    print("\n== platform engelleri (betik sayısı)")
    for e, n, aciklama in r['engeller']:
        print(f"  {e:14s} {n:3d}   {aciklama}")
    print(f"\n== en sık eksik adlar (ilk {en_sik}; betik sayısı / toplam kullanım)")
    for t, n, kez in r['eksik'][:en_sik]:
        print(f"  {t:40s} {n:3d}  {kez:4d}")
    if r['biçim']:
        print("\n== biçim uyuşmazlığı: betik DÖNÜŞTÜRÜCÜ kullanıyor, iKojo'da yalnız YÖNTEM var")
        print("   (çözüm: tr/resim.scala'ya `def ad(...): Dönüştürücü = kb....` ekleyin)")
        for t, n in r['biçim']:
            print(f"  {t:40s} {n:3d}")


def git_surumu(dizin):
    """`dizin`deki klonun kısa commit'i; git yoksa ya da klon değilse '?'."""
    try:
        import subprocess
        s = subprocess.run(['git', '-C', dizin, 'rev-parse', '--short', 'HEAD'],
                           capture_output=True, text=True, timeout=10)
        return s.stdout.strip() or '?'
    except Exception:
        return '?'


def tsv_yaz(r, yol, kojo):
    with open(yol, 'w', encoding='utf-8') as f:
        f.write("# ucurum.py tarama sonucu (tanımlayıcı taraması, derleme değil)\n")
        # HANGİ masaüstü sürümünden üretildiği. Bu satır olmadan dosya
        # atfedilemez hale geliyordu: `eksik` sütunundaki bir değişiklik iKojo'dan
        # mı yoksa yukarı akıştaki kojo'nun büyümesinden mi geldi, ayırt
        # edilemiyordu -- ve gerçekten karıştı (bkz. #107 incelemesi, 18 satırlık
        # fark). Üretilmiş dosyanın kaynağından ayrılması bu deponun aylardır
        # kovaladığı hata sınıfı; iz bırakmak tek satır.
        f.write(f"# masaüstü kojo: {git_surumu(kojo)}\n")
        f.write("betik\tdurum\tsatır\tengel\teksik\tbiçim\n")
        for b, v in r['dosyalar'].items():
            f.write(f"{b}\t{v['durum']}\t{v['satır']}\t{','.join(v['engel'])}\t"
                    f"{' '.join(v['eksik'])}\t{' '.join(v['biçim'])}\n")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--kojo', default=os.path.join(IKOJO, '..', 'kojo'), help='masaüstü kojo klonu (varsayılan: ../kojo)')
    p.add_argument('--ikojo', default=IKOJO, help='kojojs-dev kökü (varsayılan: bu repo)')
    p.add_argument('--betikler', default=os.path.join(IKOJO, 'ornekler', 'masaustu'), help='taranacak betik kökü')
    p.add_argument('--tsv', help='betik başına durumu bu TSV dosyasına yaz')
    p.add_argument('--json', help='tam raporu bu JSON dosyasına yaz')
    p.add_argument('--en-sik', type=int, default=30, help='listelenecek eksik ad sayısı')
    a = p.parse_args()
    r = olc(os.path.abspath(a.kojo), os.path.abspath(a.ikojo), os.path.abspath(a.betikler))
    yazdir(r, a.en_sik)
    if a.tsv:
        tsv_yaz(r, a.tsv, os.path.abspath(a.kojo))
        print(f"\nTSV yazıldı: {a.tsv}")
    if a.json:
        with open(a.json, 'w', encoding='utf-8') as f:
            json.dump(r, f, ensure_ascii=False, indent=1)
        print(f"JSON yazıldı: {a.json}")


if __name__ == '__main__':
    main()
