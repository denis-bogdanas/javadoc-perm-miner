package edu.oregonstate.jdminer.inspect;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 4/29/2016.
 */
@SuppressWarnings("WeakerAccess")
public class DroidPermException extends Exception {

    public DroidPermException(String message) {
        super(message);
    }

    public DroidPermException(String message, Throwable cause) {
        super(message, cause);
    }

    public DroidPermException(Throwable cause) {
        super(cause);
    }

    public DroidPermException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
