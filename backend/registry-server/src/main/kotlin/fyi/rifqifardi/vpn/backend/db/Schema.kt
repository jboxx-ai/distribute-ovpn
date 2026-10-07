package fyi.rifqifardi.vpn.backend.db

import org.jetbrains.exposed.sql.Table

object ProfilesTable : Table("profiles") {
    val id = varchar("id", 64)
    val name = varchar("name", 128)
    val description = text("description").nullable()
    val createdAtEpochMs = long("created_at_epoch_ms")

    override val primaryKey = PrimaryKey(id)
}

object ProfileVersionsTable : Table("profile_versions") {
    val id = varchar("id", 64)
    val profileId = reference("profile_id", ProfilesTable.id)
    val version = long("version")
    val ovpnContent = text("ovpn_content")
    val sha256Digest = varchar("sha256_digest", 64)
    val signature = varchar("signature", 256)
    val keyId = varchar("key_id", 64)
    val createdAtEpochMs = long("created_at_epoch_ms")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_profile_version_unique", profileId, version)
    }
}

object ChannelPublicationsTable : Table("channel_publications") {
    val channel = varchar("channel", 32)
    val profileId = reference("profile_id", ProfilesTable.id)
    val activeVersionId = reference("active_version_id", ProfileVersionsTable.id)
    val updatedAtEpochMs = long("updated_at_epoch_ms")

    override val primaryKey = PrimaryKey(channel, profileId)
}
