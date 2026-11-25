package com.example.data.model

data class HabitTemplate(
    val name: String,
    val description: String,
    val category: String,
    val colorHex: String,
    val iconName: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val recommendedHour: Int = 9,
    val recommendedMinute: Int = 0,
    val emoji: String = "✨"
)

object HabitTemplateLibrary {
    val templates: List<HabitTemplate> = listOf(
        // Health & Vitality
        HabitTemplate(
            name = "Drink 8 Glasses of Water",
            description = "Stay hydrated and energize cellular metabolism throughout the day",
            category = "Health",
            colorHex = "#0EA5E9",
            iconName = "WaterDrop",
            recommendedHour = 8,
            recommendedMinute = 0,
            emoji = "💧"
        ),
        HabitTemplate(
            name = "8 Hours Restorative Sleep",
            description = "Wind down early, sleep uninterrupted, and wake up fully refreshed",
            category = "Health",
            colorHex = "#6366F1",
            iconName = "Bedtime",
            recommendedHour = 22,
            recommendedMinute = 30,
            emoji = "😴"
        ),
        HabitTemplate(
            name = "No Processed Sugar",
            description = "Eliminate soda, candy, and refined sugars for stable energy and mental clarity",
            category = "Health",
            colorHex = "#E11D48",
            iconName = "DoNotDisturb",
            recommendedHour = 10,
            recommendedMinute = 0,
            emoji = "🚫"
        ),
        HabitTemplate(
            name = "Take Daily Vitamins & Minerals",
            description = "Maintain essential micronutrients and strengthen immune defense",
            category = "Health",
            colorHex = "#10B981",
            iconName = "HealthAndSafety",
            recommendedHour = 8,
            recommendedMinute = 30,
            emoji = "💊"
        ),

        // Fitness & Movement
        HabitTemplate(
            name = "Morning 30-Min Workout",
            description = "Full body resistance training or cardio to prime body and dopamine",
            category = "Fitness",
            colorHex = "#FF5722",
            iconName = "FitnessCenter",
            recommendedHour = 7,
            recommendedMinute = 0,
            emoji = "🏋️"
        ),
        HabitTemplate(
            name = "10,000 Steps Daily",
            description = "Hit your daily step goal through brisk walks, stairs, and active pacing",
            category = "Fitness",
            colorHex = "#F59E0B",
            iconName = "DirectionsWalk",
            recommendedHour = 17,
            recommendedMinute = 0,
            emoji = "🚶"
        ),
        HabitTemplate(
            name = "Post-Work Posture & Stretch",
            description = "10 minutes of yoga, spine mobility, and posture decompressing",
            category = "Fitness",
            colorHex = "#14B8A6",
            iconName = "SelfImprovement",
            recommendedHour = 18,
            recommendedMinute = 30,
            emoji = "🧘"
        ),

        // Mindfulness & Mental Health
        HabitTemplate(
            name = "10-Minute Mindfulness Meditation",
            description = "Observe breath, calm amygdala, and cultivate razor-sharp present focus",
            category = "Mind",
            colorHex = "#8B5CF6",
            iconName = "Spa",
            recommendedHour = 7,
            recommendedMinute = 30,
            emoji = "🧘"
        ),
        HabitTemplate(
            name = "Evening Gratitude & Reflection",
            description = "Write down 3 things you are genuinely grateful for today",
            category = "Mind",
            colorHex = "#EC4899",
            iconName = "EditNote",
            recommendedHour = 21,
            recommendedMinute = 30,
            emoji = "✍️"
        ),
        HabitTemplate(
            name = "Screen-Free Wind Down",
            description = "Disconnect from all phones, tablets, and screens 45 minutes before sleep",
            category = "Mind",
            colorHex = "#475569",
            iconName = "PhoneAndroid",
            recommendedHour = 22,
            recommendedMinute = 0,
            emoji = "📵"
        ),

        // Learning & Growth
        HabitTemplate(
            name = "Read 20 Pages of Non-Fiction",
            description = "Expand domain knowledge, strategic thinking, and reading stamina",
            category = "Learning",
            colorHex = "#059669",
            iconName = "MenuBook",
            recommendedHour = 21,
            recommendedMinute = 0,
            emoji = "📚"
        ),
        HabitTemplate(
            name = "Practice Foreign Language",
            description = "15 minutes of vocabulary drills, listening, and active speaking",
            category = "Learning",
            colorHex = "#2563EB",
            iconName = "Translate",
            recommendedHour = 19,
            recommendedMinute = 0,
            emoji = "🗣️"
        ),
        HabitTemplate(
            name = "Daily Deliberate Practice",
            description = "Hone technical coding, musical instrument, or creative craftsmanship",
            category = "Learning",
            colorHex = "#D97706",
            iconName = "Psychology",
            recommendedHour = 16,
            recommendedMinute = 0,
            emoji = "🎯"
        ),

        // Productivity & Discipline
        HabitTemplate(
            name = "90-Min Deep Work Block",
            description = "Zero notifications, single-task deep focus on your #1 highest leverage task",
            category = "Productivity",
            colorHex = "#3B82F6",
            iconName = "HourglassTop",
            recommendedHour = 9,
            recommendedMinute = 0,
            emoji = "💻"
        ),
        HabitTemplate(
            name = "Plan Tomorrow's Big 3",
            description = "List your 3 mission-critical priorities for tomorrow before wrapping up",
            category = "Productivity",
            colorHex = "#7C3AED",
            iconName = "Checklist",
            recommendedHour = 18,
            recommendedMinute = 0,
            emoji = "📝"
        ),
        HabitTemplate(
            name = "Declutter Desk & Workspace",
            description = "Clean, clear visual environment for clean, distraction-free thinking",
            category = "Productivity",
            colorHex = "#0D9488",
            iconName = "CleaningServices",
            recommendedHour = 17,
            recommendedMinute = 30,
            emoji = "✨"
        )
    )

    fun getByCategory(category: String): List<HabitTemplate> {
        return templates.filter { it.category.equals(category, ignoreCase = true) }
    }
}
