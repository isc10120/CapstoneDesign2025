package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.service.generateQuestion.QuestionGeneratorService
import jamgaVOCA.demo.service.generateQuestion.dto.EvaluateRequest
import jamgaVOCA.demo.service.generateQuestion.dto.QuestionRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class QuestionController(
    private val questionGeneratorService: QuestionGeneratorService
) {

    @PostMapping("/question-generation/{questionType}")
    fun generateQuestion(
        @PathVariable questionType: String,
        @RequestBody req: QuestionRequest
    ) = ApiResponse.success(questionGeneratorService.generateQuestion(questionType, req))

    @PostMapping("/question-generation/evaluate")
    fun evaluate(
        @RequestBody req: EvaluateRequest
    ) = ApiResponse.success(questionGeneratorService.evaluate(req))
}
