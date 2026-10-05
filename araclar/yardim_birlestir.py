# -*- coding: utf-8 -*-
"""
yardim_birlestir.py -- sozluk/yardim.json (masaüstünden ÜRETİLİR) ile
sozluk/yardim-ikojo.json (ELLE yazılır, yalnız iKojo'nun ses komutları)
birleşiminin sayfaya gömülecek metni.

NEDEN İKİ DOSYA: yardim.json masaüstü help.scala'dan üretiliyor ve her
yenilemede baştan yazılıyor; iKojo'ya özgü paneller (akorÇal, notaSus…
masaüstünde yok) orada yaşayamaz, yoksa ilk yenilemede silinirdi.
Birleşim kuralı: yardim.json'un metni OLDUĞU GİBİ kalır (sayfadaki gömme eskiden
ona bayt bayt eşitti), iKojo girdileri sonuna eklenir. Aynı anahtar iki
dosyada da varsa HATA: sessizce birinin ötekini ezmesi istenmez.

Satır biçimi help.scala dışa aktarıcısıyla aynı (kompakt JSON; `<` kaçırılır
ki `</script>` gömülü bloğu bölmesin).
"""
import io
import json
import os

ALANLAR_KOMUT = ('imza', 'açıklama', 'örnek')  # sonuç ve not isteğe bağlı
İZİNLİ_ALANLAR = ('tür', 'imza', 'açıklama', 'örnek', 'sonuç', 'not')


def satir(ad, girdi):
    s = json.dumps(girdi, ensure_ascii=False, separators=(',', ':'))
    return '  %s:%s' % (json.dumps(ad, ensure_ascii=False).replace('<', '\\u003c'), s.replace('<', '\\u003c'))


def ikojoGirdileri(yol):
    with io.open(yol, encoding='utf-8') as d:
        veri = json.load(d)
    for ad, g in veri.items():
        if g.get('tür') != 'komut':
            raise SystemExit("%s: %s için tür 'komut' olmalı" % (os.path.basename(yol), ad))
        bilinmeyen = [a for a in g if a not in İZİNLİ_ALANLAR]
        if bilinmeyen:
            raise SystemExit('%s: %s için bilinmeyen alan: %s (izinli: %s)' % (
                os.path.basename(yol), ad, ', '.join(bilinmeyen), ', '.join(İZİNLİ_ALANLAR)))
        eksik = [a for a in ALANLAR_KOMUT if not g.get(a)]
        if eksik:
            raise SystemExit('%s: %s için alan eksik: %s' % (os.path.basename(yol), ad, ', '.join(eksik)))
    return veri


def birlesik(yardimMetni, ikojoYolu):
    """yardim.json metni + iKojo girdileri -> gömülecek metin."""
    ikojo = ikojoGirdileri(ikojoYolu)
    var = json.loads(yardimMetni)
    çakışan = sorted(set(var) & set(ikojo))
    if çakışan:
        raise SystemExit('yardim.json ile yardim-ikojo.json aynı anahtarı taşıyor: %s' % ', '.join(çakışan))
    if not ikojo:
        return yardimMetni
    gövde = yardimMetni.rstrip('\n')
    assert gövde.endswith('}'), 'yardim.json beklenen biçimde değil'
    gövde = gövde[:-1].rstrip('\n')  # son "}" gitti; son satırda virgül yok
    ek = ',\n'.join(satir(a, g) for a, g in ikojo.items())
    return gövde + ',\n' + ek + '\n}\n'
