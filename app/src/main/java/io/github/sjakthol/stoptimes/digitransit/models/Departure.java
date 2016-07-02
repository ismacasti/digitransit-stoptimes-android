package io.github.sjakthol.stoptimes.digitransit.models;

import android.text.TextUtils;
import io.github.sjakthol.stoptimes.R;
import io.github.sjakthol.stoptimes.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

public final class Departure {
    private static final String TAG = Departure.class.getSimpleName();
    private final String mHeadsign;
    private final String mRoute;
    private final VehicleType mRouteType;
    private final boolean mRealtime;
    private final Timestamp mRealtimeDeparture;
    private final Timestamp mScheduledDeparture;

    private Departure(String route, VehicleType routeType, String sign, Timestamp sDep, Timestamp rtDep, boolean isRt) {
        mRoute = fixRoute(route, routeType);

        mRouteType = routeType;
        mHeadsign = sign;
        mScheduledDeparture = sDep;
        mRealtimeDeparture = rtDep;
        mRealtime = isRt;
    }

    /**
     * Parses the given JSON object into a Departure object. The object should have following form:
     * {
     *   "realtime": false,
     *   "serviceDay": 1465851600,
     *   "scheduledDeparture": 64620,
     *   "realtimeDeparture": 64620,
     *   "trip": {
     *     "route": {
     *       "shortName": "A"
     *       "type": "RAIL"
     *     },
     *     "tripHeadsign": "Helsinki"
     *   }
     * }
     *
     * }
     * @param obj the JSON object to parse
     * @return a new Departure object
     * @throws JSONException if the JSON is not valid, unexpected or is missing some fields
     */
    public static Departure fromJsonObject(JSONObject obj) throws JSONException {
        JSONObject trip = obj.getJSONObject("trip");
        JSONObject route = trip.getJSONObject("route");
        long day = obj.getLong("serviceDay");
        long scheduled = obj.getLong("scheduledDeparture");
        long realtime = obj.getLong("realtimeDeparture");

        return new Departure(
            route.getString("shortName"),
            routeToVehicleType(route.getString("type")),
            trip.getString("tripHeadsign"),
            new Timestamp((day + scheduled) * 1000),
            new Timestamp((day + realtime) * 1000),
            obj.getBoolean("realtime")
        );
    }

    /**
     * Ensures that the route is not null / empty.
     *
     * @param route the route code
     * @param routeType the type of the route
     * @return @param route if route is not null, "null" or ""; otherwise "M" for SUBWAY and "?" for others.
     */
    private String fixRoute(String route, VehicleType routeType) {
        if (TextUtils.isEmpty(route) || route.equals("null")) {
            // In some cases route name can be null (e.g. subway).
            if (routeType == VehicleType.SUBWAY) {
                return "M";
            } else {
                return "?";
            }
        }

        // Route was defined; use it
        return route;
    }

    /**
     * Convert the strings BUS, TRAM, RAIL and SUBWAY into corresponding route
     * type. Unknown type is converted to BUS.
     *
     * @param type the route type string
     *
     * @return a route type constant
     */
    static VehicleType routeToVehicleType(String type) {
        switch (type) {
            case "BUS":
                return VehicleType.BUS;
            case "TRAM":
                return VehicleType.TRAM;
            case "RAIL":
                return VehicleType.COMMUTER_TRAIN;
            case "SUBWAY":
                return VehicleType.SUBWAY;
            default:
                Logger.w(TAG, String.format("Unknown route type '%s'", type));
                return VehicleType.BUS;
        }
    }

    /**
     * Get the route code of this departure (e.g. 550 or U)
     *
     * @return the route code
     */
    public String getRoute() {
        return mRoute;
    }

    /**
     * Get a color corresponding the given route based on the route type (HSL colors).
     *
     * @return a color resource reference
     */
    public int getRouteColor() {
        return getRouteColor(mRouteType);
    }

    /**
     * Get a vehicle color for the route of this departure (HSL colors).
     *
     * @param type a route type
     *
     * @return a color resource
     */
    @SuppressWarnings("WeakerAccess")
    int getRouteColor(VehicleType type) {
        switch (type) {
            case BUS:
                return R.color.hsl_bus;
            case TRAM:
                return R.color.hsl_tram;
            case COMMUTER_TRAIN:
                return R.color.hsl_commuter_train;
            case SUBWAY:
                return R.color.hsl_subway;
            default:
                throw new RuntimeException("Non-conclusive switch!");
        }
    }

    /**
     * Get the headsign i.e. the destination of this departure.
     *
     * @return headsign
     */
    public String getHeadsign() {
        return mHeadsign;
    }

    /**
     * Get the time the departure is scheduled to happen.
     *
     * @return timestamp
     */
    public Timestamp getScheduledDeparture() {
        return mScheduledDeparture;
    }

    /**
     * Get the time the departure is about to happen based on realtime information. If realtime information is not
     * available this will be the same as scheduled departure.
     *
     * @return realtime departure timestamp
     */
    public Timestamp getRealtimeDeparture() {
        return mRealtimeDeparture;
    }

    /**
     * Check if the information is realtime.
     *
     * @return true if getRealtimeDeparture() contains realtime prediction about the departure; false otherwise
     */
    public boolean isRealtime() {
        return mRealtime;
    }
}
