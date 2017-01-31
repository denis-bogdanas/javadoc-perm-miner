package edu.oregonstate.jdminer.inspect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.oregonstate.droidperm.perm.miner.XmlPermDefMiner;
import org.oregonstate.droidperm.perm.miner.jaxb_out.PermTargetKind;
import org.oregonstate.droidperm.perm.miner.jaxb_out.PermissionDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Various constant data structrures for Javadoc permission miner.
 *
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 11/28/2016.
 */
public class JPMData {

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
            .put("com.android.voicemail.permission.ADD_VOICEMAIL", "ADD_VOICEMAIL")
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
            .put("android.permission.CAMERA", "[^_]CAMERA[^_]")
            .build();

    /**
     * Classes and packages to be excluded from the output.
     */
    public static final List<String> classExclusionList = Arrays.asList(
            "android.app.AppOpsManager",
            "android.os.Build",
            "android.content.pm.PackageParser", //from Intellij custom scopes
            "android.support.v17", //For TV devices only, unlikely to be used by f-droid apps.
            "android.location.LocationManager",
            "android.support.v4.app.Fragment"
    );

    /**
     * Map from class names to CustomPermDef objects defining their permissions.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static final List<CustomPermDef> classCustomPerm = ImmutableList.copyOf(new CustomPermDef[]{
            new CustomPermDef("android.hardware.Camera", Arrays.asList("android.permission.CAMERA"),
                    Arrays.asList("open")), new CustomPermDef("android.hardware.camera2.CameraDevice", null, null),
            new CustomPermDef("android.media.audiofx.Visualizer", Arrays.asList("android.permission.RECORD_AUDIO"),
                    Arrays.asList("<init>")),
            new CustomPermDef("android.net.rtp.AudioGroup", Arrays.asList("android.permission.RECORD_AUDIO"),
                    Arrays.asList("<init>")), new CustomPermDef("android.net.sip.SipAudioCall", null, null),
            new CustomPermDef("android.net.sip.SipManager", Arrays.asList("android.permission.USE_SIP"),
                    Arrays.asList("createSipSession", "getSessionFor", "open", "makeAudioCall", "takeAudioCall",
                            "register")),
            /*removing inner classes that are hidden (@hide)*/
            new CustomPermDef("android.provider.ContactsContract",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.AggregationExceptions",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.CommonDataKinds.Callable",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.CommonDataKinds.Contactables",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.CommonDataKinds.Email",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.CommonDataKinds.Phone",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.CommonDataKinds.StructuredPostal",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Contacts",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Data",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.DataUsageFeedback",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.DeletedContacts",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Directory", null, null),
            new CustomPermDef("android.provider.ContactsContract.DisplayPhoto",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Groups",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.PhoneLookup",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Profile",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.ProfileSyncState",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.ProviderStatus",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.RawContacts",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.RawContactsEntity",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.Settings",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.StatusUpdates",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.ContactsContract.SyncState",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"), true),
            new CustomPermDef("android.provider.VoicemailContract", null, null),
            new CustomPermDef("android.provider.VoicemailContract.Status",
                    Arrays.asList("com.android.voicemail.permission.ADD_VOICEMAIL"), true),
            new CustomPermDef("android.provider.VoicemailContract.Voicemails",
                    Arrays.asList("com.android.voicemail.permission.ADD_VOICEMAIL"), true),
            new CustomPermDef("android.speech.SpeechRecognizer", Arrays.asList("android.permission.RECORD_AUDIO"),
                    Arrays.asList("createSpeechRecognizer")), new CustomPermDef("android.telephony.SubscriptionManager",
            Arrays.asList("android.permission.READ_PHONE_STATE"), null, false, true),
            new CustomPermDef("android.telephony.SubscriptionManager.OnSubscriptionsChangedListener", null, null)
    });

