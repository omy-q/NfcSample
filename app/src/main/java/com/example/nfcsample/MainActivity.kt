package com.example.nfcsample

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var tagTextView: TextView
    private lateinit var tagToDecimalTextView: TextView
    private lateinit var tagToHexStringTextView: TextView

    private val techList = arrayOf(
        arrayOf(
            NfcA::class.java.name,
            MifareClassic::class.java.name,
            MifareUltralight::class.java.name
        )
    )
    private val filters = arrayOf(
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
    )

    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tagTextView = findViewById(R.id.tag)
        tagToDecimalTextView = findViewById(R.id.tagToDecimal)
        tagToHexStringTextView = findViewById(R.id.tagToHexString)

        val intent = Intent(this, this.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0

        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)


    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(
            this,
            pendingIntent,
            filters,
            techList
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag.let {
            tagTextView.text = "TAG: ${tag?.id}"
            tagToDecimalTextView.text = "TAG to Decimal: ${tag?.id?.toDecimal()}"
            tagToHexStringTextView.text = "TAG to HexString: ${tag?.id?.toHexString()}"

            println("TAG: ${tag?.id}")
            println("TAG to Decimal: ${tag?.id?.toDecimal()}")
            println("TAG to HexString: ${tag?.id?.toHexString()}")
//            val mifareClassic = MifareClassic.get(tag)
//            readTag(mifareClassic)
        }
    }


    private fun readTag(mifareClassic: MifareClassic) {
        try {
            mifareClassic.connect()
            for (sectorIndex in 0 until mifareClassic.sectorCount) {
                // A7 3F 5D C1 D3 33
                val key = byteArrayOf(167.toByte(), 63, 93, 193.toByte(), 211.toByte(), 51)
                val isAuthenticated = mifareClassic.authenticateSectorWithKeyA(sectorIndex, key)
                println("IS_AUTHENTICATED: ${sectorIndex} isAuthenticated = $isAuthenticated")
                if (isAuthenticated) {
                    val blockCount = mifareClassic.getBlockCountInSector(sectorIndex)
                    for (block in 0 until blockCount) {
                        val blockData =
                            mifareClassic.readBlock(sectorIndex * blockCount + block)
                        println("BLOCK DATA HEX: Data in $sectorIndex sector in $block - ${blockData.toHexString()}")
                        println("BLOCK DATA DECIMAL: Data in $sectorIndex sector in $block - ${blockData.toDecimal()}")
                    }
                }
            }

        } catch (e: TagLostException) {
            println("EXCEPTION $e")
        } catch (e: IOException) {
            println("EXCEPTION $e")
        }
    }

    private fun writeToTroyka(tag: MifareClassic, key: ByteArray, sector: Int, data: ByteArray) {
        try {
            tag.connect()
            var isAuthenticated = tag.authenticateSectorWithKeyA(sector, key)
            if (!isAuthenticated) {
                isAuthenticated = tag.authenticateSectorWithKeyB(sector, key)
            }

            if (isAuthenticated) {
                val blockCount = tag.getBlockCountInSector(sector)
                val blocksToWrite = data.size / 16
            }
        } catch (e: TagLostException) {
            println("EXCEPTION $e")
        } catch (e: IOException) {
            println("EXCEPTION $e")
        }
    }
}

fun ByteArray.toDecimal(): Long {
    var result = 0L
    var factor = 1L
    for (i in this.indices) {
        val value = this[i].toLong() and 255L
        result += value * factor
        factor *= 256L
    }
    return result
}

fun ByteArray.toHexString(): String {
    return map { byte ->
        val value = byte.toInt() and 255
        val first = value and 15
        val second = (value and (15 shl 4)) shr 4
        Integer.toHexString(second) + Integer.toHexString(first)
    }
        .reduce { acc, s ->
            "$acc $s"
        }
}
