const $ = (id) => document.getElementById(id);

const state = {
    targetWord: "",
    userLevel: "intermediate",
    koreanSentence: "",
    wordHint: "",
    idealTranslation: ""
};

function setDimText(el, text) {
    el.textContent = text;
    el.classList.toggle("dim", !text || text === "-");
}

function prettyJson(obj) {
    return JSON.stringify(obj, null, 2);
}

function fillBar(idFill, idVal, v) {
    const fill = $(idFill);
    const val = $(idVal);
    const n = Number(v ?? 0);
    fill.style.width = Math.max(0, Math.min(100, n)) + "%";
    val.textContent = String(n);
}

function resetAll() {
    state.targetWord = "";
    state.userLevel = "intermediate";
    state.koreanSentence = "";
    state.wordHint = "";
    state.idealTranslation = "";

    $("targetWord").value = "";
    $("userLevel").value = "intermediate";
    $("userAnswer").value = "";

    setDimText($("koreanSentence"), "-");
    setDimText($("wordHint"), "-");
    $("idealTranslation").textContent = "";
    $("questionRaw").textContent = "";
    $("evalRaw").textContent = "";
    $("scoreCard").style.display = "none";

    $("levelPill").textContent = "level: -";
    $("wordPill").textContent = "word: -";
}

async function postJson(url, body) {
    const res = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    });

    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { /* ignore */ }

    if (!res.ok) {
        // 기능 확인용: 원문 그대로 노출
        throw new Error(`HTTP ${res.status}\n\n${text}`);
    }
    if (!data) throw new Error("서버 응답 JSON 파싱 실패\n\n" + text);
    return data;
}

$("btnQuestion").addEventListener("click", async () => {
    const targetWord = $("targetWord").value.trim();
    const userLevel = $("userLevel").value;

    $("questionRaw").textContent = "생성 중...";
    $("evalRaw").textContent = "";
    $("scoreCard").style.display = "none";

    if (!targetWord) {
        $("questionRaw").textContent = "학습 단어를 입력하세요.";
        return;
    }

    try {
        const data = await postJson("/api/translation/question", { targetWord, userLevel });
        $("questionRaw").textContent = prettyJson(data);

        if (!data.success) return;

        state.targetWord = data.targetWord || targetWord;
        state.userLevel = userLevel;
        state.koreanSentence = data.koreanSentence || "";
        state.wordHint = data.wordHint || "";
        state.idealTranslation = data.ideal || "";

        setDimText($("koreanSentence"), state.koreanSentence || "-");
        setDimText($("wordHint"), state.wordHint || "-");
        $("idealTranslation").textContent = state.idealTranslation || "";

        $("levelPill").textContent = `level: ${state.userLevel}`;
        $("wordPill").textContent = `word: ${state.targetWord}`;

    } catch (e) {
        $("questionRaw").textContent = String(e);
    }
});

$("btnEvaluate").addEventListener("click", async () => {
    $("evalRaw").textContent = "평가 중...";
    $("scoreCard").style.display = "none";

    const userAnswer = $("userAnswer").value.trim();
    if (!state.koreanSentence || !state.idealTranslation || !state.targetWord) {
        $("evalRaw").textContent = "먼저 '문제 만들기'를 실행해서 korean_sentence / ideal을 확보하세요.";
        return;
    }
    if (!userAnswer) {
        $("evalRaw").textContent = "사용자 번역(영어)을 입력하세요.";
        return;
    }

    try {
        const data = await postJson("/api/translation/evaluate", {
            koreanSentence: state.koreanSentence,
            userAnswer,
            idealTranslation: state.idealTranslation,
            targetWord: state.targetWord,
            userLevel: state.userLevel
        });

        $("evalRaw").textContent = prettyJson(data);

        if (!data.success) return;

        $("scoreCard").style.display = "block";
        $("score").textContent = String(data.score ?? "-");

        const b = data.breakdown || {};
        fillBar("barMeaning", "valMeaning", b.meaning);
        fillBar("barGrammar", "valGrammar", b.grammar);
        fillBar("barWordUsage", "valWordUsage", b.word_usage);
        fillBar("barNaturalness", "valNaturalness", b.naturalness);

        $("feedback").textContent = data.feedback ?? "";
        setDimText($("correction"), (data.correction === null || data.correction === undefined) ? "null" : String(data.correction));
        setDimText($("idealAnswer2"), data.idealAnswer ?? "-");

        $("levelPill").textContent = `level: ${state.userLevel}`;
        $("wordPill").textContent = `word: ${state.targetWord}`;

    } catch (e) {
        $("evalRaw").textContent = String(e);
    }
});

$("btnReset").addEventListener("click", resetAll);

// 초기화
resetAll();
