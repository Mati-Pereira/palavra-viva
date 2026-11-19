#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Converte a Bíblia Livre (BLIVRE), obtida via projeto biblia.publica, para o
formato empacotado no app Palavra Viva: ``app/src/main/assets/bible/``.

Saída (um arquivo por livro, ``app/src/main/assets/bible/<bookId>.json``):

    {
      "bookId": "gn",
      "name": "Gênesis",
      "testament": "AT",            # "AT" ou "NT"
      "chapters": [
        { "number": 1, "verses": [ { "number": 1, "text": "..." } ] }
      ]
    }

Também gera ``app/src/main/assets/bible/index.json`` com o catálogo dos
66 livros (ordem, bookId, nome, testamento e nº de capítulos), útil para a
lista de livros da UI e para a importação futura ao Room sem abrir os 66
arquivos.

Fonte
-----
Projeto biblia.publica (https://github.com/Projeto-Euaggelion/biblia.publica),
arquivos ``versoes/blivre/json/blivre-<id>.json``, derivados da Bíblia Livre
(https://github.com/blivre/BibliaLivre) — atualização da tradução de João
Ferreira de Almeida de 1819 (domínio público), NT baseado no Textus Receptus.
Licença do texto: CC BY 3.0 (permite redistribuição, inclusive comercial,
com atribuição). Detalhes e licença completa: veja o README em
``app/src/main/assets/bible/README.md``.

Ajustes aplicados na conversão (todos documentados no README):
  1. ``mc`` 5 — a fonte mescla os vv. 18–19 num único versículo numerado 18
     (com espaço faltando entre as frases); o texto é redividido no ponto de
     sentença para restaurar a numeração canônica (nenhuma palavra é
     alterada).
  2. ``at`` 28:29 — a edição BLIVRE não traz o texto do versículo (apenas
     nota). Mantido como versículo com texto vazio para preservar a
     numeração; nada é importado de outra tradução.
  3. Nomes de livros com acentuação corrigida: Oseias → Oséias,
     Miqueias → Miquéias.

Uso
-----
    python3 tools/bible/convert_biblia_publica.py [--source-dir DIR] [--output-dir DIR]

Sem ``--source-dir``, o script baixa os 66 JSONs da fonte para um diretório
temporário (requer rede). Sem ``--output-dir``, escreve em
``app/src/main/assets/bible/`` (raiz do repositório detectada a partir da
posição deste script).
"""

import argparse
import json
import sys
import tempfile
import urllib.request
from pathlib import Path

SOURCE_URL = (
    "https://raw.githubusercontent.com/Projeto-Euaggelion/biblia.publica/"
    "main/versoes/blivre/json/blivre-{book_id}.json"
)

# Catálogo canônico: ordem, id, nome, testamento e nº de capítulos.
# Os ids seguem a convenção do projeto biblia.publica (idêntica à do
# repositório thiagobodruk/bible), na ordem canônica: gn, ex, ..., ap.
BOOKS = [
    ("gn", "Gênesis", "AT", 50),
    ("ex", "Êxodo", "AT", 40),
    ("lv", "Levítico", "AT", 27),
    ("nm", "Números", "AT", 36),
    ("dt", "Deuteronômio", "AT", 34),
    ("js", "Josué", "AT", 24),
    ("jz", "Juízes", "AT", 21),
    ("rt", "Rute", "AT", 4),
    ("1sm", "1 Samuel", "AT", 31),
    ("2sm", "2 Samuel", "AT", 24),
    ("1rs", "1 Reis", "AT", 22),
    ("2rs", "2 Reis", "AT", 25),
    ("1cr", "1 Crônicas", "AT", 29),
    ("2cr", "2 Crônicas", "AT", 36),
    ("ed", "Esdras", "AT", 10),
    ("ne", "Neemias", "AT", 13),
    ("et", "Ester", "AT", 10),
    ("job", "Jó", "AT", 42),
    ("sl", "Salmos", "AT", 150),
    ("pv", "Provérbios", "AT", 31),
    ("ec", "Eclesiastes", "AT", 12),
    ("ct", "Cantares", "AT", 8),
    ("is", "Isaías", "AT", 66),
    ("jr", "Jeremias", "AT", 52),
    ("lm", "Lamentações", "AT", 5),
    ("ez", "Ezequiel", "AT", 48),
    ("dn", "Daniel", "AT", 12),
    ("os", "Oséias", "AT", 14),
    ("jl", "Joel", "AT", 3),
    ("am", "Amós", "AT", 9),
    ("ob", "Obadias", "AT", 1),
    ("jn", "Jonas", "AT", 4),
    ("mq", "Miquéias", "AT", 7),
    ("na", "Naum", "AT", 3),
    ("hc", "Habacuque", "AT", 3),
    ("sf", "Sofonias", "AT", 3),
    ("ag", "Ageu", "AT", 2),
    ("zc", "Zacarias", "AT", 14),
    ("ml", "Malaquias", "AT", 4),
    ("mt", "Mateus", "NT", 28),
    ("mc", "Marcos", "NT", 16),
    ("lc", "Lucas", "NT", 24),
    ("jo", "João", "NT", 21),
    ("at", "Atos", "NT", 28),
    ("rm", "Romanos", "NT", 16),
    ("1co", "1 Coríntios", "NT", 16),
    ("2co", "2 Coríntios", "NT", 13),
    ("gl", "Gálatas", "NT", 6),
    ("ef", "Efésios", "NT", 6),
    ("fp", "Filipenses", "NT", 4),
    ("cl", "Colossenses", "NT", 4),
    ("1ts", "1 Tessalonicenses", "NT", 5),
    ("2ts", "2 Tessalonicenses", "NT", 3),
    ("1tm", "1 Timóteo", "NT", 6),
    ("2tm", "2 Timóteo", "NT", 4),
    ("tt", "Tito", "NT", 3),
    ("fm", "Filemom", "NT", 1),
    ("hb", "Hebreus", "NT", 13),
    ("tg", "Tiago", "NT", 5),
    ("1pe", "1 Pedro", "NT", 5),
    ("2pe", "2 Pedro", "NT", 3),
    ("1jo", "1 João", "NT", 5),
    ("2jo", "2 João", "NT", 1),
    ("3jo", "3 João", "NT", 1),
    ("jd", "Judas", "NT", 1),
    ("ap", "Apocalipse", "NT", 22),
]

# Acentuação corrigida em relação à fonte (erros tipográficos evidentes).
NAME_FIXES = {"os": "Oséias", "mq": "Miquéias"}

# Versículos que a edição BLIVRE não traz — mantidos com texto vazio para
# preservar a numeração canônica (nada é importado de outra tradução).
ALLOWED_EMPTY_VERSES = {("at", 28, 29)}


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def download_sources(target_dir: Path) -> None:
    """Baixa os 66 JSONs da fonte (biblia.publica) para target_dir."""
    target_dir.mkdir(parents=True, exist_ok=True)
    for book_id, _, _, _ in BOOKS:
        url = SOURCE_URL.format(book_id=book_id)
        dest = target_dir / f"blivre-{book_id}.json"
        print(f"baixando {book_id} ...", file=sys.stderr)
        with urllib.request.urlopen(url, timeout=60) as resp:
            dest.write_bytes(resp.read())


def load_source(source_dir: Path, book_id: str) -> dict:
    path = source_dir / f"blivre-{book_id}.json"
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def patch_marcos5(chapters: list) -> None:
    """
    Restaura a divisão canônica de Mc 5.18-19.

    A fonte mescla os dois versículos num único objeto numerado 18 (com um
    espaço faltando entre as frases) e pula direto para o v. 20. O texto é
    redividido no ponto de sentença — nenhuma palavra é alterada.
    Idempotente: se o capítulo já contém o v. 19, não faz nada.
    """
    chapter = chapters[4]  # Mc 5
    numbers = [v["number"] for v in chapter["verses"]]
    if 19 in numbers:
        return  # já aplicado (ou fonte corrigida no upstream)

    verses = chapter["verses"]
    merged = next((v for v in verses if v["number"] == 18), None)
    if merged is None:
        raise ValueError("Mc 5: versículo 18 não encontrado na fonte")

    marker = "com ele.Porém Jesus não lhe permitiu"
    text = merged["text"]
    idx = text.find(marker)
    if idx == -1:
        raise ValueError(
            "Mc 5:18: marcador da redivisão não encontrado — "
            f"verificar manualmente o texto da fonte: {text[:120]!r}"
        )
    split_at = idx + len("com ele.")
    v18_text = text[:split_at].strip()
    v19_text = text[split_at:].strip()

    merged["text"] = v18_text
    v18_index = verses.index(merged)
    verses.insert(v18_index + 1, {"number": 19, "text": v19_text})


def convert_book(source_dir: Path, book_id: str, name: str, testament: str,
                 n_chapters: int) -> dict:
    src = load_source(source_dir, book_id)

    if src.get("abbrev") != book_id:
        raise ValueError(f"{book_id}: abbrev divergente na fonte: {src.get('abbrev')!r}")

    chapters = src["chapters"]
    if book_id == "mc":
        patch_marcos5(chapters)

    if len(chapters) != n_chapters:
        raise ValueError(
            f"{book_id}: {len(chapters)} capítulos na fonte, esperados {n_chapters}"
        )
    for i, chapter in enumerate(chapters, start=1):
        if chapter["number"] != i:
            raise ValueError(f"{book_id}: capítulo {i} numerado como {chapter['number']}")
        for j, verse in enumerate(chapter["verses"], start=1):
            if verse["number"] != j:
                raise ValueError(
                    f"{book_id} {i}:{j}: versículo numerado como {verse['number']}"
                )
            text = (verse.get("text") or "").strip()
            if not text and (book_id, i, j) not in ALLOWED_EMPTY_VERSES:
                raise ValueError(f"{book_id} {i}:{j}: texto vazio")
            verse["text"] = text

    return {
        "bookId": book_id,
        "name": NAME_FIXES.get(book_id, src.get("name") or name),
        "testament": testament,
        "chapters": [
            {"number": c["number"], "verses": [
                {"number": v["number"], "text": v["text"]} for v in c["verses"]
            ]}
            for c in chapters
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Converte a BLIVRE (biblia.publica) para app/src/main/assets/bible/."
    )
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=None,
        help="Diretório com os blivre-<id>.json já baixados. Sem esta opção, "
             "o script baixa a fonte para um diretório temporário (requer rede).",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Diretório de saída (padrão: app/src/main/assets/bible).",
    )
    args = parser.parse_args()

    output_dir = args.output_dir or (repo_root() / "app" / "src" / "main" / "assets" / "bible")
    output_dir.mkdir(parents=True, exist_ok=True)

    tmp_dir = None
    if args.source_dir is None:
        tmp_dir = Path(tempfile.mkdtemp(prefix="blivre-src-"))
        download_sources(tmp_dir)
        source_dir = tmp_dir
    else:
        source_dir = args.source_dir

    total_chapters = 0
    total_verses = 0
    index_books = []

    try:
        for order, (book_id, name, testament, n_chapters) in enumerate(BOOKS, start=1):
            book = convert_book(source_dir, book_id, name, testament, n_chapters)
            dest = output_dir / f"{book_id}.json"
            with open(dest, "w", encoding="utf-8") as fh:
                json.dump(book, fh, ensure_ascii=False, separators=(",", ":"))
                fh.write("\n")
            n_verses = sum(len(c["verses"]) for c in book["chapters"])
            total_chapters += len(book["chapters"])
            total_verses += n_verses
            index_books.append({
                "order": order,
                "bookId": book_id,
                "name": book["name"],
                "testament": testament,
                "chapters": len(book["chapters"]),
                "verses": n_verses,
            })

        index = {
            "version": "blivre",
            "versionName": "Bíblia Livre (BLIVRE)",
            "language": "pt-BR",
            "source": "https://github.com/Projeto-Euaggelion/biblia.publica (versão blivre)",
            "upstream": "https://github.com/blivre/BibliaLivre",
            "license": "CC BY 3.0 — https://creativecommons.org/licenses/by/3.0/br/",
            "books": index_books,
        }
        with open(output_dir / "index.json", "w", encoding="utf-8") as fh:
            json.dump(index, fh, ensure_ascii=False, indent=1)
            fh.write("\n")
    finally:
        if tmp_dir is not None:
            import shutil
            shutil.rmtree(tmp_dir, ignore_errors=True)

    expected_books = len(BOOKS)
    expected_chapters = sum(b[3] for b in BOOKS)
    print(f"Livros:    {expected_books}")
    print(f"Capítulos: {total_chapters} (esperados {expected_chapters})")
    print(f"Versículos: {total_verses}")
    if total_chapters != expected_chapters:
        print("ERRO: contagem de capítulos divergente", file=sys.stderr)
        return 1
    print(f"OK: {expected_books} livros gravados em {output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
