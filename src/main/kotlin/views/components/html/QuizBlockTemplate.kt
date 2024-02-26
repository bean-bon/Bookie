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
        for (a in answers) {
            div {
                button(classes = "answer ${if (a.correct) "correct" else "incorrect"} untouched-answer") {
                    onClick = "revealAnswer(this, this.closest('.answer-container'))"
                    unsafe {
                        raw(a.answerHTMLPlaceholder)
                    }
                    p(classes = "description") {
                        a.explanation?.let {
                            +it
                        } ?: run {
                            if (a.correct) +"Correct"
                            else +"Incorrect"
                        }
                    }

                }
            }
        }
    }
}