# Araçlar

## `ucurum.py` — masaüstü ↔ ikojo uçurum ölçümü

Masaüstü Koco'nun Türkçe betiklerini (`ornekler/masaustu/`) tarar; her betikteki
Türkçe API adını masaüstü tanımlarıyla (`bulent2k2/kojo`: `lite/i18n/trInit.scala`,
`lite/i18n/tr/*.scala`) ve ikojo tanımlarıyla (`src/main/scala/kojo/TurkishTurtle.scala`,
`kojo/tr/*.scala`, İngilizce yüzey `kojo/*.scala`) karşılaştırır. Platform
engellerini (`#yükle`, Swing `ay.*`, ses, öykü, dosya…) işaretler.

Betik başına üç durum:

| durum | anlam |
|---|---|
| `çalışır` | eksik ad yok, platform engeli yok — ikojo'da olduğu gibi derlenmesi beklenir |
| `eksik-ad` | ikojo'da tanımlı olmayan Türkçe ad(lar) kullanıyor; liste TSV'de |
| `platform` | tarayıcıda karşılığı olmayan özellik kullanıyor (engel adı TSV'de) |

```sh
araclar/ucurum.py                                     # ../kojo klonunu bekler
araclar/ucurum.py --kojo ~/src/kojo --en-sik 40
araclar/ucurum.py --tsv ornekler/masaustu/tarama.tsv  # repodaki tarama dosyasını yenile
araclar/ucurum.py --json /tmp/ucurum.json             # tam rapor (eksik adlar, engeller)
```

Bu bir **tanımlayıcı taramasıdır**, derleme değil: imza farklarını (parametre
türü/sayısı) göremez. Gerçek derleme denetimi `ornekler/ornekleri-dogrula.sh`.
İki araç birbirini tamamlar: tarama *neyin* eksik olduğunu söyler, derleme
*gerçekten geçip geçmediğini*.

Plan ve ölçüm belgesi: Koco–ikojo Köprüsü (Claude artifact,
<https://claude.ai/code/artifact/04147d3d-1a10-4b18-a586-d2a105a07764>).
