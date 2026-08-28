package dev.alenajam.monsterdialer.contacts.data

data class MonsterContact(
    val name: String,
    val numbers: List<String>,
    val photoUri: String? = null
) {
    companion object {
        fun fromEntity(entity: MonsterContactEntity): MonsterContact =
            MonsterContact(entity.name, entity.numbers, entity.photoUri)
    }
}
