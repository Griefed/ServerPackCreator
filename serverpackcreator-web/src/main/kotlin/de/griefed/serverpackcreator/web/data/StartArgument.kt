package de.griefed.serverpackcreator.web.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class StartArgument {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    var id: Int = 0

    @Column
    var argument: String = ""

    constructor(argument: String) {
        this.argument = argument
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StartArgument

        return argument == other.argument
    }

    override fun hashCode(): Int {
        return argument.hashCode()
    }

    override fun toString(): String {
        return "StartArgument(id=$id, argument='$argument')"
    }
}