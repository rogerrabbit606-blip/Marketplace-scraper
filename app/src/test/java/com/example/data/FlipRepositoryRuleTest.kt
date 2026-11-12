package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AcquisitionType
import com.example.model.DealMode
import com.example.model.FlipItem
import com.example.model.FlipStatus
import com.example.model.SavedScout
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveScout_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FlipRepository(firestore)

        val scout = SavedScout(
            title = "Free Patio Furniture",
            keywords = "patio chair",
            category = "Patio & Yard",
            dealMode = DealMode.FREE_ONLY.wireValue,
            maxPrice = 0L,
            citySlug = "sfbay",
            cityName = "SF Bay Area",
            radiusMiles = 20L,
            targetResale = 120L
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createScout(scout) }
        assertTrue(createResult.isSuccess)
        val scoutId = createResult.getOrThrow()
        assertTrue(scoutId.isNotBlank())

        val scouts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeScouts(aliceUid).first { list -> list.any { it.id == scoutId } }
        }
        val saved = scouts.first { it.id == scoutId }
        assertEquals("Free Patio Furniture", saved.title)
        assertNotNull(saved.createdAt)
    }

    @Test
    fun createAndTransitionFlip_authenticatedOwner_locksAfterSold() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FlipRepository(firestore)

        val flip = FlipItem(
            title = "DeWalt 20V Brushless Drill",
            sourcePlatform = "OfferUp",
            category = "Power Tools",
            acquisitionType = AcquisitionType.CASH.wireValue,
            buyPriceCents = 4500L,
            repairCostCents = 0L,
            estResaleCents = 11000L,
            status = FlipStatus.SCOUTING.wireValue,
            listingUrl = "https://offerup.com",
            notes = "Includes 2 batteries"
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createFlip(flip) }
        assertTrue(createResult.isSuccess)
        val flipId = createResult.getOrThrow()

        val markSoldResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateFlipStatus(flipId, FlipStatus.SOLD, soldPriceCents = 11500L)
        }
        assertTrue(markSoldResult.isSuccess)

        val afterSoldUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateFlipStatus(flipId, FlipStatus.LISTED)
        }
        assertTrue(afterSoldUpdate.isFailure)
        val firestoreEx = afterSoldUpdate.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)

        val flips = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFlips(aliceUid).first { list -> list.any { it.id == flipId && it.status == "SOLD" } }
        }
        assertEquals(11500L, flips.first { it.id == flipId }.soldPriceCents)
    }

    @Test
    fun getFlipById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FlipRepository(firestore)
        val flipId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createFlip(
                FlipItem(
                    title = "Free Oak Bookshelf",
                    acquisitionType = AcquisitionType.FREE.wireValue,
                    buyPriceCents = 0L,
                    estResaleCents = 9000L
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FlipRepository(firestore)
        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFlipById(aliceUid, flipId)
        }
        assertTrue(readResult.isFailure)
        val firestoreEx = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeFlips_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = FlipRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFlips(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_flip@test.com"
        const val BOB_EMAIL = "bob_flip@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
