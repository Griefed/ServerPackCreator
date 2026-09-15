/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.plugin.grinder.core

/**
 * One selection, split across the two configuration keys it is stored under.
 *
 * @param confirmed Entries filed as proven findings.
 * @param other Entries filed as accepted-at-your-own-risk.
 *
 * @author Griefed
 */
data class AttributedSelection(
    val confirmed: Set<String>,
    val other: Set<String>
)

/**
 * Decides which of the two panes each ticked entry is recorded under.
 *
 * This is not a decision about *whether* a mod is excluded — generation reads the union of both keys, so
 * every arrangement here produces the same server pack. It decides whether the user's record of what they
 * accepted **at their own risk** survives, which is the only reason the interface has two lists.
 *
 * It lives here rather than in the tab because it is pure, and because it was wrong while it was not:
 * filing by "is it shown in the Other pane?" alone sends everything the panes are not currently showing
 * to [SelectionPane.CONFIRMED], and the module's deliberate never-prune rule guarantees such entries
 * exist.
 *
 * @author Griefed
 */
object SelectionAttribution {

    /**
     * Split [selected] into the two panes.
     *
     * What a pane currently **shows** decides, so a verdict the grinder has re-classified moves to the
     * list it now belongs in. An entry neither pane shows keeps the pane it was **stored** under, because
     * the alternative is telling a user a mod was proven clientside-only when what they actually did was
     * accept a risk. An entry that is neither shown nor stored has no evidence behind it at all and goes
     * to [SelectionPane.OTHER] — the side that warns.
     *
     * @param selected Everything currently ticked, across both panes.
     * @param shownInConfirmed The entries the Confirmed pane is displaying.
     * @param shownInOther The entries the Other Verdicts pane is displaying.
     * @param storedConfirmed What was previously saved as confirmed.
     * @param storedOther What was previously saved as other.
     */
    fun split(
        selected: Set<String>,
        shownInConfirmed: Set<String>,
        shownInOther: Set<String>,
        storedConfirmed: Set<String>,
        storedOther: Set<String>
    ): AttributedSelection {
        val confirmed = LinkedHashSet<String>()
        val other = LinkedHashSet<String>()
        for (entry in selected) {
            when {
                // Shown wins over stored: this is how a re-ground verdict changes lists.
                entry in shownInConfirmed -> confirmed += entry
                entry in shownInOther -> other += entry
                // Not shown anywhere — the grinder no longer reports it, or has not been reached this
                // session. Keep what was saved rather than inventing an attribution.
                entry in storedConfirmed -> confirmed += entry
                entry in storedOther -> other += entry
                // Never seen and never saved. Nothing supports calling it proven.
                else -> other += entry
            }
        }
        return AttributedSelection(confirmed, other)
    }
}
