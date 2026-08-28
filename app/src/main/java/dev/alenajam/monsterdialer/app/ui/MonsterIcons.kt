package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddIcCall
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhonePaused
import androidx.compose.material.icons.outlined.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.AppIcons
import dev.alenajam.opendialer.core.common.ui.IconSource
import dev.alenajam.opendialer.core.common.ui.rememberAppIsDarkTheme

@Composable
fun rememberMonsterIcons(): AppIcons {
    val isDarkTheme = rememberAppIsDarkTheme()
    return remember(isDarkTheme) {
        AppIcons(
            hangup = IconSource.Vector(Icons.Outlined.CallEnd),
            dialpad = IconSource.Resource(
                if (isDarkTheme) R.drawable.dial_on_dark else R.drawable.dial_on_light,
                tintable = false,
            ),
            mute = IconSource.Vector(Icons.Outlined.MicOff),
            speaker = IconSource.Vector(Icons.AutoMirrored.Outlined.VolumeUp),
            more = IconSource.Vector(Icons.Outlined.MoreVert),
            pause = IconSource.Vector(Icons.Outlined.Pause),
            addCall = IconSource.Vector(Icons.Outlined.AddIcCall),
            accountCircle = IconSource.Resource(R.drawable.player, tintable = false),
            callReceived = IconSource.Resource(R.drawable.incoming, tintable = false),
            callMade = IconSource.Vector(Icons.AutoMirrored.Outlined.CallMade),
            callMissed = IconSource.Resource(R.drawable.missed, tintable = false),
            voicemail = IconSource.Resource(R.drawable.voicemail, tintable = false),
            voicemailSelected = IconSource.Resource(R.drawable.voicemail, tintable = false),
            block = IconSource.Resource(R.drawable.block, tintable = false),
            blockCall = IconSource.Resource(R.drawable.block_call, tintable = false),
            phone = IconSource.Resource(R.drawable.phone, tintable = false),
            message = IconSource.Resource(R.drawable.message, tintable = false),
            personAdd = IconSource.Resource(
                if (isDarkTheme) R.drawable.add_contact_on_dark else R.drawable.add_contact_on_light,
                tintable = false,
            ),
            personAddInContactsList = IconSource.Resource(
                if (isDarkTheme) R.drawable.add_contact_on_light else R.drawable.add_contact_on_dark,
                tintable = false,
            ),
            history = IconSource.Resource(R.drawable.book, tintable = false),
            merge = IconSource.Vector(Icons.Outlined.Merge),
            swapCalls = IconSource.Vector(Icons.Outlined.SwapCalls),
            phonePaused = IconSource.Vector(Icons.Outlined.PhonePaused),
            recents = IconSource.Resource(R.drawable.clock, tintable = false),
            recentsSelected = IconSource.Resource(R.drawable.clock, tintable = false),
            contacts = IconSource.Resource(R.drawable.player, tintable = false),
            contactsSelected = IconSource.Resource(R.drawable.player, tintable = false),
            voicemailLarge = IconSource.Resource(R.drawable.voicemail, tintable = false),
            search = IconSource.Resource(R.drawable.search, tintable = false),
            close = IconSource.Resource(R.drawable.close, tintable = false),
            share = IconSource.Resource(R.drawable.share, tintable = false),
            edit = IconSource.Resource(R.drawable.edit, tintable = false),
            copy = IconSource.Resource(R.drawable.copy, tintable = false),
            delete = IconSource.Resource(
                if (isDarkTheme) R.drawable.delete_on_dark else R.drawable.delete_on_light,
                tintable = false,
            ),
            arrowLeft = IconSource.Resource(R.drawable.arrow_left, tintable = false),
            arrowRight = IconSource.Resource(R.drawable.arrow_right, tintable = false),
            arrowUp = IconSource.Resource(R.drawable.arrow_up, tintable = false),
            arrowDown = IconSource.Resource(R.drawable.arrow_down, tintable = false),
            favorite = IconSource.Resource(R.drawable.star, tintable = false),
        )
    }
}
