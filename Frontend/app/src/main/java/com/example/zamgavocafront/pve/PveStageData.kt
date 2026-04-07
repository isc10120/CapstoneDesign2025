package com.example.zamgavocafront.pve

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PVE 스테이지 / 몬스터 하드코딩 데이터
//  밸런스 조정 시 이 파일만 수정하면 됩니다.
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

enum class MonsterStatusAttack { NONE, POISON, PARALYSIS }

data class MonsterTemplate(
    val id: Int,
    val name: String,
    val emoji: String,          // 캐릭터 표시용 이모지
    val maxHp: Int,
    val attackEveryNTurns: Int, // 플레이어 N턴마다 1회 공격
    val baseDamage: Int,        // 공격 시 플레이어에게 주는 기본 데미지
    val isBoss: Boolean = false,
    val statusAttack: MonsterStatusAttack = MonsterStatusAttack.NONE
)

data class RoundTemplate(val monsters: List<MonsterTemplate>)

data class StageTemplate(
    val id: Int,
    val name: String,
    val emoji: String,
    val rounds: List<RoundTemplate>  // 인덱스 0~3 잡몹, 4 보스
)

object PveStageData {

    // ── 잡몹 ───────────────────────────────────────────────────────
    private val goblin    = MonsterTemplate(1, "고블린",   "👺", maxHp = 80,   attackEveryNTurns = 3, baseDamage = 20)
    private val wolf      = MonsterTemplate(2, "늑대",     "🐺", maxHp = 120,  attackEveryNTurns = 2, baseDamage = 30)
    private val troll     = MonsterTemplate(3, "트롤",     "👾", maxHp = 180,  attackEveryNTurns = 4, baseDamage = 45)
    private val harpy     = MonsterTemplate(4, "하피",     "🦅", maxHp = 100,  attackEveryNTurns = 3, baseDamage = 25)
    private val jellyfish = MonsterTemplate(5, "해파리",   "🪼", maxHp = 90,   attackEveryNTurns = 2, baseDamage = 15, statusAttack = MonsterStatusAttack.POISON)
    private val shark     = MonsterTemplate(6, "상어",     "🦈", maxHp = 150,  attackEveryNTurns = 3, baseDamage = 38)
    private val merman    = MonsterTemplate(7, "인어전사", "🧜", maxHp = 110,  attackEveryNTurns = 2, baseDamage = 28)
    private val eagle     = MonsterTemplate(8, "독수리",   "🦆", maxHp = 95,   attackEveryNTurns = 2, baseDamage = 22)
    private val snowBear  = MonsterTemplate(9, "설곰",     "🐻", maxHp = 140,  attackEveryNTurns = 3, baseDamage = 35)
    private val iceWolf   = MonsterTemplate(10,"빙설늑대", "🐺", maxHp = 120,  attackEveryNTurns = 2, baseDamage = 28, statusAttack = MonsterStatusAttack.PARALYSIS)

    // ── 보스 ────────────────────────────────────────────────────────
    private val dragonBoss = MonsterTemplate(
        101, "드래곤 군주", "🐉", maxHp = 1200, attackEveryNTurns = 3, baseDamage = 60,
        isBoss = true, statusAttack = MonsterStatusAttack.POISON
    )
    private val kraken = MonsterTemplate(
        102, "크라켄", "🐙", maxHp = 1300, attackEveryNTurns = 2, baseDamage = 55,
        isBoss = true, statusAttack = MonsterStatusAttack.PARALYSIS
    )
    private val griffin = MonsterTemplate(
        103, "그리핀", "🦁", maxHp = 1100, attackEveryNTurns = 3, baseDamage = 50,
        isBoss = true, statusAttack = MonsterStatusAttack.POISON
    )

    // ── 스테이지 정의 ───────────────────────────────────────────────
    val stages: List<StageTemplate> = listOf(
        StageTemplate(
            id = 1, name = "마법의 숲", emoji = "🌲",
            rounds = listOf(
                RoundTemplate(listOf(goblin)),
                RoundTemplate(listOf(goblin, wolf)),
                RoundTemplate(listOf(wolf, harpy)),
                RoundTemplate(listOf(troll)),
                RoundTemplate(listOf(dragonBoss))
            )
        ),
        StageTemplate(
            id = 2, name = "심해의 미로", emoji = "🌊",
            rounds = listOf(
                RoundTemplate(listOf(jellyfish)),
                RoundTemplate(listOf(jellyfish, merman)),
                RoundTemplate(listOf(shark, jellyfish)),
                RoundTemplate(listOf(shark, merman)),
                RoundTemplate(listOf(kraken))
            )
        ),
        StageTemplate(
            id = 3, name = "고산 설원", emoji = "🏔️",
            rounds = listOf(
                RoundTemplate(listOf(eagle)),
                RoundTemplate(listOf(eagle, iceWolf)),
                RoundTemplate(listOf(snowBear, eagle)),
                RoundTemplate(listOf(snowBear, iceWolf)),
                RoundTemplate(listOf(griffin))
            )
        )
    )
}
