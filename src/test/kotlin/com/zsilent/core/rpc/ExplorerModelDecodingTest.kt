package com.zsilent.core.rpc

import com.zsilent.core.models.BlockSummary
import com.zsilent.core.models.BlockTransactions
import com.zsilent.core.models.MempoolTransaction
import com.zsilent.core.models.PrivateTxLookup
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the explorer models to the JSON the node actually emits.
 *
 * These payloads are copied from the node's own serializers in `src/api/mod.rs`
 * (`block_metadata_json`, `get_block_transactions`, `get_mempool_transaction`),
 * not invented here. The models were written by reading that Rust code, so
 * without this test a renamed field would surface as a silently defaulted zero
 * in the UI instead of a failure.
 *
 * Checked against a live mainnet node at height 12943 on 2026-07-26. That run
 * corrected the reward fixtures: `reward_zka_display` is a bare `"12"`, not the
 * `"12.00000000 ZKA"` this file first assumed, and a block reward is 12_000_000
 * atoms rather than 1_200_000_000. Both decoded either way, so only a real
 * payload could catch it.
 */
class ExplorerModelDecodingTest {
    private val codec = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun block_metadata_from_the_node_decodes_field_for_field() {
        val payload = """
            {
              "hash": "0000abc",
              "height": 1234,
              "timestamp": 1751000000,
              "state_root": "root-xyz",
              "previous_hash": "0000abb",
              "nonce": 987654,
              "difficulty": 21,
              "difficulty_bits": 21,
              "size_bytes": 4096,
              "reward_atoms": 12000000,
              "reward_zka": 12.0,
              "reward_zka_display": "12",
              "transactions_count": 3,
              "private_tx_count": 2,
              "private_nullifier_count": 4,
              "private_coinbase": true
            }
        """.trimIndent()

        val block = codec.decodeFromString<BlockSummary>(payload)

        assertEquals("0000abc", block.hash)
        assertEquals(1234L, block.height)
        assertEquals(1751000000L, block.timestamp)
        assertEquals("root-xyz", block.stateRoot)
        assertEquals("0000abb", block.previousHash)
        assertEquals(987654uL, block.nonce)
        assertEquals(21, block.difficultyBits)
        assertEquals(4096L, block.sizeBytes)
        assertEquals(12000000L, block.rewardAtoms)
        assertEquals("12", block.rewardZkaDisplay)
        assertEquals(3, block.transactionsCount)
        assertEquals(2, block.privateTxCount)
        assertEquals(4, block.privateNullifierCount)
        assertTrue(block.privateCoinbase)
    }

    /**
     * Zoka Quantic sends the nonce as a u64 that does not fit in a Long. With
     * a Long, "Latest blocks" failed on 3/11/2026 with "Numeric value overflow
     * at path: $.nonce". Payload of block 4003 from the testnet v2 node.
     */
    @Test
    fun a_quantic_nonce_above_long_max_decodes() {
        val payload = """
            {"canonical":true,"confirmations":2,"difficulty_bits":16,"fees_atoms":0,
             "hash":"cc177aadb331b45a86254b4ce5b2b0ed8441b393ce50e4c4f78c7a6673fbf443",
             "hashrate_estimate_hps":909,"height":4003,"nonce":17480248669717492693,
             "previous_hash":"308d87b043989fdc0b744f02f5ade29950f6a12d4caa0848fbf1800decaca0b6",
             "private_coinbase":true,"private_nullifier_count":0,"private_tx_count":0,
             "reward_atoms":12000000,"reward_zka_display":"12","size_bytes":2832,
             "timestamp":1793744033,"transactions_count":1}
        """.trimIndent()

        val block = codec.decodeFromString<BlockSummary>(payload)

        assertEquals(4003L, block.height)
        assertEquals("17480248669717492693", block.nonce.toString())
    }

    /** `GET /transactions/<txid>` of the v2 node, pending and then mined (block 4012). */
    @Test
    fun a_v2_payment_lookup_decodes_pending_and_confirmed() {
        val pending = codec.decodeFromString<V2TransactionStatus>(
            """{"txid":"ebfdbf1a75ac1c5a0638f9978cbe641c7979dd202242b37004c2b7d289330554","status":"pending"}"""
        )
        assertEquals("pending", pending.status)
        assertNull(pending.height)

        val mined = codec.decodeFromString<V2TransactionStatus>(
            """{"txid":"ebfdbf1a75ac1c5a0638f9978cbe641c7979dd202242b37004c2b7d289330554",
                "status":"confirmed","height":4012,"confirmations":3,
                "block_hash":"a63fff231ee40935d6ffcac0337ae39dd8f7d0719c194d1c626ad3c573862cba"}"""
        )
        assertEquals("confirmed", mined.status)
        assertEquals(4012L, mined.height)
        assertEquals("a63fff231ee40935d6ffcac0337ae39dd8f7d0719c194d1c626ad3c573862cba", mined.blockHash)
        assertEquals(3L, mined.confirmations)
    }

