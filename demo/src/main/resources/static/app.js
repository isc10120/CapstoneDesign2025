const $ = (id) => document.getElementById(id);

function showImageFromResponse(data) {
    const img = $("preview");
    if (!img) return;

    // 초기화
    img.removeAttribute("src");

    // base64 있으면 표시
    if (data.imageBase64) {
        img.src = "data:image/png;base64," + data.imageBase64;
        return;
    }

    // 없으면 안내(선택)
    // img.alt = "이미지 생성 실패";
}

$("btn").addEventListener("click", async () => {
    const word = $("word")?.value.trim();
    const meaningKo = $("meaning")?.value.trim();

    const out = $("out");
    out.textContent = "생성 중...";

    // 입력 검증
    if (!word || !meaningKo) {
        out.textContent = "단어와 뜻(한국어)을 모두 입력하세요.";
        return;
    }

    try {
        const res = await fetch("/api/skills/generate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ word, meaningKo })
        });

        const text = await res.text();

        if (!res.ok) {
            out.textContent = `HTTP ${res.status}\n\n${text}`;
            return;
        }

        const data = JSON.parse(text);
        out.textContent = JSON.stringify(data, null, 2);

        showImageFromResponse(data);
    } catch (e) {
        out.textContent = String(e);
    }
});
