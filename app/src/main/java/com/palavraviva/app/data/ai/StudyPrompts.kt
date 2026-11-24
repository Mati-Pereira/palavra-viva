package com.palavraviva.app.data.ai

/**
 * Prompts e instruções do sistema para estudo bíblico com foco histórico,
 * cultural e linguístico neutro em português brasileiro.
 */
object StudyPrompts {

    val VALID_BOOK_IDS: Set<String> = setOf(
        "gn", "ex", "lv", "nm", "dt", "js", "jz", "rt", "1sm", "2sm",
        "1rs", "2rs", "1cr", "2cr", "ed", "ne", "et", "job", "sl", "pv",
        "ec", "ct", "is", "jr", "lm", "ez", "dn", "os", "jl", "am",
        "ob", "jn", "mq", "na", "hc", "sf", "ag", "zc", "ml",
        "mt", "mc", "lc", "jo", "at", "rm", "1co", "2co", "gl", "ef",
        "fp", "cl", "1ts", "2ts", "1tm", "2tm", "tt", "fm", "hb", "tg",
        "1pe", "2pe", "1jo", "2jo", "3jo", "jd", "ap",
    )

    val SYSTEM_INSTRUCTION = """
        Você é um especialista em estudo bíblico histórico-cultural e linguístico.
        Responda sempre em português brasileiro claro, didático e acessível.
        Adote uma abordagem neutra, informativa e sem viés denominacional ou polêmica teológica.
        Priorize:
        1. Contexto histórico, geográfico, social e político da época da passagem.
        2. Costumes, profissões, leis, medidas ou metáforas que faziam sentido naquele período.
        3. Significado de termos relevantes nos idiomas originais (hebraico, aramaico ou grego koiné), quando enriquecerem a compreensão.
        4. Fidelidade estrita ao texto bíblico apresentado, evitando especulações.
    """.trimIndent()

    private val BOOK_ID_GUIDE = """
        gn=Gênesis, ex=Êxodo, lv=Levítico, nm=Números, dt=Deuteronômio, js=Josué, jz=Juízes, rt=Rute,
        1sm=1 Samuel, 2sm=2 Samuel, 1rs=1 Reis, 2rs=2 Reis, 1cr=1 Crônicas, 2cr=2 Crônicas,
        ed=Esdras, ne=Neemias, et=Ester, job=Jó, sl=Salmos, pv=Provérbios, ec=Eclesiastes, ct=Cantares,
        is=Isaías, jr=Jeremias, lm=Lamentações, ez=Ezequiel, dn=Daniel, os=Oséias, jl=Joel, am=Amós,
        ob=Obadias, jn=Jonas, mq=Miquéias, na=Naum, hc=Habacuque, sf=Sofonias, ag=Ageu, zc=Zacarias, ml=Malaquias,
        mt=Mateus, mc=Marcos, lc=Lucas, jo=João, at=Atos, rm=Romanos, 1co=1 Coríntios, 2co=2 Coríntios,
        gl=Gálatas, ef=Efésios, fp=Filipenses, cl=Colossenses, 1ts=1 Tessalonicenses, 2ts=2 Tessalonicenses,
        1tm=1 Timóteo, 2tm=2 Timóteo, tt=Tito, fm=Filemom, hb=Hebreus, tg=Tiago,
        1pe=1 Pedro, 2pe=2 Pedro, 1jo=1 João, 2jo=2 João, 3jo=3 João, jd=Judas, ap=Apocalipse
    """.trimIndent()

    fun buildVerseStudyPrompt(
        bookName: String,
        bookId: String,
        chapter: Int,
        verseNumber: Int,
        verseText: String,
        surroundingVerses: String,
    ): String = """
        Analise a passagem bíblica abaixo para facilitar o estudo pessoal:
        Referência: $bookName $chapter:$verseNumber (bookId: "$bookId")
        Versículo em foco: "$verseText"
        Contexto imediato no capítulo:
        $surroundingVerses

        Retorne APENAS um objeto JSON válido com exatamente esta estrutura:
        {
          "summary": "Explicação clara e objetiva do sentido do versículo dentro do seu contexto imediato (2 a 4 frases).",
          "historicalContext": "Panorama histórico, cultural, geográfico ou literário da época que ajuda a compreender por que isso foi dito/escrito dessa maneira (3 a 5 frases).",
          "periodTerms": [
            {
              "term": "Expressão ou termo cultural/histórico presente no versículo ou contexto",
              "originalWord": "Termo transliterado no original hebraico/grego/aramaico (ou null se não aplicável)",
              "meaning": "Explicação do que significava naquela época e cultura (1 a 3 frases)."
            }
          ],
          "crossReferences": [
            {
              "bookId": "código exato do livro da lista abaixo",
              "chapter": 1,
              "verse": 1,
              "referenceLabel": "Nome do Livro Capítulo:Versículo",
              "reason": "Breve motivo da relação temática, histórica ou textual (1 frase)."
            }
          ]
        }

        Regras:
        - Em "periodTerms", inclua de 1 a 4 termos realmente úteis para entender a época; se não houver termos específicos, destaque 1 expressão-chave do idioma original.
        - Em "crossReferences", inclua de 2 a 4 passagens realmente relacionadas (evite repetir o mesmo versículo em foco).
        - Use obrigatoriamente "bookId" exato desta tabela:
        $BOOK_ID_GUIDE
    """.trimIndent()

    fun buildChapterGuidePrompt(
        bookName: String,
        bookId: String,
        chapter: Int,
        chapterText: String,
    ): String = """
        Crie um guia de contexto de estudo para este capítulo bíblico:
        Livro: $bookName (bookId: "$bookId"), Capítulo: $chapter
        Texto do capítulo:
        $chapterText

        Retorne APENAS um objeto JSON válido com exatamente esta estrutura:
        {
          "overview": "Resumo estruturado dos acontecimentos, argumentos ou temas centrais do capítulo (3 a 5 frases).",
          "bookPlacement": "Como este capítulo se conecta com o momento do livro de $bookName e seu cenário histórico (2 a 4 frases).",
          "keyFiguresAndPlaces": [
            "Personagem, grupo ou lugar citado — breve descrição histórica"
          ],
          "reflectionQuestions": [
            "Pergunta aberta e neutra para ajudar o leitor a observar e refletir sobre o texto"
          ]
        }

        Regras:
        - Em "keyFiguresAndPlaces", liste de 2 a 6 itens relevantes citados ou diretamente ligados ao capítulo.
        - Em "reflectionQuestions", forneça 2 ou 3 perguntas que estimulem leitura atenta do texto e compreensão do contexto.
    """.trimIndent()

    fun buildVerseChatContext(
        bookName: String,
        chapter: Int,
        verseNumber: Int,
        verseText: String,
        surroundingVerses: String,
    ): String = """
        [Contexto da passagem que o usuário está estudando]
        Referência em foco: $bookName $chapter:$verseNumber
        Texto do versículo: "$verseText"
        Versículos ao redor:
        $surroundingVerses

        Responda de forma clara, direta, historicamente embasada e neutra (sem viés denominacional).
    """.trimIndent()

    fun buildChapterChatContext(
        bookName: String,
        chapter: Int,
        chapterExcerpt: String,
    ): String = """
        [Contexto do capítulo que o usuário está estudando]
        Livro e capítulo: $bookName $chapter
        Trecho/conteúdo do capítulo:
        $chapterExcerpt

        Responda de forma clara, direta, historicamente embasada e neutra (sem viés denominacional).
    """.trimIndent()
}
