package com.yansproject.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.yansproject.app.ui.settings.MemberModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MemberRepository(private val context: Context) {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun observeMembersRealtime(): Flow<List<MemberModel>> = callbackFlow {
        if (!FirebaseSyncManager.isFirebaseActive) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d("MemberRepository", "Registering real-time Firestore listener for 'users' collection")
        val listenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MemberRepository", "Error observing members: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val membersList = mutableListOf<MemberModel>()
                    val prefs = context.getSharedPreferences("yans_local_credentials", Context.MODE_PRIVATE)
                    val edit = prefs.edit()

                    for (doc in snapshot.documents) {
                        val email = doc.getString("email") ?: doc.id
                        val displayName = doc.getString("displayName") ?: ""
                        val role = doc.getString("role") ?: "MEMBER"
                        val priceCategory = doc.getString("priceCategory") ?: "Member"
                        val passwordOrPin = doc.getString("passwordOrPin") ?: ""
                        val whatsapp = doc.getString("whatsapp") ?: doc.getString("phone") ?: doc.getString("phoneNumber") ?: ""
                        val address = doc.getString("address") ?: ""
                        val createdAt = doc.getLong("created_at") ?: doc.getLong("createdAt") ?: 0L
                        val lastLogin = doc.getLong("lastLogin") ?: doc.getLong("last_login") ?: doc.getLong("lastActive") ?: 0L
                        val statusAkun = doc.getString("statusAkun") ?: doc.getString("status") ?: "Aktif"
                        val statusVerifikasi = doc.getString("statusVerifikasi") ?: doc.getString("status_verifikasi") ?: "Terverifikasi"

                        // System Role separation: Dashboard / Member Management only handles actual registered "MEMBER" role
                        // Filter out OWNER accounts completely so Owner is never listed as a Member
                        val isOwner = role.equals("OWNER", ignoreCase = true) ||
                                displayName.equals("Owner", ignoreCase = true) ||
                                displayName.contains("Owner", ignoreCase = true) ||
                                email.equals("admin@yansproject.id", ignoreCase = true) ||
                                email.contains("owner", ignoreCase = true)

                        if (!isOwner && displayName.isNotBlank() && (role.equals("MEMBER", ignoreCase = true) || role.isBlank())) {
                            val normalizedEmail = email.lowercase().trim()
                            
                            // Synchronize with local offline cache
                            com.yansproject.app.ui.AppSettings.addMember(context, displayName)
                            com.yansproject.app.ui.AppSettings.saveLocalUserCredential(
                                context, email, passwordOrPin, displayName, "MEMBER", priceCategory
                            )
                            com.yansproject.app.ui.AppSettings.saveMemberPriceCategory(context, displayName, priceCategory)

                            edit.putString("wa_$normalizedEmail", whatsapp)
                                .putString("address_$normalizedEmail", address)
                                .putLong("created_at_$normalizedEmail", createdAt)
                                .putLong("last_login_$normalizedEmail", lastLogin)
                                .putString("status_akun_$normalizedEmail", statusAkun)
                                .putString("status_verifikasi_$normalizedEmail", statusVerifikasi)

                            val finalModel = MemberModel(
                                email = email,
                                displayName = displayName,
                                role = "MEMBER",
                                priceCategory = priceCategory,
                                passwordOrPin = passwordOrPin,
                                whatsapp = whatsapp,
                                address = address,
                                createdAt = createdAt,
                                lastLogin = lastLogin,
                                statusAkun = "Aktif",
                                statusVerifikasi = statusVerifikasi
                            )
                            membersList.add(finalModel)
                        }
                    }
                    edit.apply()
                    val distinctMembers = membersList.distinctBy { 
                        it.email.lowercase().trim().ifEmpty { it.displayName.lowercase().trim() } 
                    }
                    trySend(distinctMembers)
                }
            }

        awaitClose {
            Log.d("MemberRepository", "Removing real-time Firestore listener for 'users' collection")
            listenerRegistration.remove()
        }
    }

    suspend fun deleteMemberFromCloud(email: String, displayName: String): Boolean {
        return try {
            val targetEmail = email.lowercase().trim()
            if (FirebaseSyncManager.isFirebaseActive) {
                try {
                    firestore.collection("users").document(targetEmail).delete().await()
                } catch (e: Exception) {
                    Log.e("MemberRepository", "Failed deleting doc by ID: ${e.message}")
                }
                
                try {
                    val snapshotByEmail = firestore.collection("users").whereEqualTo("email", targetEmail).get().await()
                    for (doc in snapshotByEmail.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.e("MemberRepository", "Failed deleting docs by email query: ${e.message}")
                }

                if (displayName.isNotBlank()) {
                    try {
                        val snapshotByName = firestore.collection("users").whereEqualTo("displayName", displayName).get().await()
                        for (doc in snapshotByName.documents) {
                            val role = doc.getString("role") ?: ""
                            if (!role.equals("OWNER", ignoreCase = true)) {
                                doc.reference.delete().await()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MemberRepository", "Failed deleting docs by displayName query: ${e.message}")
                    }
                }

                FirebaseSyncManager.deleteItemFromCloud("users", targetEmail)
            }
            true
        } catch (e: Exception) {
            Log.e("MemberRepository", "Failed deleting member from cloud: ${e.message}")
            false
        }
    }

    suspend fun updateMemberTier(email: String, displayName: String, newTier: String): Boolean {
        return try {
            val targetEmail = email.lowercase().trim()
            if (FirebaseSyncManager.isFirebaseActive) {
                firestore.collection("users").document(targetEmail)
                    .update("priceCategory", newTier)
                    .await()
            }
            com.yansproject.app.ui.AppSettings.saveMemberPriceCategory(context, displayName, newTier)
            val prefs = context.getSharedPreferences("yans_local_credentials", Context.MODE_PRIVATE)
            prefs.edit().putString("price_$targetEmail", newTier).apply()
            true
        } catch (e: Exception) {
            Log.e("MemberRepository", "Failed updating member tier: ${e.message}")
            false
        }
    }

    suspend fun resetPasswordOrPin(email: String, newPassOrPin: String): Boolean {
        return try {
            val targetEmail = email.lowercase().trim()
            if (FirebaseSyncManager.isFirebaseActive) {
                firestore.collection("users").document(targetEmail)
                    .update("passwordOrPin", newPassOrPin)
                    .await()
            }
            val prefs = context.getSharedPreferences("yans_local_credentials", Context.MODE_PRIVATE)
            prefs.edit().putString("pass_$targetEmail", newPassOrPin).apply()
            true
        } catch (e: Exception) {
            Log.e("MemberRepository", "Failed resetting password or PIN: ${e.message}")
            false
        }
    }

    suspend fun updateMemberProfile(
        email: String,
        newDisplayName: String,
        newWhatsapp: String,
        newAddress: String,
        newTier: String
    ): Boolean {
        return try {
            val targetEmail = email.lowercase().trim()
            if (FirebaseSyncManager.isFirebaseActive) {
                firestore.collection("users").document(targetEmail)
                    .update(
                        mapOf(
                            "displayName" to newDisplayName,
                            "whatsapp" to newWhatsapp,
                            "address" to newAddress,
                            "priceCategory" to newTier
                        )
                    )
                    .await()
            }
            com.yansproject.app.ui.AppSettings.saveMemberPriceCategory(context, newDisplayName, newTier)
            com.yansproject.app.ui.AppSettings.addMember(context, newDisplayName)
            val prefs = context.getSharedPreferences("yans_local_credentials", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("name_$targetEmail", newDisplayName)
                .putString("wa_$targetEmail", newWhatsapp)
                .putString("address_$targetEmail", newAddress)
                .putString("price_$targetEmail", newTier)
                .apply()
            true
        } catch (e: Exception) {
            Log.e("MemberRepository", "Failed updating member profile: ${e.message}")
            false
        }
    }
}
