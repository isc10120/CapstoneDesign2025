package com.example.zamgavocafront

import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.model.WordData

object WordRepository {

    val allWords = mutableListOf(
        WordData(
            id = 1,
            word = "ephemeral",
            meaning = "일시적인, 단명하는",
            exampleEn = "Fame in the digital age can be ephemeral.",
            exampleKr = "디지털 시대의 명성은 일시적일 수 있다.",
            difficulty = Difficulty.MEDIUM
        ),
        WordData(
            id = 2,
            word = "serendipity",
            meaning = "뜻밖의 행운",
            exampleEn = "Meeting her was pure serendipity.",
            exampleKr = "그녀를 만난 건 완전 뜻밖의 행운이었다.",
            difficulty = Difficulty.HARD
        ),
        WordData(
            id = 3,
            word = "eloquent",
            meaning = "유창한, 설득력 있는",
            exampleEn = "He gave an eloquent speech.",
            exampleKr = "그는 설득력 있는 연설을 했다.",
            difficulty = Difficulty.MEDIUM
        ),
        WordData(
            id = 4,
            word = "benevolent",
            meaning = "자비로운, 친절한",
            exampleEn = "She has a benevolent personality.",
            exampleKr = "그녀는 자비로운 성격을 가지고 있다.",
            difficulty = Difficulty.EASY
        ),
        WordData(
            id = 5,
            word = "melancholy",
            meaning = "우울, 침울함",
            exampleEn = "He felt a sense of melancholy.",
            exampleKr = "그는 우울함을 느꼈다.",
            difficulty = Difficulty.MEDIUM
        ),
        WordData(
            id = 6,
            word = "perseverance",
            meaning = "인내, 불굴의 정신",
            exampleEn = "Success requires perseverance.",
            exampleKr = "성공에는 인내가 필요하다.",
            difficulty = Difficulty.EASY
        ),
        WordData(
            id = 7,
            word = "ambiguous",
            meaning = "애매한, 불명확한",
            exampleEn = "His answer was ambiguous.",
            exampleKr = "그의 대답은 애매했다.",
            difficulty = Difficulty.MEDIUM
        ),
        WordData(
            id = 8,
            word = "tenacious",
            meaning = "끈질긴, 완강한",
            exampleEn = "She is a tenacious worker.",
            exampleKr = "그녀는 끈질긴 일꾼이다.",
            difficulty = Difficulty.HARD
        ),
        WordData(
            id = 9,
            word = "profound",
            meaning = "심오한, 깊은",
            exampleEn = "He made a profound statement.",
            exampleKr = "그는 심오한 말을 했다.",
            difficulty = Difficulty.HARD
        ),
        WordData(
            id = 10,
            word = "candid",
            meaning = "솔직한, 숨김없는",
            exampleEn = "She gave a candid opinion.",
            exampleKr = "그녀는 솔직한 의견을 말했다.",
            difficulty = Difficulty.EASY
        )
    )
}