    /**
     * Manually defined raw data for permissions, based on inspecting apps with unused permissions after javadoc perm
     * were in place.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static final List<CustomPermDef> manualPerm = ImmutableList.copyOf(new CustomPermDef[]{
            /*other actions in WifiManager might need Location or other permissions. Hard to know generally.*/
            new CustomPermDef("android.net.wifi.WifiManager",
                    Arrays.asList("android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_FINE_LOCATION"),
                    Arrays.asList("SCAN_RESULTS_AVAILABLE_ACTION")),
            new CustomPermDef("android.telephony.TelephonyManager",
                    Arrays.asList("android.permission.ACCESS_COARSE_LOCATION"),
                    Arrays.asList("getNeighboringCellInfo")),
            new CustomPermDef("android.provider.Telephony",
                    Arrays.asList("android.permission.READ_SMS"),
                    null, true, false, true),
            //some actions below actually require BROADCAST_SMS PERMISSION
            //Class contains other actions as well.
            //WARNING: permission specification for this field is not in javadoc comment,
            // but appears when pressing Ctrl+Q
            new CustomPermDef("android.provider.Telephony.Sms.Intents",
                    Arrays.asList("android.permission.RECEIVE_SMS"),
                    Arrays.asList("SMS_RECEIVED_ACTION")),
            //discovered by running DroidPerm init URI implementation and inspecting crashed apps.
            new CustomPermDef("android.provider.ContactsContract.Contacts",
                    Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                    Arrays.asList("lookupContact")),
            new CustomPermDef("android.media.MediaRecorder",
                    Arrays.asList("android.permission.RECORD_AUDIO"),
                    Arrays.asList("setAudioSource")),
            new CustomPermDef("android.media.AudioRecord",
                    Arrays.asList("android.permission.RECORD_AUDIO"),
                    Arrays.asList("<init>")),
            new CustomPermDef("android.telephony.PhoneStateListener",
                    Arrays.asList("android.permission.READ_PHONE_STATE"),
                    Arrays.asList("LISTEN_SIGNAL_STRENGTHS")),
            });

    /**
     * Raw input to generate parametric permission defs.
     * <p>
     * Perm lists here won't be used, but they have to be non-null.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static final List<CustomPermDef> parametricPerm = ImmutableList.copyOf(new CustomPermDef[]{
            /*other actions in WifiManager might need Location or other permissions. Hard to know generally.*/
            new CustomPermDef("android.content.ContentResolver", Arrays.asList("foo"),
                    Arrays.asList("query", "insert", "bulkInsert", "update", "delete")),
            new CustomPermDef("android.content.ContentProvider", Arrays.asList("foo"),
                    Arrays.asList("query", "insert", "bulkInsert", "update", "delete")),
            new CustomPermDef("android.content.CursorLoader", Arrays.asList("foo"),
                    Arrays.asList("<init>", "setUri")),
            new CustomPermDef("android.support.v4.content.CursorLoader", Arrays.asList("foo"),
                    Arrays.asList("<init>", "setUri")),
            new CustomPermDef("android.database.Cursor", Arrays.asList("foo"),
                    Arrays.asList("setNotificationUri")),
            new CustomPermDef("android.telephony.TelephonyManager", Arrays.asList("foo"),
                    Arrays.asList("listen")),
            new CustomPermDef("android.content.ContentProviderOperation", Arrays.asList("foo"),
                    Arrays.asList("newAssertQuery", "newInsert", "newUpdate", "newDelete")),
            });

    public static List<PermissionDef> getClassPermDefsCoveredByCustomDefs() {
        return classCustomPerm.stream().map(rawPermDef -> rawPermDef.className).distinct().map(className ->
                new PermissionDef(XmlPermDefMiner.processInnerClasses(className), null, PermTargetKind.Class, null))
                .collect(Collectors.toList());
    }

    /**
     * Key permission definitions info inside a certain class. Produced by manual processing for classes that don't have
     * permissions mentioned individually for members.
     */
    public static final class CustomPermDef {
        public final String className;
        public final List<String> permList;
        public final List<String> memberNames;
        public final boolean includeUriFields;
        public final boolean includeAllMethods;
        public final boolean includeInnerClassesForUri;

        public CustomPermDef(String className, List<String> permList, List<String> memberNames) {
            this.className = className;
            this.permList = permList;
            this.memberNames = memberNames;
            includeUriFields = false;
            includeAllMethods = false;
            includeInnerClassesForUri = false;
        }

        public CustomPermDef(String className, List<String> permList, boolean includeUriFields) {
            this.className = className;
            this.permList = permList;
            memberNames = null;
            this.includeUriFields = includeUriFields;
            includeAllMethods = false;
            includeInnerClassesForUri = false;
        }

        public CustomPermDef(String className, List<String> permList, List<String> memberNames,
                             boolean includeUriFields, boolean includeAllMethods) {
            this.className = className;
            this.permList = permList;
            this.memberNames = memberNames;
            this.includeUriFields = includeUriFields;
            this.includeAllMethods = includeAllMethods;
            includeInnerClassesForUri = false;
        }

        public CustomPermDef(String className, List<String> permList, List<String> memberNames,
                             boolean includeUriFields, boolean includeAllMethods, boolean includeInnerClassesForUri) {
            this.className = className;
            this.permList = permList;
            this.memberNames = memberNames;
            this.includeUriFields = includeUriFields;
            this.includeAllMethods = includeAllMethods;
            this.includeInnerClassesForUri = includeInnerClassesForUri;
        }
    }
}
