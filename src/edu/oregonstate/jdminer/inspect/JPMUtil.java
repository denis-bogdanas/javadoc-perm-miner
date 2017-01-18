package edu.oregonstate.jdminer.inspect;

import java.util.Collection;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 11/29/2016.
 */
public class JPMUtil {

    /**
     * @return true if str starts with any prefix in prefixes, false otherwise.
     */
    public static boolean startsWithAny(String str, Collection<String> prefixes) {
        return prefixes.stream().anyMatch(str::startsWith);
    }
}
