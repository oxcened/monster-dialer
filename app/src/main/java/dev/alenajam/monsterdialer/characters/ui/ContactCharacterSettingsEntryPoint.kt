package dev.alenajam.monsterdialer.characters.ui

enum class ContactCharacterSettingsEntryPoint(val payload: String?) {
    Toolbox(null),
    ContactList("contact-list");

    companion object {
        fun fromPayload(payload: String?): ContactCharacterSettingsEntryPoint =
            entries.firstOrNull { it.payload == payload } ?: Toolbox
    }
}
