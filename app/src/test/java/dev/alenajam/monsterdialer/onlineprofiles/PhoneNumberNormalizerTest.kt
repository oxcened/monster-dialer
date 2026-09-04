package dev.alenajam.monsterdialer.onlineprofiles

import dev.alenajam.monsterdialer.onlineprofiles.data.LibPhoneNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    private val normalizer = LibPhoneNumberNormalizer(defaultRegion = { "US" })

    @Test fun `formats a local number as E164`() {
        assertEquals("+14155552671", normalizer.toE164OrNull("(415) 555-2671"))
    }

    @Test fun `does not turn arbitrary call labels into lookup keys`() {
        assertNull(normalizer.toE164OrNull("Private number"))
    }
}
