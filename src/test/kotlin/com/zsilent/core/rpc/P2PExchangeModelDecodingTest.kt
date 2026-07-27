package com.zsilent.core.rpc

import com.zsilent.core.models.ClaimOfferResponse
import com.zsilent.core.models.OfferList
import com.zsilent.core.models.PaymentFailureSummary
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class P2PExchangeModelDecodingTest {
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun offer_feed_includes_node_verification_and_trade_state() {
        val feed = codec.decodeFromString<OfferList>(
            """
                {
                  "offers": [{
                    "id": "offer-1",
                    "seller_address": "zka1seller",
                    "zka_atoms": 1250000,
                    "micro_usdt": 2500000,
                    "usdt_receive_address": "T111111111111111111111111111111111",
                    "status": "Claimed",
                    "created_at": 1751000000,
                    "claim_expires_at": 1751002700,
                    "claimed_by": "zka1buyer"
                  }],
                  "verification": "checking offer offer-1"
                }
            """.trimIndent()
        )

        assertEquals("checking offer offer-1", feed.verification)
        assertEquals(1, feed.offers.size)
        assertEquals(1_250_000L, feed.offers.single().zkaAtoms)
        assertEquals("zka1buyer", feed.offers.single().claimedBy)
    }

    @Test
    fun claim_response_stays_a_request_until_seller_acknowledges() {
        val response = codec.decodeFromString<ClaimOfferResponse>(
            """
                {
                  "offer_id": "offer-1",
                  "seller_address": "zka1seller",
                  "status": "claim_requested"
                }
            """.trimIndent()
        )

        assertEquals("offer-1", response.offerId)
        assertEquals("zka1seller", response.sellerAddress)
        assertEquals("claim_requested", response.status)
    }

    @Test
    fun payment_failure_response_preserves_public_tron_evidence() {
        val report = codec.decodeFromString<PaymentFailureSummary>(
            """
                {
                  "offer_id": "offer-1",
                  "seller_address": "zka1seller",
                  "buyer_address": "zka1buyer",
                  "tron_tx_hash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "timestamp": 1751003000,
                  "signature": []
                }
            """.trimIndent()
        )

        assertEquals("zka1buyer", report.buyerAddress)
        assertEquals(64, report.tronTxHash.length)
        assertEquals(1_751_003_000L, report.timestamp)
    }
}
