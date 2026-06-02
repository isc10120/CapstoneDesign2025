package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RankingService(
    private val userRepository: UserRepository
) {
    fun getExpRanking(userId: Long, page: Int, size: Int): RankingResponse {
        val pageable = PageRequest.of(page, size)
        val result = userRepository.findAllByIsDummyFalseOrderByExpPointDescLevelDesc(pageable)
        val entries = result.content.mapIndexed { idx, user ->
            RankingEntry(
                rank = page * size + idx + 1,
                userId = user.id!!,
                nickname = user.nickname,
                level = user.level,
                value = user.expPoint.toLong()
            )
        }
        val me = userRepository.findById(userId).orElseThrow()
        val myRank = userRepository.findExpRankByExpPoint(me.expPoint)
        return RankingResponse(
            entries = entries,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            myEntry = RankingEntry(myRank.toInt(), me.id!!, me.nickname, me.level, me.expPoint.toLong())
        )
    }

    fun getSkillCountRanking(userId: Long, page: Int, size: Int): RankingResponse {
        val pageable = PageRequest.of(page, size)
        val result = userRepository.findAllOrderBySkillCountDesc(pageable)
        val entries = result.content.mapIndexed { idx, user ->
            RankingEntry(
                rank = page * size + idx + 1,
                userId = user.id!!,
                nickname = user.nickname,
                level = user.level,
                value = userRepository.countSkillsByUserId(user.id!!)
            )
        }
        val myRank = userRepository.findSkillRankByUserId(userId)
        val mySkillCount = userRepository.countSkillsByUserId(userId)
        val me = userRepository.findById(userId).orElseThrow()
        return RankingResponse(
            entries = entries,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            myEntry = RankingEntry(myRank.toInt(), me.id!!, me.nickname, me.level, mySkillCount)
        )
    }

    data class RankingEntry(val rank: Int, val userId: Long, val nickname: String, val level: Int, val value: Long)
    data class RankingResponse(
        val entries: List<RankingEntry>,
        val totalElements: Long,
        val totalPages: Int,
        val myEntry: RankingEntry
    )
}
