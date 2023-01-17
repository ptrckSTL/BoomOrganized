package com.simplemobiletools.smsmessenger.interfaces

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bo_table")
data class OrganizedContact(
    val cell: String,
    val firstName: String?,
    val lastName: String?,
    val status: BoomStatus,
    @PrimaryKey
    val uuid: String = cell + firstName,
)

enum class BoomStatus {
    PENDING,
    SENDING,
    SENT
}

@Dao
interface BoomOrganizedDao {
    @Insert
    suspend fun insertContacts(contacts: List<OrganizedContact>)

    @Upsert
    suspend fun upsertContact(contact: OrganizedContact)

    @Query("UPDATE bo_table SET status = :status WHERE uuid = :uuid")
    suspend fun updateMessageStatus(uuid: String, status: BoomStatus)

    @Query("DELETE FROM bo_table WHERE cell =:cell")
    suspend fun removeContact(cell: String)

    @Query("UPDATE bo_table SET status = :status WHERE uuid = :uuid")
    suspend fun updateMessageStatusToSent(uuid: String, status: BoomStatus = BoomStatus.SENT)

    @Delete
    suspend fun removeContact(contact: OrganizedContact)

    @Query("SELECT * FROM bo_table")
    suspend fun getPendingContacts(): List<OrganizedContact>
    @Query("SELECT * FROM bo_table LIMIT 1")
    suspend fun getOnePendingContact(): OrganizedContact

    @Query("SELECT * FROM bo_table LIMIT 1")
    fun observePendingContactsAsFlow(): Flow<OrganizedContact>

    @Query("SELECT * FROM bo_table LIMIT 1")
    fun observeAllPendingContactsAsFlow(): Flow<List<OrganizedContact>>

    @Query("DELETE FROM bo_table")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM bo_table")
    suspend fun countRows(): Int

}