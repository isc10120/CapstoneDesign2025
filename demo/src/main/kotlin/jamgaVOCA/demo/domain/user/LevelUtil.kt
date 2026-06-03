package jamgaVOCA.demo.domain.user

object LevelUtil {
    // 인덱스 = 레벨-1, 값 = 해당 레벨 도달에 필요한 누적 경험치
    private val EXP_TABLE = listOf(0, 50, 100, 150, 200, 300, 400, 500, 600, 700, 850, 1050, 1300, 1600, 1950, 2400, 2950, 3600, 4400, 5400)

    fun calcLevel(exp: Int): Int {
        if (exp >= EXP_TABLE.last()) {
            return 20 + (exp - EXP_TABLE.last()) / 1000
        }
        return EXP_TABLE.indexOfLast { exp >= it } + 1
    }

    fun maxDeckSize(level: Int): Int = if (level <= 10) 10 else minOf(level, 20)

    fun minExpForLevel(level: Int): Int = when {
        level <= 1 -> 0
        level <= 20 -> EXP_TABLE[level - 1]
        else -> EXP_TABLE.last() + (level - 20) * 1000
    }
}
