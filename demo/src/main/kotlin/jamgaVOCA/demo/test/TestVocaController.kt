package jamgaVOCA.demo.test

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CopyOnWriteArraySet

@RestController
@RequestMapping("/test")
class TestVocaController {

    // ---- Mock Storage (in-memory) ----
    private val vocaRepo: LinkedHashMap<Long, VocaDto> = linkedMapOf(
        1L to VocaDto(
            id = 1L,
            word = "apple",
            definition = "사과",
            explanation = "과일의 한 종류로 백설공주가 좋아한다.",
            example = "This is an apple.",
            exampleKor = "이것은 사과이다.",
            skillName = "apple rain",
            skillDef = "사과의 비를 내려 공격한다.",
            skillImg = "iVBORw0KGgoAf...",
            skillDmg = 50,
            widgetType = 1
        ),
        2L to VocaDto(
            id = 2L,
            word = "banana",
            definition = "바나나",
            explanation = "과일의 한 종류로 원숭이가 좋아한다. 우끼끼!",
            example = "This is a banana.",
            exampleKor = "이것은 바나나이다.",
            skillName = "banana rain",
            skillDef = "바나나의 비를 내려 공격한다.",
            skillImg = "iVBORw0KGgoAAf...",
            skillDmg = 70,
            widgetType = 2
        )
    )

    private val collectedIds = CopyOnWriteArraySet<Long>()

    /**
     * 그날 단어 목록 조회
     * GET /test/daily-voca-list?num=10
     */
    @GetMapping("/daily-voca-list")
    fun getDailyVocaList(
        @RequestParam(name = "num", required = false) num: Int?
    ): ResponseEntity<Map<String, Any>> {
        val limit = if (num == null || num <= 0) vocaRepo.size else num

        val vocaList = vocaRepo.values
            .asSequence()
            .take(limit)
            .toList()

        val res = linkedMapOf<String, Any>(
            "vocaList" to vocaList
        )
        return ResponseEntity.ok(res)
    }

    /**
     * 단어 수집 알림
     * POST /test/collect
     * body: {"id": 1}
     */
    @PostMapping("/collect")
    fun collect(@RequestBody req: CollectRequest?): ResponseEntity<Map<String, Any>> {
        val id = req?.id
            ?: return ResponseEntity.badRequest().body(
                mapOf("ok" to false, "message" to "id is required")
            )

        if (!vocaRepo.containsKey(id)) {
            return ResponseEntity.badRequest().body(
                mapOf("ok" to false, "message" to "unknown voca id: $id")
            )
        }

        collectedIds.add(id)

        return ResponseEntity.ok(
            mapOf("ok" to true, "collectedId" to id)
        )
    }

    /**
     * 수집한 단어 목록 id 조회
     * GET /test/collected-voca-list
     * 응답 예: {"vocaIdList":[...], "vocaInfoURL":"/test/voca-info"}
     */
    @GetMapping("/collected-voca-list")
    fun getCollectedVocaIdList(): ResponseEntity<Map<String, Any>> {
        val vocaIdList = collectedIds
            .toList()
            .sortedDescending()

        val res = linkedMapOf<String, Any>(
            "vocaIdList" to vocaIdList,
            "vocaInfoURL" to "/test/voca-info"
        )
        return ResponseEntity.ok(res)
    }

    /**
     * 단일 단어 정보 조회
     * GET /test/voca-info?id=1
     */
    @GetMapping("/voca-info")
    fun getVocaInfo(@RequestParam("id") id: Long): ResponseEntity<Any> {
        val voca = vocaRepo[id] ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(voca)
    }

    // ---- DTOs ----

    data class CollectRequest(
        val id: Long?
    )

    data class VocaDto(
        val id: Long,
        val word: String,
        val definition: String,
        val explanation: String,
        val example: String,
        val exampleKor: String,
        val skillName: String,
        val skillDef: String,
        val skillImg: String,
        val skillDmg: Int,
        val widgetType: Int
    )
}
