package app.fiber.user

import app.fiber.database.User
import app.fiber.database.UserDatabase
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

/**
 * Repository to save [User] data.
 *
 * @author Tammo0987
 * @since 1.0
 */
class UserRepository : KoinComponent {

    /**
     * [UserDatabase] to get the data.
     */
    private val userDatabase by inject<UserDatabase>()

    /**
     * Add [User] to this repository.
     */
    fun addUser(user: User) = this.userDatabase.insertUser(user)

    /**
     * Get specific user by id [userId].
     *
     * @param [userId] [UUID] id of the user.
     *
     * @return [User] user with the specific id.
     */
    fun getUserById(userId: String): User? = this.userDatabase.getUserById(UUID.fromString(userId))

    /**
     * Get specific user by name [name].
     *
     * @param [name] id of the user.
     *
     * @return [User] user with the specific name.
     */
    fun getUserByName(name: String): User? = this.userDatabase.getUserByName(name)

    /**
     * Delete a specific user.
     *
     * @param [user] [User] user to delete.
     */
    fun deleteUser(user: User) = this.userDatabase.deleteUser(user)

    /**
     * Invalidate cache for [userId].
     *
     * @param [userId] [UUID] user id to invalidate.
     */
    fun invalidateCache(userId: UUID) = this.userDatabase.invalidateCache(userId)

}