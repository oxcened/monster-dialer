package dev.alenajam.monsterdialer.onlineprofiles.ui

import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.ui.GuideContent

/** Help shown when a player receives and links someone else's Online Profile. */
internal fun sharedOnlineProfileGuideContents(): List<GuideContent> = listOf(
    GuideContent(
        R.string.shared_online_profile_guide_title,
        R.string.shared_online_profile_guide_message,
    ),
    GuideContent(
        R.string.shared_online_profile_choose_contact_guide_title,
        R.string.shared_online_profile_choose_contact_guide_message,
    ),
    GuideContent(
        R.string.shared_online_profile_loading_guide_title,
        R.string.shared_online_profile_loading_guide_message,
    ),
    GuideContent(
        R.string.shared_online_profile_cache_guide_title,
        R.string.shared_online_profile_cache_guide_message,
    ),
    GuideContent(
        R.string.shared_online_profile_unlink_guide_title,
        R.string.shared_online_profile_unlink_guide_message,
    ),
)

/** Help shown for the profile a player publishes themselves. */
internal fun ownedOnlineProfileGuideContents(): List<GuideContent> = listOf(
    GuideContent(R.string.online_profile_guide_title, R.string.online_profile_guide_message),
    GuideContent(R.string.online_profile_public_id_title, R.string.online_profile_public_id_message),
    GuideContent(R.string.online_profile_delete_guide_title, R.string.online_profile_delete_guide_message),
)
