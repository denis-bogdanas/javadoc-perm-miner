package edu.oregonstate.jdminer.inspect;

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
    public static final ImmutableMap<String, CustomPermDef> classCustomPerm =
            ImmutableMap.<String, CustomPermDef>builder()
                    .put("android.hardware.Camera",
                            new CustomPermDef(Arrays.asList("android.permission.CAMERA"),
                                    Arrays.asList("open")))
                    .put("android.hardware.camera2.CameraDevice", new CustomPermDef(null, null))
                    .put("android.media.audiofx.Visualizer",
                            new CustomPermDef(Arrays.asList("android.permission.RECORD_AUDIO"),
                                    Arrays.asList("<init>")))
                    .put("android.net.rtp.AudioGroup",
                            new CustomPermDef(Arrays.asList("android.permission.RECORD_AUDIO"),
                                    Arrays.asList("<init>")))
                    .put("android.net.sip.SipAudioCall", new CustomPermDef(null, null))
                    .put("android.net.sip.SipManager",
                            new CustomPermDef(Arrays.asList("android.permission.USE_SIP"),
                                    Arrays.asList("createSipSession", "getSessionFor", "open", "makeAudioCall",
                                            "takeAudioCall",
                                            "register")))

                    //removing classes inner that are hidden (@hide)
                    .put("android.provider.ContactsContract", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.AggregationExceptions", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.CommonDataKinds.Callable", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.CommonDataKinds.Contactables", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.CommonDataKinds.Email", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.CommonDataKinds.Phone", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.CommonDataKinds.StructuredPostal", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Contacts", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Data", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.DataUsageFeedback", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.DeletedContacts", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Directory", new CustomPermDef(null, null))
                    .put("android.provider.ContactsContract.DisplayPhoto", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Groups", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.PhoneLookup", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Profile", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.ProfileSyncState", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.ProviderStatus", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.RawContacts", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.RawContactsEntity", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.Settings", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.StatusUpdates", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))
                    .put("android.provider.ContactsContract.SyncState", new CustomPermDef(
                            Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
                            true))

                    .put("android.provider.VoicemailContract", new CustomPermDef(null, null))
                    .put("android.provider.VoicemailContract.Status",
                            new CustomPermDef(Arrays.asList("com.android.voicemail.permission.ADD_VOICEMAIL"), true))
                    .put("android.provider.VoicemailContract.Voicemails",
                            new CustomPermDef(Arrays.asList("com.android.voicemail.permission.ADD_VOICEMAIL"), true))
                    .put("android.speech.SpeechRecognizer",
                            new CustomPermDef(Arrays.asList("android.permission.RECORD_AUDIO"),
                                    Arrays.asList("createSpeechRecognizer")))
                    .put("android.telephony.SubscriptionManager",
                            new CustomPermDef(Arrays.asList("android.permission.READ_PHONE_STATE"), null, false, true))
                    .put("android.telephony.SubscriptionManager.OnSubscriptionsChangedListener",
                            new CustomPermDef(null, null))
                    .build();

    public static List<PermissionDef> getClassPermDefsCoveredByCustomDefs() {
        return classCustomPerm.keySet().stream().map(className ->
                new PermissionDef(XmlPermDefMiner.processInnerClasses(className), null, PermTargetKind.Class, null))
                .collect(Collectors.toList());
    }

    /**
     * Key permission definitions info inside a certain class. Produced by manual processing for classes that don't have
     * permissions mentioned individually for members.
     */
    public static final class CustomPermDef {
        public final List<String> permList;
        public final List<String> methodNames;
        public final boolean includeUriFields;
        public final boolean includeAllMethods;

        public CustomPermDef(List<String> permList, List<String> methodNames) {
            this.permList = permList;
            this.methodNames = methodNames;
            includeUriFields = false;
            includeAllMethods = false;
        }

        public CustomPermDef(List<String> permList, boolean includeUriFields) {
            this.permList = permList;
            methodNames = null;
            this.includeUriFields = includeUriFields;
            includeAllMethods = false;
        }

        public CustomPermDef(List<String> permList, List<String> methodNames, boolean includeUriFields,
                             boolean includeAllMethods) {
            this.permList = permList;
            this.methodNames = methodNames;
            this.includeUriFields = includeUriFields;
            this.includeAllMethods = includeAllMethods;
        }
    }
}
