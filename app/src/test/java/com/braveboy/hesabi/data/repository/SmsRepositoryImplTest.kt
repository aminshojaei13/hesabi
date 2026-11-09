package com.braveboy.hesabi.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.SmsAcceptInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SmsRepositoryImplTest {

    private lateinit var db: HesabiDatabase
    private lateinit var repository: SmsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HesabiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SmsRepositoryImpl(
            db = db,
            smsDao = db.smsDao(),
            senderRuleDao = db.senderRuleDao(),
            transactionDao = db.transactionDao(),
            settingsRepository = SettingsRepositoryImpl(context),
        )
        kotlinx.coroutines.runBlocking {
            db.accountDao().upsertAll(
                listOf(
                    account("acc_a", "مبدأ"),
                    account("acc_b", "مقصد"),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sms transfer subtracts from source and credits destination`() = runTest {
        val accepted = repository.acceptAll(
            listOf(
                transfer(
                    eventId = "sms_transfer",
                    from = "acc_a",
                    to = "acc_b",
                ),
            ),
        )

        assertEquals(1, accepted)
        val balances = AccountRepositoryImpl(db.accountDao(), db.transactionDao())
            .observeWithBalances()
            .first()
            .associate { it.account.id to it.balanceToman }
        assertEquals(-100_000L, balances["acc_a"])
        assertEquals(100_000L, balances["acc_b"])
    }

    @Test
    fun `sms transfer without a destination is rejected`() = runTest {
        val accepted = repository.acceptAll(
            listOf(transfer(eventId = "sms_missing", from = "acc_a", to = null)),
        )

        assertEquals(0, accepted)
        assertEquals(0, db.transactionDao().count())
    }

    @Test
    fun `sms transfer to the same account is rejected`() = runTest {
        val accepted = repository.acceptAll(
            listOf(transfer(eventId = "sms_same", from = "acc_a", to = "acc_a")),
        )

        assertEquals(0, accepted)
        assertEquals(0, db.transactionDao().count())
    }

    private fun transfer(eventId: String, from: String, to: String?) = SmsAcceptInput(
        eventId = eventId,
        type = TransactionType.TRANSFER,
        amountToman = 100_000L,
        accountId = from,
        toAccountId = to,
        categoryId = null,
        occurredOnEpochDay = JalaliDate(1405, 8, 1).toEpochDay(),
        note = "انتقال آزمایشی",
    )

    private fun account(id: String, name: String) = AccountEntity(
        id = id,
        name = name,
        kind = "BANK",
        colorArgb = 0xFF2563EB,
        openingBalanceToman = 0L,
        archived = false,
        sortOrder = 0,
    )
}
