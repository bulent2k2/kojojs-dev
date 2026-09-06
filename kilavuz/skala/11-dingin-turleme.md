# Dingin Türleme ve Tür Çıkarımı

Scala dingin türleme (static typing) yapan bir dil. Bu ne demek? Bütün değişkenlerin, değişmezlerin ve işlevlerin türü, program çalışmadan önce, yani derleme sırasında belirleniyor. Bütün diller böyle değil. Örneğin pek çok yazılımcının favorilerinden olan Python ve Ruby, devingen türlemeli diller (dynamically typed languages). Dingin türleme sayesinde pek çok yazım ve mantık hatası programı çalıştırmadan önce yani derleme sırasında yakalanır. Örneğin, yanlışlıkla bir işleve uygun olmayan tür bir nesneyi girdi olarak sokarsak, derleyici hemen yakalar ve bizden hatayı düzeltmemizi ister. Kısaca, her değerin türünün önceden belli olması hem faydalı hem de gerekli. Ama, buna rağmen yazılımcık örneklerimizde tür tanımlarının pek az olduğunu farketmiş olabilirsin. Bu nasıl oluyor? Çünkü Scala tür çıkarımında çok becerikli. Tür çıkarımı (type inference) bir değerin türünü kullanıldığı yere bakarak belirlemek demek.

Örneğin, `dez x = 3` yazalım. Derleyici elbette 3'ün yalın bir değer olduğunu ve ayrıca ne tür bir sayı olduğunu biliyor. 3 (kesirsiz bir) Sayı. Onun için derleyici `x` değişmezinin türünün de Sayı olması gerektiği çıkarımında bulunuyor ve elbette yanılmıyor. Az da olsa bir kaç durum var ki derleyici tür çıkarımını beceremeyebiliyor. İşte o zaman, kafam karıştı der ve bir hata verir. Hatanın türü "type ambiguity" gibi birşeylerdir, yani tür belirsizliği. O durumda gerekli tür bilgisini yazılımcığa ekleyiverirsin.

```scala
dez x = 3          // derleyici: x bir Sayı
dez y = 3.0        // derleyici: y bir Kesir
dez z = "üç"       // derleyici: z bir Yazı
tanım iki(a: Sayı) = a * 2   // çıktının türünü derleyici çıkarır: Sayı
satıryaz(iki(x))
// Şunu deneyip hatayı gör: iki(y)
```

Genelde işlevlerin girdilerinin türünü tanımlarız. Ama çıktının türünü belirtmek gerekmez. Çünkü derleyici işlevin tanımından çıktının türünün çıkarımını yapıverir. Bunun istisnası özyineleyen, yani kendi kendini çağıran işlevler. Pek çok örneğini gördük elbet. Onların çıktısının türünü hep belirttik.

Tür çıkarımı sayesinde programların doğru çalışması için yazmamız gereken sözcük ve harf sayısı epey azalır. Bu da elbette çok işimize yarar. Ayrıca yazılım daha okunur hale gelir ve anlaşılması da kolaylaşır. Bu becerisi sayesinde Scala da Python ve Ruby gibi diller gibi sade ve yalın gelir göze.
