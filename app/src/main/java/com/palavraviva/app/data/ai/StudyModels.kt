package com.palavraviva.app.data.ai

/** Termo histórico, cultural ou do idioma original presente na passagem. */
data class PeriodTerm(
    val term: String,
    val originalWord: String? = null,
    val meaning: String,
)

/** Referência cruzada navegável dentro do leitor do aplicativo. */
data class CrossReference(
    val bookId: String,
    val chapter: Int,
    val verse: Int? = null,
    val referenceLabel: String,
    val reason: String,
)

/** Estudo estruturado de um versículo específico. */
data class VerseStudy(
    val summary: String,
    val historicalContext: String,
    val periodTerms: List<PeriodTerm>,
    val crossReferences: List<CrossReference>,
)

/** Guia de panorama e contexto de um capítulo inteiro. */
data class ChapterGuide(
    val overview: String,
    val bookPlacement: String,
    val keyFiguresAndPlaces: List<String>,
    val reflectionQuestions: List<String>,
)

/** Metadados de um modelo retornado pela API do Gemini. */
data class GeminiModelInfo(
    val id: String,
    val displayName: String,
)

/** Erros tipados das chamadas ao assistente de IA. */
sealed class AiError(override val message: String) : Exception(message) {
    class MissingApiKey : AiError("Configure sua chave da API do Gemini nos Ajustes para usar a IA.")
    class InvalidApiKey(detail: String? = null) : AiError(
        detail ?: "Chave da API do Gemini inválida ou sem permissão. Verifique em Ajustes.",
    )
    class NetworkError : AiError("Sem conexão com a internet. Estudos já salvos continuam disponíveis offline.")
    class RateLimited : AiError("Limite temporário de requisições da API atingido. Aguarde um instante e tente novamente.")
    class ParseError : AiError("A resposta da IA veio em formato inesperado. Tente novamente.")
    class ApiError(detail: String) : AiError(detail)
}
