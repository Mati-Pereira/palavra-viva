# Texto bíblico empacotado (`assets/bible/`)

Texto completo da Bíblia em português (66 livros, 1.189 capítulos, 31.102
versículos), pronto para importação ao Room (ver “Notas para a importação”
abaixo).

## Versão e licença

| | |
|---|---|
| **Versão** | Bíblia Livre (**BLIVRE**), 2018 |
| **Base** | Atualização da tradução de **João Ferreira de Almeida (1819)**, em domínio público; Novo Testamento baseado no *Textus Receptus* |
| **Licença do texto** | **CC BY 3.0** — <https://creativecommons.org/licenses/by/3.0/br/> |
| **Upstream** | <https://github.com/blivre/BibliaLivre> |
| **Fonte utilizada** | Projeto **biblia.publica** — <https://github.com/Projeto-Euaggelion/biblia.publica> (arquivos `versoes/blivre/json/blivre-<id>.json`, declarados como “Bíblia Protestante, 66 livros completos”) |

**Por que não a ARA?** A Almeida Revista e Atualizada é © Sociedade Bíblica
do Brasil e não pode ser redistribuída dentro do app sem autorização. A
BLIVRE foi escolhida por (a) ser legalmente redistribuível (CC BY 3.0,
permite uso comercial com atribuição), (b) pertencer à tradição Almeida
(derivada da edição de 1819, em domínio público) e (c) usar português atual
— ao contrário da Almeida 1911 (também em domínio público, disponível no
mesmo projeto), cuja grafia é arcaica (“creou”, “peccadores”).

### Obrigações da licença (CC BY 3.0)

A licença exige **atribuição**: dar o crédito apropriado, prover um link para
a licença e **indicar que mudanças foram feitas**. As mudanças aplicadas
aqui são apenas de empacotamento/formatação e estão listadas em
“Ajustes aplicados na conversão” abaixo.

> **Pendência de UI:** a tela “Sobre/Aparência” do app deve exibir, em algum
> lugar visível, o crédito “Texto bíblico: Bíblia Livre (BLIVRE), CC BY 3.0 —
> bibliaeuaggelion / blivre”, com link para a licença. (Os arquivos Kotlin de
> UI estão fora do escopo desta etapa.)

## Estrutura dos arquivos

Um arquivo JSON por livro, em UTF-8, nomeado pelo id do livro
(ordem canônica: `gn`, `ex`, …, `ap`):

```json
{
  "bookId": "gn",
  "name": "Gênesis",
  "testament": "AT",
  "chapters": [
    {
      "number": 1,
      "verses": [
        { "number": 1, "text": "No princípio criou Deus os céus e a terra." }
      ]
    }
  ]
}
```

- `bookId` — id curto e estável do livro (convenção idêntica à usada pelos
  repositórios biblia.publica e thiagobodruk/bible; ex.: `1sm`, `sl`, `1co`, `ap`).
- `testament` — `"AT"` ou `"NT"`.
- `number` de capítulo/versículo começa em 1 e é sequencial — o número também
  pode ser obtido pela posição no array.
- `text` — texto plano; sem notas de rodapé, títulos de seção ou marcação de
  poesia (a edição BLIVRE traz essas notas apenas no upstream, não aqui).

Além dos 66 livros, o **`index.json`** traz o catálogo completo
(`bookId`, `name`, `testament`, nº de capítulos e de versículos, na ordem
canônica) — use-o para montar a lista de livros da UI ou validar a
importação sem abrir os 66 arquivos.

## Ajustes aplicados na conversão

Nenhuma palavra do texto foi alterada. Os ajustes abaixo são de
empacotamento/formatação e estão implementados (com validação) em
[`tools/bible/convert_biblia_publica.py`](../../../tools/bible/convert_biblia_publica.py):

1. **Marcos 5:18–19 redividido** — a fonte mesclava os dois versículos num
   único objeto numerado 18 (com um espaço faltando entre as frases) e saltava
   para o v. 20. O texto foi redividido no ponto de sentença, restaurando a
   numeração canônica (Mc 5 voltou a ter 43 versículos). As palavras são
   exatamente as mesmas da fonte.
2. **Atos 28:29 mantido vazio** — a edição BLIVRE não traz o texto deste
   versículo (no upstream há apenas a nota “N4 não contém todo o versículo
   29”, sem o texto do TR). Para não misturar traduções, o versículo é
   preservado com `text: ""`, mantendo a numeração alinhada (o leitor passa
   de 28:28 direto para 28:30, como em edições de texto crítico).
3. **Acentuação de nomes corrigida** — `Oseias` → `Oséias`,
   `Miqueias` → `Miquéias`. Os demais nomes seguem a fonte (`Cantares`,
   `Lamentações` etc., grafias usuais em português).

## Notas para a importação ao Room (próxima etapa)

- Total esperado: **66 livros, 1.189 capítulos, 31.102 versículos**
  (contagem canônica protestante).
- Ler `index.json` primeiro para a tabela de livros; depois abrir cada
  `<bookId>.json` e popular `Verse(bookId, chapter, number, text)`.
- **At 28:29 tem texto vazio por design** (ver ajuste 2 acima) — a importação
  deve aceitar string vazia; se preferir, pode ignorar o versículo no banco.
- Os JSONs são compactos (sem indentação) para reduzir o APK; total ≈ 4,4 MB.

## Como regenerar / atualizar

Requisitos: Python 3 (stdlib apenas).

```bash
# 1) Sem rede local prévia — o script baixa a fonte sozinho:
python3 tools/bible/convert_biblia_publica.py

#    (equivalente manual: baixar os 66 arquivos
#     https://raw.githubusercontent.com/Projeto-Euaggelion/biblia.publica/main/versoes/blivre/json/blivre-<id>.json
#     para uma pasta e rodar com --source-dir <pasta>)

# 2) Com uma cópia local da fonte (offline / reproduzível):
python3 tools/bible/convert_biblia_publica.py --source-dir <pasta-com-os-66-json>
```

O script valida a cada execução: ids/ordem canônica, contagem de capítulos,
numeração sequencial de capítulos e versículos, texto não-vazio (exceto a
exceção documentada) — e falha se a fonte divergir. Para trocar de versão
(ex.: Almeida 1911 em domínio público, NVA em CC BY-SA), ajuste `SOURCE_URL`,
`BOOKS` e os patches no topo do script e atualize a licença/atribuição
neste README.
