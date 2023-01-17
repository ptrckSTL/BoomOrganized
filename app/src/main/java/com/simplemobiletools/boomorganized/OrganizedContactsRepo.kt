package com.simplemobiletools.boomorganized

import com.simplemobiletools.smsmessenger.App
import com.simplemobiletools.smsmessenger.extensions.boomOrganizedDB
import com.simplemobiletools.smsmessenger.interfaces.BoomOrganizedDao
import com.simplemobiletools.smsmessenger.interfaces.BoomStatus
import com.simplemobiletools.smsmessenger.interfaces.OrganizedContact

interface OrganizedContactsRepo

 val OrganizedContactsRepo.dao: BoomOrganizedDao
    get() = App.instance.boomOrganizedDB

suspend fun OrganizedContactsRepo.clearAndPopulate(contacts: List<OrganizedContact>) {
    dao.apply {
        clear()
        insertContacts(contacts)
    }
}

suspend fun OrganizedContactsRepo.upsertContact(contact: OrganizedContact) = dao.upsertContact(contact)
suspend fun OrganizedContactsRepo.getPendingContacts() = dao.getPendingContacts()

suspend fun OrganizedContactsRepo.remove(organizedContact: OrganizedContact) = dao.removeContact(organizedContact)

suspend fun OrganizedContactsRepo.countPending() = dao.countRows()

suspend fun OrganizedContactsRepo.clearDB() {
    dao.clear()
}

suspend fun OrganizedContactsRepo.setStatusToSending(contact: OrganizedContact){
    dao.updateMessageStatus(contact.uuid, BoomStatus.SENDING)
}
suspend fun OrganizedContactsRepo.setStatusToSent(contact: OrganizedContact){
    dao.updateMessageStatus(contact.uuid, BoomStatus.SENT)
}