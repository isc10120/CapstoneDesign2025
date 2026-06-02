package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.DeckService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/decks")
class DeckController(
    private val deckService: DeckService
) {
    @GetMapping
    fun getDecks(@AuthUser user: User): ApiResponse<List<DeckService.DeckSummary>> {
        return ApiResponse.success(deckService.getDecks(user.id!!))
    }

    @GetMapping("/{deckId}")
    fun getDeck(@AuthUser user: User, @PathVariable deckId: Long): ApiResponse<DeckService.DeckDetail> {
        return ApiResponse.success(deckService.getDeck(user.id!!, deckId))
    }

    @PostMapping
    fun createDeck(@AuthUser user: User, @RequestBody request: DeckRequest): ApiResponse<DeckService.DeckDetail> {
        return ApiResponse.success(deckService.createDeck(user.id!!, request.name, request.skillIds))
    }

    @PutMapping("/{deckId}")
    fun updateDeck(
        @AuthUser user: User,
        @PathVariable deckId: Long,
        @RequestBody request: DeckUpdateRequest
    ): ApiResponse<DeckService.DeckDetail> {
        return ApiResponse.success(deckService.updateDeck(user.id!!, deckId, request.name, request.skillIds))
    }

    @DeleteMapping("/{deckId}")
    fun deleteDeck(@AuthUser user: User, @PathVariable deckId: Long): ApiResponse<Nothing> {
        deckService.deleteDeck(user.id!!, deckId)
        return ApiResponse.success(null)
    }

    data class DeckRequest(val name: String, val skillIds: List<Long>)
    data class DeckUpdateRequest(val name: String?, val skillIds: List<Long>?)
}
