package com.yansproject.app.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yansproject.app.data.AppDatabase
import com.yansproject.app.data.FirebaseSyncManager
import com.yansproject.app.ui.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MemberViewModel : ViewModel() {
    private val _members = MutableStateFlow<List<MemberModel>>(emptyList())
    val members: StateFlow<List<MemberModel>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteStatus = MutableStateFlow<String?>(null)
    val deleteStatus: StateFlow<String?> = _deleteStatus.asStateFlow()

    private var isObservingRealtime = false

    fun loadMembers(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val tempMap = mutableMapOf<String, MemberModel>()
                val localMembers = AppSettings.getMembers(context).toMutableSet()
 
                // 1. Instantly populate with saved local credentials and emit to UI
                val prefs = context.getSharedPreferences("yans_local_credentials", Context.MODE_PRIVATE)
                val allEntries = prefs.all
                for ((key, value) in allEntries) {
                    if (key.startsWith("name_") && value is String) {
                        val emailKey = key.substring("name_".length)
                        val pass = prefs.getString("pass_$emailKey", "") ?: ""
                        val role = prefs.getString("role_$emailKey", "MEMBER") ?: "MEMBER"
                        val price = prefs.getString("price_$emailKey", "Member") ?: "Member"
                        val whatsapp = prefs.getString("wa_$emailKey", "") ?: ""
                        val address = prefs.getString("address_$emailKey", "") ?: ""
                        val createdAt = prefs.getLong("created_at_$emailKey", 0L)
                        val statusAkun = prefs.getString("status_akun_$emailKey", "Aktif") ?: "Aktif"
                        val statusVerifikasi = prefs.getString("status_verifikasi_$emailKey", "Terverifikasi") ?: "Terverifikasi"
                        
                        val lastLogin = prefs.getLong("last_login_$emailKey", 0L)
                        
                        tempMap[emailKey] = MemberModel(
                            email = emailKey,
                            displayName = value,
                            role = role,
                            priceCategory = price,
                            passwordOrPin = pass,
                            whatsapp = whatsapp,
                            address = address,
                            createdAt = createdAt,
                            lastLogin = lastLogin,
                            statusAkun = "Aktif",
                            statusVerifikasi = statusVerifikasi
                        )
                        if (!role.equals("OWNER", ignoreCase = true) && !value.contains("Owner", ignoreCase = true)) {
                            localMembers.add(value)
                        }
                    }
                }

                // Immediately emit cached/offline list to UI (filtered & deduplicated)
                val filtered = tempMap.values.toList().filter { m ->
                    val isOwner = m.role.equals("OWNER", ignoreCase = true) ||
                            m.displayName.equals("Owner", ignoreCase = true) ||
                            m.displayName.contains("Owner", ignoreCase = true) ||
                            m.email.equals("admin@yansproject.id", ignoreCase = true) ||
                            m.email.contains("owner", ignoreCase = true)
                    !isOwner && m.displayName.isNotBlank()
                }.distinctBy { it.email.lowercase().trim().ifEmpty { it.displayName.lowercase().trim() } }

                _members.value = filtered
                _isLoading.value = false // Instant first load complete
 
                // 2. Start Real-Time Flow Observation
                if (!isObservingRealtime && FirebaseSyncManager.isFirebaseActive) {
                    isObservingRealtime = true
                    val repository = com.yansproject.app.data.MemberRepository(context)
                    repository.observeMembersRealtime().collect { observedMembers ->
                        val distinctObserved = observedMembers.filter { m ->
                            !m.role.equals("OWNER", ignoreCase = true) &&
                            !m.displayName.equals("Owner", ignoreCase = true) &&
                            !m.displayName.contains("Owner", ignoreCase = true) &&
                            !m.email.equals("admin@yansproject.id", ignoreCase = true)
                        }.distinctBy { it.email.lowercase().trim().ifEmpty { it.displayName.lowercase().trim() } }
                        _members.value = distinctObserved
                    }
                }
            } catch (e: Exception) {
                Log.e("MemberViewModel", "Error loading or observing members: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerMember(
        context: Context,
        email: String,
        passwordOrPin: String,
        displayName: String,
        priceCategory: String,
        role: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = FirebaseSyncManager.registerMemberOnCloud(
                    context,
                    email,
                    passwordOrPin,
                    displayName,
                    priceCategory,
                    role
                )
                if (result == "SUCCESS") {
                    loadMembers(context)
                    onComplete(true, "Akun '$displayName' ($role) berhasil didaftarkan!")
                } else {
                    onComplete(false, "Pendaftaran Gagal: $result")
                }
            } catch (e: Exception) {
                onComplete(false, "Pendaftaran Gagal: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemberTier(
        context: Context,
        email: String,
        displayName: String,
        newTier: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val repo = com.yansproject.app.data.MemberRepository(context)
                val success = repo.updateMemberTier(email, displayName, newTier)
                withContext(Dispatchers.Main) {
                    if (success) {
                        loadMembers(context)
                        onComplete(true, "Tier harga member '$displayName' berhasil diperbarui ke '$newTier'!")
                    } else {
                        onComplete(false, "Gagal memperbarui tier harga.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Error: ${e.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPasswordOrPin(
        context: Context,
        email: String,
        displayName: String,
        newPassOrPin: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val repo = com.yansproject.app.data.MemberRepository(context)
                val success = repo.resetPasswordOrPin(email, newPassOrPin)
                withContext(Dispatchers.Main) {
                    if (success) {
                        loadMembers(context)
                        onComplete(true, "Password / PIN member '$displayName' berhasil di-reset!")
                    } else {
                        onComplete(false, "Gagal me-reset password / PIN.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Error: ${e.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemberProfile(
        context: Context,
        email: String,
        newDisplayName: String,
        newWhatsapp: String,
        newAddress: String,
        newTier: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val repo = com.yansproject.app.data.MemberRepository(context)
                val success = repo.updateMemberProfile(email, newDisplayName, newWhatsapp, newAddress, newTier)
                withContext(Dispatchers.Main) {
                    if (success) {
                        loadMembers(context)
                        onComplete(true, "Profil member '$newDisplayName' berhasil diperbarui!")
                    } else {
                        onComplete(false, "Gagal memperbarui profil member.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Error: ${e.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMember(userId: String, context: Context, member: MemberModel, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _deleteStatus.value = null
            var success = true
            var message = ""
            try {
                val targetEmail = member.email.lowercase().trim()
                val displayName = member.displayName.trim()

                // 1. Immediately clear local credentials and member name from AppSettings
                AppSettings.removeLocalUserCredential(context, targetEmail, displayName)

                // 2. Delete corresponding invoice transactions from local SQLite if needed
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM invoices WHERE clientName = ? OR clientName = ?",
                        arrayOf(displayName, targetEmail)
                    )
                } catch (e: Exception) {
                    Log.e("MemberViewModel", "Failed clearing local invoices for member: ${e.message}")
                }

                // 3. Delete from Cloud Firestore and queue cloud sync deletion
                if (FirebaseSyncManager.isFirebaseActive) {
                    val repo = com.yansproject.app.data.MemberRepository(context)
                    repo.deleteMemberFromCloud(targetEmail, displayName)
                }

                _deleteStatus.value = "Member '$displayName' berhasil dihapus total dari lokal & cloud"
                message = "Member '$displayName' berhasil dihapus total"
                
                // Immediately update local UI
                loadMembers(context)
            } catch (e: Exception) {
                success = false
                _deleteStatus.value = "Error: ${e.message}"
                message = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    onComplete(success, message)
                }
            }
        }
    }
}

object memberDao {
    fun deleteById(userId: String) {
        Log.d("memberDao", "Mock Room deleteById for userId: $userId")
    }
}
