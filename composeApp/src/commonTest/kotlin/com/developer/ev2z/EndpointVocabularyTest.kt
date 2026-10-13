package com.developer.ev2z

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiJson
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.RouteHistoryResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The gateway describes one tracker in four vocabularies, and the app has to end up
 * with one. These are real payloads, captured on 2026-10-10 from account 2901:
 *
 * | thing     | my-devices    | latest-locations       | live-location                | route-history |
 * |-----------|---------------|------------------------|------------------------------|---------------|
 * | device    | device_id     | device_id              | device_id                    | terminal_id   |
 * | fix time  | —             | timestamp              | timestamp_utc, timestamp_ist | created_at    |
 * | age       | —             | last_seen_minutes_ago  | last_seen_seconds_ago        | —             |
 * | motion    | status        | status                 | status + db_status           | movement      |
 * | speed     | —             | Int                    | Int                          | String        |
 * | SIM       | sim_no        | —                      | —                            | —             |
 *
 * What has to hold is that a rename on one endpoint cannot blank a field, and that a
 * null cannot cost more than the field it is on.
 */
class EndpointVocabularyTest {

    /** The exact body that used to throw and take the coordinates down with it. */
    private val liveLocationJson = """
        {"success":true,"data":{"latest_location":{
          "device_id":"0356218606632279","latitude":"28.60788","longitude":"77.425053",
          "speed":20,"status":"running","db_status":"moving",
          "timestamp_utc":"2026-10-10 07:10:14","timestamp_ist":"2026-10-10 12:40:14 PM",
          "battery_level":null,"battery_display":"0%","battery_status":"No Power",
          "last_seen_seconds_ago":9},
          "status_count":{"idle":0,"running":35,"offline":35}}}
    """.trimIndent()

    @Test
    fun aNulledFieldCostsThatFieldAndNotTheFix() {
        val fix = ApiJson.decodeFromString(LiveLocationResponse.serializer(), liveLocationJson)
            .data?.latestLocation

        assertNotNull(fix, "a null battery_level must not discard the whole response")
        assertEquals(28.60788 to 77.425053, fix.validPosition())
        assertEquals(null, fix.batteryLevel)
        // The rest of the battery block still arrives — only the nulled field is lost.
        assertEquals("No Power", fix.batteryStatus)
    }

