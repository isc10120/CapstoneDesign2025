package jamgaVOCA.demo.domain.word

enum class PartOfSpeech (
    val alias: String
){
    NOUN("n."),
    VERB("v."),
    ADJECTIVE("adj."),
    ADVERB("adv.");
    companion object {
        fun from(value: String): PartOfSpeech =
            entries.find {
                it.name.equals(value, true) || it.alias.equals(value, true)
            } ?: throw IllegalArgumentException("Invalid value: $value")
    }
}