package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AcquisitionType
import com.example.model.DealMode
import com.example.model.FlipItem
import com.example.model.FlipStatus
import com.example.model.SavedScout
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FlipRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeScouts(userId: String): Flow<List<SavedScout>> {
        val path = "users/$userId/scouts"
        return db.collection("users")
            .document(userId)
            .collection("scouts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavedScout::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeFlips(userId: String): Flow<List<FlipItem>> {
        val path = "users/$userId/flips"
        return db.collection("users")
            .document(userId)
            .collection("flips")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FlipItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createScout(scout: SavedScout): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("scouts").document()
        val effectiveMode = if (scout.dealMode == DealMode.FREE_ONLY.wireValue) {
            DealMode.FREE_ONLY.wireValue
        } else {
            DealMode.CASH_BARGAIN.wireValue
        }
        val effectiveMaxPrice = if (effectiveMode == DealMode.FREE_ONLY.wireValue) 0L else scout.maxPrice.coerceIn(1L, 100000L)

        val payload = mapOf(
            "userId" to uid,
            "title" to scout.title.trim().take(80),
            "keywords" to scout.keywords.trim().take(100),
            "category" to scout.category.trim().take(40).ifEmpty { "All" },
            "dealMode" to effectiveMode,
            "maxPrice" to effectiveMaxPrice,
            "citySlug" to scout.citySlug.trim().take(40).ifEmpty { "sfbay" },
            "cityName" to scout.cityName.trim().take(60).ifEmpty { "SF Bay Area" },
            "radiusMiles" to scout.radiusMiles.coerceIn(1L, 500L),
            "targetResale" to scout.targetResale.coerceIn(0L, 1000000L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteScout(scoutId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("scouts").document(scoutId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createFlip(item: FlipItem): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("flips").document()
        val effectiveAcq = if (item.acquisitionType == AcquisitionType.FREE.wireValue) {
            AcquisitionType.FREE.wireValue
        } else {
            AcquisitionType.CASH.wireValue
        }
        val effectiveBuyCents = if (effectiveAcq == AcquisitionType.FREE.wireValue) {
            0L
        } else {
            item.buyPriceCents.coerceIn(0L, 100000000L)
        }

        val payload = mapOf(
            "userId" to uid,
            "title" to item.title.trim().take(100),
            "sourcePlatform" to item.sourcePlatform.trim().take(40).ifEmpty { "Facebook Marketplace" },
            "category" to item.category.trim().take(40).ifEmpty { "Furniture" },
            "acquisitionType" to effectiveAcq,
            "buyPriceCents" to effectiveBuyCents,
            "repairCostCents" to item.repairCostCents.coerceIn(0L, 100000000L),
            "estResaleCents" to item.estResaleCents.coerceIn(0L, 100000000L),
            "soldPriceCents" to item.soldPriceCents.coerceIn(0L, 100000000L),
            "status" to item.status,
            "listingUrl" to item.listingUrl.trim().take(500),
            "notes" to item.notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFlipStatus(
        flipId: String,
        newStatus: FlipStatus,
        soldPriceCents: Long? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("flips").document(flipId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus.wireValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (soldPriceCents != null) {
            updates["soldPriceCents"] = soldPriceCents.coerceIn(0L, 100000000L)
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFlipDetails(
        flipId: String,
        title: String,
        buyPriceCents: Long,
        repairCostCents: Long,
        estResaleCents: Long,
        notes: String,
        acquisitionType: AcquisitionType
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("flips").document(flipId)
        val effectiveBuy = if (acquisitionType == AcquisitionType.FREE) 0L else buyPriceCents.coerceIn(0L, 100000000L)
        val updates = mapOf<String, Any>(
            "title" to title.trim().take(100),
            "acquisitionType" to acquisitionType.wireValue,
            "buyPriceCents" to effectiveBuy,
            "repairCostCents" to repairCostCents.coerceIn(0L, 100000000L),
            "estResaleCents" to estResaleCents.coerceIn(0L, 100000000L),
            "notes" to notes.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getFlipById(userId: String, flipId: String): Result<FlipItem?> {
        val docRef = db.collection("users").document(userId).collection("flips").document(flipId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    FlipItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFlip(flipId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("flips").document(flipId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
