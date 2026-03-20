package jamgaVOCA.demo.legacy.translation

import jamgaVOCA.demo.legacy.translation.dto.CreateQuestionRequest
import jamgaVOCA.demo.legacy.translation.dto.CreateQuestionResponse
import jamgaVOCA.demo.legacy.translation.dto.EvaluateRequest
import jamgaVOCA.demo.legacy.translation.dto.EvaluateResponse
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
