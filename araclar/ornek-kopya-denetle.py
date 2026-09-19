#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ornek-kopya-denetle.py -- `ornekler/*.kojo` gövdelerinin Scala sınamalarındaki
kopyaları kaynaklarıyla aynı mı.

NEDEN: örnekleri GERÇEK derleyiciye gönderen yol `ornekler/ornekleri-dogrula.sh`
ve o ikojo sunucusuna ağ üstünden gidiyor -- CI ve geliştirme konteyneri oraya
çıkamıyor. Telafi olarak bir örneğin gövdesi, anahtar kelimeleri çıkarılmış
hâliyle bir sınamaya kopyalanıyor (bugün: `14-agir-dolgu.kojo` -> #68).
Kopya ELLE tutuluyor: örnek değişip kopya değişmezse sav BAYAT koda karşı
yeşil kalır ve örnek hakkında hiçbir şey kanıtlamaz -- yani telafi sessizce
telafi olmaktan çıkar.

Bu, deponun aylardır kovaladığı sınıfın aynısı (CI işinin adı da onu söylüyor:
"üretilmiş ve ÇOĞALTILMIŞ dosyalar tazeyken mi"), ve o ailede beş denetim
zaten var -- PIXI kopyası, libtess kopyası, gömülü YARDIM bloğu, renk
satırları, adlar anlık görüntüsü. Bu altıncısı. (#124 incelemesi, §3.)

NE DENETLİYOR: yalnız adı verilen TANIMIN gövdesi, Koco anahtar kelimeleri
Scala'ya çevrildikten ve yorumlar atıldıktan sonra.

NE DENETLEMİYOR: çağrı satırları. Onlar BİLEREK ayrı -- örnek 250/1000 nokta
çiziyor, sınama 20/30 ile derlemeye bakıyor (1000 noktalık bir dolguyu
sınamada hesaplamanın anlamı yok). Ayrışması beklenen yeri denetlemek,
denetimi ilk gerçek değişiklikte gürültüye çevirirdi.

Kullanım:
  araclar/ornek-kopya-denetle.py
"""
import io
import os
import re
import sys

# (örnek, sınama, tanım adı) -- bir örnek gövdesi bir sınamaya kopyalandıkça
# buraya bir satır eklenir.
ÇOĞALTMALAR = [
    ('ornekler/14-agir-dolgu.kojo', 'src/test/scala/kojo/TurkishPreludeTest.scala', 'gül'),
    ('ornekler/15-mesh-olcumu.kojo', 'src/test/scala/kojo/TurkishPreludeTest.scala', 'gülÇiz'),
]

# Örnekte Koco anahtar kelimeleri var, kopyada Scala'nınkiler. Karşılaştırma
# çeviriden SONRA: sınanan şey gövdenin aynılığı, yazımın değil.
ANAHTAR = [('tanım', 'def'), ('dez', 'val'), ('den', 'var')]


def gövde(metin, ad, anahtarlar):
    """`<anahtar> <ad>(` ile başlayan tanımın tamamını kaşlı ayraç sayarak çıkarır."""
    baş = None
    for a in anahtarlar:
        m = re.search(r'(?m)^[ \t]*%s\s+%s\s*\(' % (re.escape(a), re.escape(ad)), metin)
        if m:
            baş = m.start()
            break
    if baş is None:
        return None
    açılış = metin.find('{', baş)
    if açılış < 0:
        return None
    derinlik = 0
    for i in range(açılış, len(metin)):
        if metin[i] == '{':
            derinlik += 1
        elif metin[i] == '}':
            derinlik -= 1
            if derinlik == 0:
                return metin[baş:i + 1]
    return None


def normalize(gövde):
    """Yorumları at, anahtar kelimeleri çevir, boşlukları tekilleştir."""
    g = re.sub(r'//[^\n]*', '', gövde)
    for tr, en in ANAHTAR:
        g = re.sub(r'\b%s\b' % re.escape(tr), en, g)
    return re.sub(r'\s+', ' ', g).strip()


def main():
    kök = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    hata = 0
    for örnek, sınama, ad in ÇOĞALTMALAR:
        ö = io.open(os.path.join(kök, örnek), encoding='utf-8').read()
        s = io.open(os.path.join(kök, sınama), encoding='utf-8').read()
        a = gövde(ö, ad, [tr for tr, _ in ANAHTAR])
        b = gövde(s, ad, [en for _, en in ANAHTAR])
        # Bulunamama da hata: tanım yeniden adlandırılırsa denetim sessizce
        # hiçbir şeye bakmaz olurdu -- denetimin kendi sessiz kusuru.
        if a is None:
            print('::error::%s içinde `%s` tanımı bulunamadı.' % (örnek, ad))
            hata = 1
            continue
        if b is None:
            print('::error::%s içinde `%s` tanımı bulunamadı.' % (sınama, ad))
            hata = 1
            continue
        if normalize(a) != normalize(b):
            print('::error::%s içindeki `%s` ile %s içindeki kopyası ayrışmış.'
                  % (örnek, ad, sınama))
            print('  örnek : %s' % normalize(a))
            print('  kopya : %s' % normalize(b))
            print('  İkisi birlikte güncellenmeli (anahtar kelimeler: %s).'
                  % ', '.join('%s->%s' % k for k in ANAHTAR))
            hata = 1
        else:
            print('%s `%s` <-> %s: aynı' % (örnek, ad, sınama))
    return hata


if __name__ == '__main__':
    sys.exit(main())
