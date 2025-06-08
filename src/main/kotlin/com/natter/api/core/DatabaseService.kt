package com.natter.api.core

import com.natter.api.model.Space
import org.dalesbred.Database

class DatabaseService(url: String, username: String, password: String) {

    val db: Database = Database.forUrlAndCredentials(url, username, password)

    fun createSpace(spaceName: String, owner: String): Space {
        return db.withTransaction {
            val spaceId = db.findUniqueLong("SELECT NEXTVAL('space_id_seq')")
            db.updateUnique(
                    "INSERT INTO spaces (space_id, name, owner) VALUES (?, ?, ?)",
                    spaceId,
                    spaceName,
                    owner
            )
            Space(spaceId.toLong(), spaceName, owner)
        }
    }
}
