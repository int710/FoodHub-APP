package com.example.foodhubapp

import com.example.foodhubapp.feature.customer.table.data.extractQrToken
import org.junit.Assert.assertEquals
import org.junit.Test

class TableQrTokenTest {
    @Test fun extractsTokenFromBackendQrUrl() {
        assertEquals(
            "qr-token-value",
            extractQrToken("https://foodhub.example/scan?token=qr-token-value")
        )
    }

    @Test fun extractsEncodedTokenAndJsonPayload() {
        assertEquals("token/with+chars", extractQrToken("foodhub://table/scan?qrToken=token%2Fwith%2Bchars"))
        assertEquals("json-token", extractQrToken("{\"qrToken\":\"json-token\"}"))
    }

    @Test fun acceptsRawToken() {
        assertEquals("raw-table-token", extractQrToken("  raw-table-token  "))
    }
}