    /**
     * Genesis is served with `difficulty_bits: null` — the node passes `None`
     * because height 0 has no recorded difficulty. A non-nullable Int would
     * have thrown here and broken the one lookup every user tries first.
     */
    @Test
    fun genesis_block_with_null_difficulty_decodes() {
        val payload = """
            {
              "hash": "genesis",
              "height": 0,
              "timestamp": 1700000000,
              "state_root": "",
              "previous_hash": "",
              "nonce": 0,
              "difficulty": null,
              "difficulty_bits": null,
              "size_bytes": 512,
              "reward_atoms": 0,
              "reward_zka": 0.0,
              "reward_zka_display": "0",
              "transactions_count": 0,
              "private_tx_count": 0,
              "private_nullifier_count": 0,
              "private_coinbase": false
            }
        """.trimIndent()

        val block = codec.decodeFromString<BlockSummary>(payload)

        assertEquals(0L, block.height)
        assertNull(block.difficultyBits)
    }

    @Test
    fun block_transactions_expose_hashes_only() {
        val payload = """
            {
              "block_height": 77,
              "transactions_count": 2,
              "transactions": [{ "hash": "tx-a" }, { "hash": "tx-b" }]
            }
        """.trimIndent()

        val txs = codec.decodeFromString<BlockTransactions>(payload)

        assertEquals(77L, txs.blockHeight)
        assertEquals(2, txs.transactionsCount)
        assertEquals(listOf("tx-a", "tx-b"), txs.transactions.map { it.hash })
    }

    @Test
    fun empty_genesis_transaction_list_decodes() {
        val txs = codec.decodeFromString<BlockTransactions>(
            """{"block_height":0,"transactions_count":0,"transactions":[]}"""
        )

        assertEquals(0, txs.transactionsCount)
        assertTrue(txs.transactions.isEmpty())
    }

    @Test
    fun mempool_transaction_decodes() {
        val tx = codec.decodeFromString<MempoolTransaction>(
            """{"hash":"pending-1","timestamp":1751000123,"status":"pending"}"""
        )

        assertEquals("pending-1", tx.hash)
        assertEquals(1751000123L, tx.timestamp)
        assertEquals("pending", tx.status)
    }

    /**
     * `GET /transactions/:hash` reports a mempool hit as "pending_in_mempool"
     * while `GET /mempool/tx/:hash` says plain "pending" for the same
     * transaction. The explorer must not depend on either spelling.
     */
    @Test
    fun the_two_pending_status_spellings_both_decode() {
        val fromTransactionsEndpoint = codec.decodeFromString<MempoolTransaction>(
            """{"hash":"pending-2","timestamp":1751000200,"status":"pending_in_mempool"}"""
        )

        assertEquals("pending_in_mempool", fromTransactionsEndpoint.status)
    }

    /**
     * The node already sends fields the models ignore — `difficulty` carries
     * the same value as `difficulty_bits`, and `reward_zka` the same amount as
     * `reward_zka_display`. Decoding must keep tolerating additions.
     */
    @Test
    fun unknown_fields_from_a_newer_node_are_ignored() {
        val block = codec.decodeFromString<BlockSummary>(
            """{"hash":"h","height":5,"some_future_field":"whatever","another":[1,2]}"""
        )

        assertEquals("h", block.hash)
        assertEquals(5L, block.height)
    }

    /**
     * `GET /private/tx/:hash` returns the whole private body plus two fields the
     * node computes against the chain (`get_private_tx_by_hash` and
     * `con_estado_de_cadena` in `src/api/mod.rs`). Checked on 2026-10-14
     * against the node at height 25,466: the private transaction of block 14,704
     * came back "Confirmed" with `block_height` 14704, inside a ~350 KB body of
     * proofs and ring members that the explorer skips.
     */
    @Test
    fun a_confirmed_private_tx_reports_the_block_that_holds_it() {
        val tx = codec.decodeFromString<PrivateTxLookup>(
            """
            {
              "tx_hash": "452fb30432b8fb6eb088645529a57829",
              "commitment": "c0ffee",
              "ring_members": ["a", "b", "c"],
              "ring_signature": [1, 2, 3],
              "input_nullifiers": ["n1"],
              "fee": 0,
              "timestamp": 1785765348,
              "status": "Confirmed",
              "block_height": 14704
            }
            """.trimIndent()
        )

        assertEquals("452fb30432b8fb6eb088645529a57829", tx.txHash)
        assertEquals("Confirmed", tx.status)
        assertEquals(14704L, tx.blockHeight)
        assertEquals(1785765348L, tx.timestamp)
    }

    /** Pending, Spent and Unknown carry no block: the model must say so, not 0. */
    @Test
    fun a_private_tx_without_a_block_keeps_the_height_null() {
        val tx = codec.decodeFromString<PrivateTxLookup>(
            """{"tx_hash":"6cbcf5fde38cc24d32408f385696e692","timestamp":1791989000,"status":"Pending"}"""
        )

        assertEquals("Pending", tx.status)
        assertNull(tx.blockHeight)
    }
}