    @Test
    fun theFixTimeSurvivesWhicheverNameItArrivesUnder() {
        val fix = ApiJson.decodeFromString(LiveLocationResponse.serializer(), liveLocationJson)
            .data?.latestLocation
        assertNotNull(fix)

        // Sent as timestamp_utc here...
        assertEquals("2026-10-10 07:10:14", fix.timestamp)
        assertEquals("2026-10-10 12:40:14 PM", fix.timestampIst)

        // ...and as plain `timestamp` by the endpoint this model used to be written for.
        val legacy = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"timestamp":"2026-10-10 07:10:14"}}}""",
        ).data?.latestLocation
        assertEquals("2026-10-10 07:10:14", legacy?.timestamp)
    }

    @Test
    fun ageIsReadInMinutesWhicheverUnitTheServerCounted() {
        val seconds = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"last_seen_seconds_ago":90}}}""",
        ).data?.latestLocation
        val minutes = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"last_seen_minutes_ago":7}}}""",
        ).data?.latestLocation

        // 90 seconds is one minute, not ninety — the reason the unit is kept apart
        // from the name instead of being aliased to it.
        assertEquals(1, seconds?.lastSeenMinutesAgo)
        assertEquals(7, minutes?.lastSeenMinutesAgo)
    }

    @Test
    fun oneMotionStateHasOneNameAcrossAllFourEndpoints() {
        val fleet = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"user_id":1,"name":"T","total_devices":1,"running_count":1,
               "idle_count":0,"parked_count":0,"offline_count":0,
               "data":[{"device_id":"D1","device_name":"T","latitude":"28.6","longitude":"77.4",
               "speed":20,"status":"running","timestamp":"2026-10-10 07:10:14",
               "last_seen_minutes_ago":0}]}""",
        ).data.first()
        val live = ApiJson.decodeFromString(LiveLocationResponse.serializer(), liveLocationJson)
            .data?.latestLocation
        val history = ApiJson.decodeFromString(
            RouteHistoryResponse.serializer(),
            """{"success":true,"terminal_id":"D1","average_speed":40.5,
               "data":[{"latitude":"28.6","longitude":"77.4","speed":"20",
               "created_at":"2026-10-10 07:10:14","movement":"moving"}]}""",
        ).data.first()

        // "running", "running"/"moving" and "movement":"moving" are the same state.
        assertEquals("running", fleet.getStatusKind())
        assertEquals("running", live?.statusKind())
        assertEquals("running", history.statusKind())
    }

    @Test
    fun aDeviceIsTheSameDeviceWhenItIsCalledATerminal() {
        val history = ApiJson.decodeFromString(
            RouteHistoryResponse.serializer(),
            """{"success":true,"terminal_id":"0356218606632279","data":[]}""",
        )
        assertEquals("0356218606632279", history.terminalId)

        // The same response written with the name every other endpoint uses.
        val renamed = ApiJson.decodeFromString(
            RouteHistoryResponse.serializer(),
            """{"success":true,"device_id":"0356218606632279","data":[]}""",
        )
        assertEquals("0356218606632279", renamed.terminalId)
    }

    @Test
    fun theSimIsReadUnderBothOfItsNames() {
        fun simOf(body: String) = ApiJson
            .decodeFromString(MyDevicesResponse.serializer(), body)
            .devices.first().sim_no

        assertEquals(
            "89911026034096209649U",
            simOf("""{"success":true,"devices":[{"sim_no":"89911026034096209649U"}]}"""),
        )
        assertEquals(
            "89911026034096209649U",
            simOf("""{"success":true,"devices":[{"sim_number":"89911026034096209649U"}]}"""),
        )
    }

    @Test
    fun ignitionIsNotFoldedIntoMotion() {
        // my-devices sends engine_status "unknown" next to status "moving" for a
        // running vehicle, so the two must stay separate fields.
        val device = ApiJson.decodeFromString(
            MyDevicesResponse.serializer(),
            """{"success":true,"devices":[{"device_id":"D1","device_name":"T",
               "engine_status":"unknown","status":"moving"}]}""",
        ).devices.first()

        assertEquals("unknown", device.engine_status)
        assertEquals("running", device.statusKind())
    }

    @Test
    fun theVehicleIconIsReadAsASlugAndSurvivesHavingNoVehicle() {
        fun fixWith(iconUrl: String) = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"latitude":"12.900704",
               "longitude":"77.641759","icon_url":$iconUrl}}}""",
        ).data?.latestLocation

        // As live-location spells it — underscores, and a URL that 404s as sent.
        val assigned = fixWith(""""https://gps.eva2z.in/api/vehicle-icons/electric_scooter.svg"""")
        // As vehicle-types spells the identical type.
        val catalogue = fixWith(""""https://gps.eva2z.in/api/vehicle-icons/electric-scooter.svg"""")

        // One slug for both, so a single icon table serves the two endpoints.
        assertEquals("electric-scooter", assigned?.iconSlug)
        assertEquals("electric-scooter", catalogue?.iconSlug)

        // A device with no vehicle assigned sends null here, and must still arrive
        // with its coordinates — the battery_level failure all over again otherwise.
        val unassigned = fixWith("null")
        assertNotNull(unassigned)
        assertEquals(null, unassigned.iconUrl)
        assertEquals(null, unassigned.iconSlug)
        assertEquals(12.900704 to 77.641759, unassigned.validPosition())
    }

    /**
     * One field, two names, depending on which endpoint you ask.
     *
     * Verified against the live API on 2026-10-13: `live-location/{id}` sends
     * `icon_url`, `route-history` sends `vehicle_icon_url`, and
     * `user/devices/latest-locations` sends neither — it has no icon field at all. Both
     * spellings are accepted everywhere, so a model copied from the wrong neighbour, or
     * a gateway that swaps the names, cannot silently blank the vehicle.
     */
    @Test
    fun theVehicleIconIsReadUnderEitherKey() {
        fun fixWith(key: String) = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"latitude":"12.900704",
               "longitude":"77.641759",
               "$key":"https://gps.eva2z.in/api/vehicle-icons/electric-scooter.svg"}}}""",
        ).data?.latestLocation

        assertEquals("electric-scooter", fixWith("vehicle_icon_url")?.iconSlug)
        assertEquals("electric-scooter", fixWith("icon_url")?.iconSlug)

        // The fleet payload gets the same treatment, so the home map picks the vehicle
        // up under whichever name it starts sending.
        fun deviceWith(key: String) = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","latitude":"12.900704",
               "longitude":"77.641759",
               "$key":"https://gps.eva2z.in/api/vehicle-icons/tourist-bus.svg"}]}""",
        ).data.first()

        assertEquals("tourist-bus", deviceWith("vehicle_icon_url").iconSlug)
        assertEquals("tourist-bus", deviceWith("icon_url").iconSlug)
    }

    /**
     * `vehicle_id` is read on all three endpoints, and beats the slug on each.
     *
     * The payloads below are the real ones as of 2026-10-13: `latest-locations` gained
     * `vehicle_id`, `vehicle_name`, `icon` and `icon_url` together; `route-history` has
     * `vehicle_id` beside `vehicle_icon_url`; `live-location` still sends only
     * `icon_url`, which is why it is pinned here with an id it does not send yet — the
     * day the gateway adds it, this test says the app already used it.
     */
    @Test
    fun theVehicleIdIsReadEverywhereAndOutranksTheSlug() {
        // Fleet: id 5 is "bus"; the slug deliberately disagrees and must lose.
        val fleet = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","latitude":"12.9","longitude":"77.6",
               "vehicle_id":5,"vehicle_name":"Bus","icon":"motorcycle",
               "icon_url":"https://gps.eva2z.in/api/vehicle-icons/motorcycle.svg"}]}""",
        ).data.first()
        assertEquals("bus", fleet.iconSlug)
        assertEquals("Bus", fleet.vehicleName)

        // With no id, the standalone slug is next — ahead of digging it out of the URL.
        val byIcon = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","icon":"electric_scooter"}]}""",
        ).data.first()
        assertEquals("electric-scooter", byIcon.iconSlug)

        // A device with no vehicle assigned: every one of the four arrives null, and
        // the row — coordinates included — still has to survive it.
        val unassigned = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","latitude":"12.9","longitude":"77.6",
               "vehicle_id":null,"vehicle_name":null,"icon":null,"icon_url":null}]}""",
        ).data.first()
        assertEquals(null, unassigned.iconSlug)
        assertEquals(12.9, unassigned.getLatitudeDouble())

        // Route history, which sends the id next to its own spelling of the URL.
        val history = ApiJson.decodeFromString(
            RouteHistoryResponse.serializer(),
            """{"success":true,"terminal_id":"D1","vehicle_id":35,
               "vehicle_icon_url":"https://gps.eva2z.in/api/vehicle-icons/car.svg",
               "data":[]}""",
        )
        assertEquals("water-tanker", history.vehicleIconSlug)

        // Live location, which does not send the id yet — today the URL is all it has.
        fun live(body: String) = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"latitude":"12.9",
               "longitude":"77.6",$body}}}""",
        ).data?.latestLocation
        assertEquals(
            "motorcycle",
            live(""""icon_url":"https://gps.eva2z.in/api/vehicle-icons/motorcycle.svg"""")?.iconSlug,
        )
        assertEquals("tractor", live(""""vehicle_id":10""")?.iconSlug)
    }

    /**
     * The fleet map draws the same vehicles as the single-vehicle map, so it has to
     * read the vehicle type the same way — including the underscores this gateway
     * spells the slug with on one endpoint and not the other.
     */
    @Test
    fun theFleetDeviceReadsItsVehicleTheSameWayTheLiveFixDoes() {
        fun deviceWith(iconUrl: String) = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","latitude":"12.900704",
               "longitude":"77.641759","icon_url":$iconUrl}]}""",
        ).data.first()

        assertEquals(
            "electric-scooter",
            deviceWith(""""https://gps.eva2z.in/api/vehicle-icons/electric_scooter.svg"""").iconSlug,
        )

        // What the fleet endpoint actually sends today: no icon_url at all, which has
        // to leave the device — coordinates included — intact.
        val unassigned = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"data":[{"device_id":"D1","latitude":"12.900704",
               "longitude":"77.641759"}]}""",
        ).data.first()
        assertEquals(null, unassigned.iconSlug)
        assertEquals(12.900704, unassigned.getLatitudeDouble())

        // And a device the server explicitly nulls it on: the battery_level failure
        // all over again otherwise.
        assertEquals(null, deviceWith("null").iconSlug)
    }

    @Test
    fun theFleetResponseParsesAsTheServerSendsIt() {
        // Verbatim from `user/devices/latest-locations`, account 2826, trimmed to two
        // of its three devices.
        val fleet = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"user_id":2826,"username":"Neetesh Nayak",
               "name":"Neetesh Nayak","total_devices":3,"running_count":0,
               "idle_count":0,"parked_count":0,"offline_count":3,"data":[
                 {"device_id":"0356218606631586","device_name":"ka 03 AJ 9053",
                  "latitude":"12.900704","longitude":"77.641759","speed":0,
                  "status":"offline","timestamp":"2026-10-10 05:43:50",
                  "last_seen_minutes_ago":343},
                 {"device_id":"0356218606630471","device_name":"KA 03 AK 5637",
                  "latitude":"12.913599","longitude":"77.678641","speed":0,
                  "status":"offline","timestamp":"2026-10-10 09:32:50",
                  "last_seen_minutes_ago":114}]}""",
        )

        assertEquals(3, fleet.totalDevices)
        assertEquals("Neetesh Nayak", fleet.displayName)
        assertEquals(2, fleet.data.size)
        assertEquals(12.900704 to 77.641759, fleet.data.first().getLatitudeDouble()!! to
                fleet.data.first().getLongitudeDouble()!!)
        assertEquals("offline", fleet.data.first().getStatusKind())
        assertEquals(343, fleet.data.first().lastSeenMinutes())
    }

    @Test
    fun oneBadFieldDoesNotCostTheWholeFleet() {
        // The counts nulled and a device missing its name — the shape this response
        // takes when something upstream goes wrong. The map must still get its
        // devices rather than going blank.
        val fleet = ApiJson.decodeFromString(
            DeviceResponse.serializer(),
            """{"success":true,"user_id":2826,"username":"Neetesh Nayak",
               "running_count":null,"offline_count":null,"data":[
                 {"device_id":"0356218606631586","latitude":"12.900704",
                  "longitude":"77.641759","status":null,"speed":null}]}""",
        )

        assertEquals(1, fleet.data.size)
        assertEquals(0, fleet.runningCount)
        assertEquals("Neetesh Nayak", fleet.displayName)
        // name was absent, so displayName fell through to username.
        assertEquals("", fleet.name)
        val device = fleet.data.first()
        assertEquals(0, device.speed)
        assertEquals("offline", device.getStatusKind())
        assertEquals(12.900704 to 77.641759, device.getLatitudeDouble()!! to device.getLongitudeDouble()!!)
    }

    @Test
    fun anUnknownKeyIsNotAFailedResponse() {
        // The server adds fields without warning — db_status and timestamp_ist both
        // appeared this way. A new one must cost nothing.
        val fix = ApiJson.decodeFromString(
            LiveLocationResponse.serializer(),
            """{"success":true,"data":{"latest_location":{"latitude":"28.6","longitude":"77.4",
               "something_new":{"nested":true}}}}""",
        ).data?.latestLocation

        assertNotNull(fix)
        assertEquals(28.6 to 77.4, fix.validPosition())
    }
}
