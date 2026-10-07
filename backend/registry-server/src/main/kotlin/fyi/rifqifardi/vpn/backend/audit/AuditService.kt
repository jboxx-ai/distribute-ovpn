package fyi.rifqifardi.vpn.backend.audit

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object AuditLogsTable : Table("audit_logs") {
    val id = varchar("id", 64)
    val actor = varchar("actor", 128)
    val action = varchar("action", 64)
    val targetType = varchar("target_type", 64)
    val targetId = varchar("target_id", 128)
    val detailsSanitized = text("details_sanitized")
    val timestampEpochMs = long("timestamp_epoch_ms")

    override val primaryKey = PrimaryKey(id)
}

class AuditService {
    private val sensitivePatterns = listOf(
        Regex("(?i)password=([^&\\s]+)"),
        Regex("(?i)bearer\\s+([a-zA-Z0-9_-]+)"),
        Regex("(?i)apikey=([^&\\s]+)"),
        Regex("(?i)-----BEGIN[ A-Z0-9_-]+PRIVATE KEY-----[\\s\\S]+?-----END[ A-Z0-9_-]+PRIVATE KEY-----")
    )

    fun log(actor: String, action: String, targetType: String, targetId: String, rawDetails: String) {
        val sanitized = sanitize(rawDetails)
        transaction {
            AuditLogsTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[AuditLogsTable.actor] = actor
                it[AuditLogsTable.action] = action
                it[AuditLogsTable.targetType] = targetType
                it[AuditLogsTable.targetId] = targetId
                it[detailsSanitized] = sanitized
                it[timestampEpochMs] = System.currentTimeMillis()
            }
        }
    }

    fun sanitize(input: String): String {
        var result = input
        for (pattern in sensitivePatterns) {
            result = result.replace(pattern, "[REDACTED_SECRET]")
        }
        return result
    }
}
