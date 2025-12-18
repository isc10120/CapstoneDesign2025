package jamgaVOCA.demo.translation

import jamgaVOCA.demo.translation.dto.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/translation")
class TranslationController(
    private val service: TranslationService
) {

    @PostMapping("/question")
    fun createQuestion(@RequestBody req: CreateQuestionRequest): CreateQuestionResponse =
        service.createQuestion(req)

    @PostMapping("/evaluate")
    fun evaluate(@RequestBody req: EvaluateRequest): EvaluateResponse =
        service.evaluate(req)
}
