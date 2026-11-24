package com.communityvoice.backend.repository

import com.communityvoice.backend.model.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, String> {

    /** [email] must already be lower-cased and trimmed by the caller. */
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
