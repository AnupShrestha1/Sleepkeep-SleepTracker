package com.anupshrestha.sleepkeep.guidance

/**
 * Public adult sleep guidance, stored in the app so it works offline. Checked 2026-10-28; the
 * dates are the ones shown on each page at that time.
 */
object SleepGuidelines {
    /** Every CDC adult age group (18–60, 61–64, 65+) starts at 7 hours; NHLBI says 7–9. */
    const val MIN_ADULT_SLEEP_MILLIS = 7 * 60 * 60_000L

    /** Weekly figures need at least this many nights in the last 7 days. */
    const val MIN_NIGHTS = 3
    const val WINDOW_DAYS = 7

    data class Source(val name: String, val title: String, val url: String, val pageDate: String)

    val sources = listOf(
        Source(
            name = "CDC",
            title = "About Sleep",
            url = "https://www.cdc.gov/sleep/about/index.html",
            pageDate = "May 15, 2024",
        ),
        Source(
            name = "NHLBI",
            title = "How Much Sleep Is Enough?",
            url = "https://www.nhlbi.nih.gov/health/sleep/how-much-sleep",
            pageDate = "March 24, 2022",
        ),
    )
    const val CHECKED_ON = "October 28, 2026"
}
