package org.opentripplanner.routing.alertpatch;

import com.google.common.collect.ImmutableList;
import com.google.transit.realtime.GtfsRealtime;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

public class Alert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    public enum AlertId {
        CAR_PARK_FULL("car_park_full"),
        CAR_PARK_CLOSING_SOON("car_closing_soone"),
        BIKE_RENTAL_FREE_FLOATING_DROP_OFF("bike_rental_free_floating_drop_off");

        public String value;

        AlertId(String value) {
            this.value = value;
        }
    }

    public I18NString alertHeaderText;

    public I18NString alertDescriptionText;

    public I18NString alertUrl;

    public AlertId alertId;

    //null means unknown
    public Date effectiveStartDate;

    //null means unknown
    public Date effectiveEndDate;

    public GtfsRealtime.Alert.Effect effect;

    public GtfsRealtime.Alert.Cause cause;

    public GtfsRealtime.Alert.SeverityLevel severityLevel;

    public static HashSet<Alert> newSimpleAlertSet(String text) {
        Alert note = createSimpleAlerts(text);
        HashSet<Alert> notes = new HashSet<Alert>(1);
        notes.add(note);
        return notes;
    }

    public static Alert createSimpleAlerts(String text) {
        Alert note = new Alert();
        note.alertHeaderText = new NonLocalizedString(text);
        return note;
    }

    public static Alert createFloatingDropOffAlert() {
        var alert = createTranslatedAlert("bicycle_rental.free_floating_dropoff");
        alert.alertId = AlertId.BIKE_RENTAL_FREE_FLOATING_DROP_OFF;
        return alert;
    }

    public static Alert createLowCarParkSpacesAlert() {
        Alert alert = createTranslatedAlert("car_park.full");
        alert.alertId = AlertId.CAR_PARK_FULL;
        return alert;
    }

    public static Alert createCarParkClosingSoonAlert() {
        Alert alert = createTranslatedAlert("car_park.closing_soon");
        alert.alertId = AlertId.CAR_PARK_CLOSING_SOON;
        return alert;
    }

    private static List<Locale> locales = ImmutableList.of(Locale.ENGLISH, Locale.GERMAN);

    private static Alert createTranslatedAlert(String translationKey) {
        var alert = new Alert();
        alert.alertHeaderText = getTranslatedString("alert." + translationKey + ".header");
        alert.alertDescriptionText = getTranslatedString("alert." + translationKey + ".description");
        return alert;
    }

    private static I18NString getTranslatedString(String key) {
        var translations = new HashMap<String, String >();
        locales.forEach(l -> translations.put(l.toLanguageTag().toLowerCase(), ResourceBundleSingleton.INSTANCE.localize(key, l)));
        return TranslatedString.getI18NString(translations);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Alert)) {
            return false;
        }
        Alert ao = (Alert) o;
        if (alertDescriptionText == null) {
            if (ao.alertDescriptionText != null) {
                return false;
            }
        } else {
            if (!alertDescriptionText.equals(ao.alertDescriptionText)) {
                return false;
            }
        }
        if (alertHeaderText == null) {
            if (ao.alertHeaderText != null) {
                return false;
            }
        } else {
            if (!alertHeaderText.equals(ao.alertHeaderText)) {
                return false;
            }
        }
        if (alertUrl == null) {
            return ao.alertUrl == null;
        } else {
            return alertUrl.equals(ao.alertUrl);
        }
    }

    public int hashCode() {
        return (alertDescriptionText == null ? 0 : alertDescriptionText.hashCode())
                + (alertHeaderText == null ? 0 : alertHeaderText.hashCode())
                + (alertUrl == null ? 0 : alertUrl.hashCode());
    }

    @Override
    public String toString() {
        return "Alert('"
                + (alertHeaderText != null ? alertHeaderText.toString()
                : alertDescriptionText != null ? alertDescriptionText.toString()
                : "?") + "')";
    }

    public int getHashWithoutInformedEntities() {
        return Objects.hash(alertDescriptionText, alertHeaderText, alertUrl, cause, effect, severityLevel);
    }
}
