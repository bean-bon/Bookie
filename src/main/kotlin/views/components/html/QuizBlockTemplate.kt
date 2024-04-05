package views.components.html

import backend.model.QuizAnswer
import kotlinx.html.*
import kotlinx.html.stream.createHTML

/**
 * Template for building quiz blocks.
 * @param question That being asked.
 * @param answers HTML strings for each answer.
 */
fun quizBlockTemplate(
    question: String,
    answers: List<QuizAnswer>,
) = createHTML().div("quiz-block") {
    strong {
        p("quiz-question") {
            +question
        }
    }
    div("answer-container") {
        attributes["aria-label"] = "Quiz block: $question"
        for (a in answers) {
            button(classes = "answer ${if (a.correct) "correct" else "incorrect"} untouched-answer") {
                attributes["tabindex"] = "0"
                onClick = "revealAnswer(this, this.closest('.answer-container'))"
                unsafe {
                    raw(a.answerHTMLPlaceholder)
                }
                p(classes = "description") {
                    attributes["tabindex"] = "-1"
                    attributes["aria-hidden"] = "true"
                    attributes["aria-live"] = "polite"
                    attributes["aria-label"] = "is ${if (a.correct) "correct" else "incorrect"}: ${a.explanation}"
                    a.explanation.let {
                        if (it.isNotEmpty()) +it
                        else if (a.correct) +"Correct"
                        else +"Incorrect"
                    }
                }
            }
        }
    }
}