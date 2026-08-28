package dev.alenajam.monsterdialer.contacts.data

data class MonsterContact(
    val name: String,
    val numbers: List<String>
) {
    companion object {
        fun fromEntity(entity: MonsterContactEntity): MonsterContact =
            MonsterContact(entity.name, entity.numbers)
    }
}
