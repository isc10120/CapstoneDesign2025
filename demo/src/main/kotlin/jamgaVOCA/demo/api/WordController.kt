package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.NudgeRequest
import jamgaVOCA.demo.api.dto.WordResponse
import jamgaVOCA.demo.service.WordService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class WordController(
    private val wordService: WordService
) {

    @GetMapping("/daily-word-list")
    fun getDailyWordList(): ApiResponse<List<WordResponse>> {
        val data = wordService.getDailyWordList()
        return ApiResponse.success(data)
    }

    @GetMapping("/daily-word-list/new")
    fun getNewDailyWordList(@RequestParam level: String): ApiResponse<List<WordResponse>> {
        val data = wordService.getNewDailyWordList(level)
        return ApiResponse.success(data)
    }

    @GetMapping("/word-info")
    fun getWordInfo(@RequestParam id: Long): ApiResponse<WordResponse> {
        val data = wordService.getWordInfo(id)
        return ApiResponse.success(data)
    }

    @PatchMapping("/nudge")
    fun updateNudge(@RequestBody nudgeRequests: List<NudgeRequest>): ApiResponse<Nothing> {
        wordService.updateNudge(nudgeRequests)
        return ApiResponse.success(null)
    }

    @GetMapping("/week-collected-list")
    fun getWeekCollectedList(): ApiResponse<List<WordResponse>> {
        val data = wordService.getWeekCollectedList()
        return ApiResponse.success(data)
    }
}
