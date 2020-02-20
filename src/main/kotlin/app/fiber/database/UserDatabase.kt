package app.fiber.database

import java.util.*

/**
 * Represent a user account.
 *
 * @param [uuid] [UUID] to identify the user as unique.
 * @param [name] Name of the user as an alias for a better distinction.
 * @param [password] Password to authorize if the user of the application has access to this account.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class User(val uuid: UUID, val name: String, val password: String)

/**
 * Abstraction of the database for user.
 *
 * @author Tammo0987
 * @since 1.0
 */
interface UserDatabase {

    /**
     * Insert [User] in the database.
     *
     * @param [user] [User] the user which will be inserted.
     */
    fun insertUser(user: User)

    /**
     * Get specific user by id [userId].
     *
     * @param [userId] [UUID] id of the user.
     *
     * @return [User] user with the specific id.
     */
    fun getUserById(userId: UUID): User?

    /**
     * Get specific user by name [name].
     *
     * @param [name] id of the user.
     *
     * @return [User] user with the specific name.
     */
    fun getUserByName(name: String): User?

    /**
     * Delete a specific user.
     *
     * @param [user] [User] user to delete.
     */
    fun deleteUser(user: User)

    /**
     * Invalidate cache for [userId].
     *
     * @param [userId] [UUID] user id to invalidate.
     */
    fun invalidateCache(userId: UUID)

}