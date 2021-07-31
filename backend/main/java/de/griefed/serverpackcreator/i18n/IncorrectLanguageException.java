/* Copyright (C) 2021  Griefed
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

package de.griefed.serverpackcreator.i18n;

/**
 * <strong>Table of Exceptions</strong><br>
 * {@link #IncorrectLanguageException()}<br>
 * {@link #IncorrectLanguageException(String)}<br>
 * {@link #IncorrectLanguageException(Throwable)}<br>
 * {@link #IncorrectLanguageException(String, Throwable)}<p>
 * Provides exceptions to {@link LocalizationManager}
 * @author whitebear60
 */
public class IncorrectLanguageException extends Exception {

    /**
     * @author whitebear60
     */
    public IncorrectLanguageException() {
        super("Incorrect language specified");
    }

    /**
     * @author whitebear60
     * @param message The exception message.
     */
    public IncorrectLanguageException(String message) {
        super(message);
    }

    /**
     * @author whitebear60
     * @param message The exception message.
     * @param cause The cause of the exception.
     */
    public IncorrectLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @author whitebear60
     * @param cause The cause of the exception.
     */
    public IncorrectLanguageException(Throwable cause) {
        super(cause);
    }
}