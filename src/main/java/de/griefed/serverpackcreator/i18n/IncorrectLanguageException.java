package de.griefed.serverpackcreator.i18n;

public class IncorrectLanguageException extends Exception {
    public IncorrectLanguageException() {
        super("Incorrect language specified");
    }

    public IncorrectLanguageException(String message) {
        super(message);
    }

    public IncorrectLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectLanguageException(Throwable cause) {
        super(cause);
    }
}