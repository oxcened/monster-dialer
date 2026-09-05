package dev.alenajam.monsterdialer.characters.ui

enum class ContactCharacterSettingsEntryPoint(val payload: String?) {
    Toolbox(null),
    Defaults("contact-defaults"),
    ContactList("contact-list");

    companion object {
        fun fromPayload(payload: String?): ContactCharacterSettingsEntryPoint =
            entries.firstOrNull { it.payload == payload } ?: Toolbox
    }
}
