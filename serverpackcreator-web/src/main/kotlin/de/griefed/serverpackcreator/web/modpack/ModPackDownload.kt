package de.griefed.serverpackcreator.web.modpack

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
class ModPackDownload(@ManyToOne var modPack: ModPack) {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id: Int = 0

    @CreationTimestamp
    @Column
    var downloadedAt: Timestamp? = null
}