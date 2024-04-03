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
    private val deferredInlineBlocks: MutableMap<String, String>,
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
                val inlineId = IDCreator.inlineBlock.nextId
                answersFound.add(
                    QuizAnswer(
                        answerHTMLPlaceholder = "<div class=\"answer-content\" tabindex=\"-1\">$inlineId</div>",
                        explanation =
                            if (explanation.isNotEmpty())
                                explanation
                                    .joinToString("")
                                    .removePrefix("C ")
                                    .trim()
                            else null,
                        correct = explanation.getOrNull(0)?.startsWith("C ") == true
                    )
                )
                deferredInlineBlocks[inlineId] = qSplit[0].trim()
            }
        }
        visitor.consumeHtml(quizBlockTemplate(activeQuestion, answersFound))
    }
}