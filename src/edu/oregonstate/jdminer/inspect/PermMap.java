package edu.oregonstate.jdminer.inspect;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 11/28/2016.
 */
public class PermMap {

    /**
     * Map from permissions to regex expressions used to search for these permissions in javadoc.
     */
    public static final Map<String, String> wordMap = ImmutableMap.<String, String>builder()
            .put("android.permission.READ_CALENDAR", "READ_CALENDAR")
            .put("android.permission.WRITE_CALENDAR", "WRITE_CALENDAR")
            .put("android.permission.CAMERA", "CAMERA")
            .put("android.permission.READ_CONTACTS", "READ_CONTACTS")
            .put("android.permission.WRITE_CONTACTS", "WRITE_CONTACTS")
            .put("android.permission.GET_ACCOUNTS", "GET_ACCOUNTS")
            .put("android.permission.ACCESS_FINE_LOCATION", "ACCESS_FINE_LOCATION")
            .put("android.permission.ACCESS_COARSE_LOCATION", "ACCESS_COARSE_LOCATION")
            .put("android.permission.RECORD_AUDIO", "RECORD_AUDIO")
            .put("android.permission.READ_PHONE_STATE", "READ_PHONE_STATE")
            .put("android.permission.CALL_PHONE", "CALL_PHONE")
            .put("android.permission.READ_CALL_LOG", "READ_CALL_LOG")
            .put("android.permission.WRITE_CALL_LOG", "WRITE_CALL_LOG")
            .put("android.permission.ADD_VOICEMAIL", "ADD_VOICEMAIL")
            .put("android.permission.USE_SIP", "USE_SIP")
            .put("android.permission.PROCESS_OUTGOING_CALLS", "PROCESS_OUTGOING_CALLS")
            .put("android.permission.BODY_SENSORS", "BODY_SENSORS")
            .put("android.permission.SEND_SMS", "SEND_SMS")
            .put("android.permission.RECEIVE_SMS", "RECEIVE_SMS")
            .put("android.permission.READ_SMS", "READ_SMS")
            .put("android.permission.RECEIVE_WAP_PUSH", "RECEIVE_WAP_PUSH")
            .put("android.permission.RECEIVE_MMS", "RECEIVE_MMS")
            .build();

    /**
     * Map from permissions to regex expressions used to search for these permissions in javadoc.
     * <p>
     * Only include permissions for which regex is different than permission word.
     */
    public static final Map<String, String> regexMap = ImmutableMap.<String, String>builder()
            .build();
}
