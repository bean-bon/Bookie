package backend.model

/**
 * The format of quiz answers received from the Bookie quiz provider.
 * @author Benjamin Groom
 * @see backend.html.providers.BDQuizProvider
 */
data class QuizAnswer(
    val answerHTMLPlaceholder: String,
    val explanation: String?,
    val correct: Boolean
)
