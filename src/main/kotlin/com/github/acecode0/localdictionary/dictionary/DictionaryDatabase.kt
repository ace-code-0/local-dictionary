package com.github.acecode0.localdictionary.dictionary

import com.intellij.openapi.application.PathManager
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

class DictionaryDatabase(private val databasePath: Path) : AutoCloseable {
    init {
        // IntelliJ's plugin classloader may prevent DriverManager from discovering JDBC services.
        Class.forName("org.sqlite.JDBC")
    }

    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:${databasePath.toUri()}?mode=ro")

    fun lookup(word: String): String? =
        connection.prepareStatement(
            "SELECT translation FROM stardict WHERE lower(word) = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, word.lowercase())
            statement.executeQuery().use { results ->
                if (results.next()) results.getString(1) else null
            }
        }

    override fun close() {
        connection.close()
    }

    companion object {
        private const val RESOURCE_PATH = "/dictionary/ecdict.db"

        fun openBundled(): DictionaryDatabase = DictionaryDatabase(prepareBundledDatabase())

        @Synchronized
        private fun prepareBundledDatabase(): Path {
            val destination = Path.of(PathManager.getSystemPath(), "local-dictionary", "ecdict.db")
            Files.createDirectories(destination.parent)

            DictionaryDatabase::class.java.getResourceAsStream(RESOURCE_PATH).use { resource ->
                requireNotNull(resource) { "Missing bundled dictionary resource: $RESOURCE_PATH" }
                if (Files.exists(destination) && resource.sha256() == destination.sha256()) {
                    return destination
                }
            }

            DictionaryDatabase::class.java.getResourceAsStream(RESOURCE_PATH).use { resource ->
                requireNotNull(resource) { "Missing bundled dictionary resource: $RESOURCE_PATH" }
                val temporary = Files.createTempFile(destination.parent, "ecdict-", ".db")
                try {
                    resource.copyTo(Files.newOutputStream(temporary))
                    try {
                        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temporary)
                }
            }
            return destination
        }

        private fun InputStream.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(readBytes())

        private fun Path.sha256(): ByteArray = Files.newInputStream(this).use { input -> input.sha256() }
    }
}
