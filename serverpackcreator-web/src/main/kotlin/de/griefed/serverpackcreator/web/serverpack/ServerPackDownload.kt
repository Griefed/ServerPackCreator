package de.griefed.serverpackcreator.web.serverpack

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
class ServerPackDownload(@ManyToOne var serverPack: ServerPack) {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id: Int = 0

    @CreationTimestamp
    @Column
    var downloadedAt: Timestamp? = null
}