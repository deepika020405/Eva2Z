package com.developer.ev2z.ui.screens.DriverScore

// ─────────────────────────────────────────────────────────────────────────────
//  Driver Score — Data Models
// ─────────────────────────────────────────────────────────────────────────────

enum class ScoreGrade(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    AVERAGE("Average"),
    POOR("Poor");

    companion object {
        fun fromScore(score: Int): ScoreGrade = when {
            score >= 90 -> EXCELLENT
            score >= 75 -> GOOD
            score >= 50 -> AVERAGE
            else        -> POOR
        }
    }
}

data class ScoreParameter(
    val id: String,
    val label: String,
    val emoji: String,
    val score: Int,
    val weight: Float,
    val eventCount: Int = 0,
    val unit: String = ""
) {
    val grade: ScoreGrade get() = ScoreGrade.fromScore(score)
}

data class TripScore(
    val tripId: String,
    val tripName: String,
    val emoji: String = "🚗",
    val timestamp: String,
    val distanceKm: Int,
    val score: Int
) {
    val grade: ScoreGrade get() = ScoreGrade.fromScore(score)
}

data class DailyScore(
    val dayLabel: String,
    val score: Int,
    val isToday: Boolean = false
)

data class VehicleScore(
    val vehicleId: String,
    val vehicleName: String,
    val vehiclePlate: String,
    val vehicleEmoji: String,
    val vehicleType: String,
    val overallScore: Int,
    val monthOverMonthChange: Int,
    val period: String,
    val parameters: List<ScoreParameter>,
    val recentTrips: List<TripScore>,
    val weeklyTrend: List<DailyScore>,
    val totalTrips: Int,
    val totalKm: Int,
    val safeRatePercent: Int,
    val lastTripTime: String,
    val driverId: String = "",
    val driverName: String = "",
    val isTopDriver: Boolean = false
) {
    val grade: ScoreGrade get() = ScoreGrade.fromScore(overallScore)
}

