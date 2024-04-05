package backend.html.providers

import backend.html.helpers.IDCreator
import backend.model.QuizAnswer
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import views.components.html.quizBlockTemplate

class BDQuizProvider(
    private val getActiveQuestion: () -> String?,
    private val deferredQuizAnswers: MutableMap<String, String>,
    private val ulGeneratingProvider: GeneratingProvider,
): GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val activeQuestion = getActiveQuestion() ?: return ulGeneratingProvider.processNode(visitor, text, node)
        val answersFound = mutableListOf<QuizAnswer>()
        for (a in node.children) {
            val question = a.getTextInNode(text)
            if (question.startsWith("- ")) {
                val qSplit = question.removePrefix("- ").split("|")
                val explanation = qSplit.drop(1)
                val inlineId = IDCreator.answer.nextId
                answersFound.add(
                    QuizAnswer(
                        answerHTMLPlaceholder = "<div class=\"answer-content\" tabindex=\"-1\">$inlineId</div>",
                        explanation = explanation
                            .joinToString("")
                            .removePrefix("C ")
                            .trim(),
                        correct = explanation
                            .getOrNull(0)
                            ?.startsWith("C ") == true
                    )
                )
                deferredQuizAnswers[inlineId] = qSplit[0].trim()
            }
        }
        visitor.consumeHtml(quizBlockTemplate(activeQuestion, answersFound))
    }
}