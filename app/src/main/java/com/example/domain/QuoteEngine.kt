package com.example.domain

object QuoteEngine {
    private val quotes = listOf(
        "Small disciplines repeated with consistency every day lead to great achievements.",
        "Your future is created by what you do today, not tomorrow.",
        "The secret of your future is hidden in your daily routine.",
        "Success is the sum of small efforts, repeated day in and day out.",
        "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
        "Motivation is what gets you started. Habit is what keeps you going.",
        "It’s not what we do once in a while that shapes our lives. It’s what we do consistently.",
        "Don't decrease the goal. Increase the effort.",
        "The only way to achieve the impossible is to believe it is possible.",
        "Great things are not done by impulse, but by a series of small things brought together.",
        "Atomic habits are the building blocks of remarkable results.",
        "You do not rise to the level of your goals. You fall to the level of your systems."
    )

    fun getDailyQuote(): String {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }
}