data class LeaderboardEntry(
    val rank: Int,
    val driverName: String,
    val driverId: String,
    val avatarInitial: String,
    val avatarColorHex: String,
    val score: Int,
    val totalTrips: Int,
    val totalKm: Int,
    val needsAttention: Boolean = false
) {
    val grade: ScoreGrade get() = ScoreGrade.fromScore(score)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sample Data
// ─────────────────────────────────────────────────────────────────────────────

object DriverScoreSampleData {

    val sampleParameters = listOf(
        ScoreParameter("acceleration", "Acceleration",     "🚀", 90, 0.20f, 0),
        ScoreParameter("braking",      "Braking",          "🛑", 72, 0.25f, 3, "events"),
        ScoreParameter("speed",        "Speed Compliance", "📍", 95, 0.30f, 0),
        ScoreParameter("cornering",    "Cornering",        "🔄", 68, 0.15f, 7, "events"),
        ScoreParameter("fuel",         "Fuel Efficiency",  "⛽", 88, 0.10f, 12, "min idle")
    )

    val sampleTrips = listOf(
        TripScore("t1", "Morning Delivery", "🚗", "Today 8:30 AM",     24, 91),
        TripScore("t2", "Afternoon Run",    "🚗", "Yesterday 2:15 PM", 18, 74),
        TripScore("t3", "Night Shift",      "🌙", "2 days ago",         32, 88)
    )

    val sampleWeeklyTrend = listOf(
        DailyScore("Mon",   65),
        DailyScore("Tue",   78),
        DailyScore("Wed",   55),
        DailyScore("Thu",   90),
        DailyScore("Fri",   83),
        DailyScore("Sat",   70),
        DailyScore("Today", 83, isToday = true)
    )

    val hondaCity = VehicleScore(
        vehicleId    = "EVA-MH12AB1234",
        vehicleName  = "Honda City",
        vehiclePlate = "MH 12 AB 1234",
        vehicleEmoji = "🚗",
        vehicleType  = "Car",
        overallScore = 83,
        monthOverMonthChange = 5,
        period       = "February 2026",
        parameters   = sampleParameters,
        recentTrips  = sampleTrips,
        weeklyTrend  = sampleWeeklyTrend,
        totalTrips   = 247,
        totalKm      = 3840,
        safeRatePercent = 92,
        lastTripTime = "Today 8:30 AM",
        driverName   = "Aasha Bhosle",
        driverId     = "EVA-0024",
        isTopDriver  = true
    )

    val bajajPulsar = VehicleScore(
        vehicleId    = "EVA-MH12CD5678",
        vehicleName  = "Bajaj Pulsar",
        vehiclePlate = "MH 12 CD 5678",
        vehicleEmoji = "🏍️",
        vehicleType  = "Bike",
        overallScore = 71,
        monthOverMonthChange = -2,
        period       = "February 2026",
        parameters   = listOf(
            ScoreParameter("acceleration", "Acceleration",     "🚀", 75, 0.20f),
            ScoreParameter("braking",      "Braking",          "🛑", 55, 0.25f, 8),
            ScoreParameter("speed",        "Speed Compliance", "📍", 78, 0.30f, 2),
            ScoreParameter("cornering",    "Cornering",        "🔄", 60, 0.15f, 11),
            ScoreParameter("fuel",         "Fuel Efficiency",  "⛽", 70, 0.10f)
        ),
        recentTrips = listOf(
            TripScore("t4", "City Ride",   "🏍️", "Today 10:15 AM", 18, 68),
            TripScore("t5", "Highway Run", "🏍️", "Yesterday",       45, 74)
        ),
        weeklyTrend = listOf(
            DailyScore("Mon",   70),
            DailyScore("Tue",   65),
            DailyScore("Wed",   72),
            DailyScore("Thu",   68),
            DailyScore("Fri",   75),
            DailyScore("Sat",   69),
            DailyScore("Today", 71, isToday = true)
        ),
        totalTrips   = 198,
        totalKm      = 2950,
        safeRatePercent = 78,
        lastTripTime = "Today 10:15 AM"
    )

    val vegusEV = VehicleScore(
        vehicleId    = "EVA-MH12EV9999",
        vehicleName  = "Eva2z VEGUS EV",
        vehiclePlate = "MH 12 EV 9999",
        vehicleEmoji = "⚡",
        vehicleType  = "EV",
        overallScore = 92,
        monthOverMonthChange = 3,
        period       = "February 2026",
        parameters   = listOf(
            ScoreParameter("acceleration", "Acceleration",     "🚀", 96, 0.20f),
            ScoreParameter("braking",      "Braking",          "🛑", 88, 0.25f, 1),
            ScoreParameter("speed",        "Speed Compliance", "📍", 98, 0.30f),
            ScoreParameter("cornering",    "Cornering",        "🔄", 90, 0.15f, 3),
            ScoreParameter("fuel",         "Fuel Efficiency",  "⛽", 94, 0.10f)
        ),
        recentTrips = listOf(
            TripScore("t6", "Morning Commute", "⚡", "Yesterday 6:00 PM", 31, 94),
            TripScore("t7", "Weekend Trip",    "⚡", "3 days ago",         22, 90)
        ),
        weeklyTrend = listOf(
            DailyScore("Mon",   90),
            DailyScore("Tue",   93),
            DailyScore("Wed",   88),
            DailyScore("Thu",   95),
            DailyScore("Fri",   92),
            DailyScore("Sat",   91),
            DailyScore("Today", 92, isToday = true)
        ),
        totalTrips   = 134,
        totalKm      = 1870,
        safeRatePercent = 97,
        lastTripTime = "Yesterday 6:00 PM"
    )

    val allVehicles = listOf(hondaCity, bajajPulsar, vegusEV)

    val leaderboard = listOf(
        LeaderboardEntry(1, "Aasha Bhosle",  "EVA-0024", "A", "B45309", 83, 247, 3840),
        LeaderboardEntry(2, "Amit Sharma",   "EVA-0031", "A", "1E40AF", 79, 198, 2950),
        LeaderboardEntry(3, "Suresh Patel",  "EVA-0019", "S", "065F46", 76, 162, 2100),
        LeaderboardEntry(4, "Mohan Verma",   "EVA-0045", "M", "4C1D95", 65, 134, 1870),
        LeaderboardEntry(5, "Pradeep Singh", "EVA-0052", "P", "7F1D1D", 48,  89, 1200, needsAttention = true)
    )
}