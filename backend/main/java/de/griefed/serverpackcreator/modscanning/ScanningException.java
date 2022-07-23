package de.griefed;

public class ScanningException extends Exception {

  public ScanningException(String errorMessage, Throwable error) {
    super(errorMessage, error);
  }

  public ScanningException(String errorMessage) {
    super(errorMessage);
  }

}
