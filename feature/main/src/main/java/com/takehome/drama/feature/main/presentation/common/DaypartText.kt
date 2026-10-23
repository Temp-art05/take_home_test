package com.takehome.drama.feature.main.presentation.common

import androidx.annotation.StringRes
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.feature.main.R

/** Copy của mỗi khung giờ (voice lexicon flow/stream/pour của design). */
@get:StringRes
val Daypart.titleRes: Int
    get() = when (this) {
        Daypart.Morning -> R.string.daypart_morning
        Daypart.Lunch -> R.string.daypart_lunch
        Daypart.Night -> R.string.daypart_night
    }

/** Dòng phụ dưới tiêu đề Home ("21:45 · long episodes, lights off"). */
@get:StringRes
val Daypart.moodRes: Int
    get() = when (this) {
        Daypart.Morning -> R.string.daypart_morning_mood
        Daypart.Lunch -> R.string.daypart_lunch_mood
        Daypart.Night -> R.string.daypart_night_mood
    }

/** Nội dung của khung giờ trên thẻ nhỏ. */
@get:StringRes
val Daypart.pitchRes: Int
    get() = when (this) {
        Daypart.Morning -> R.string.daypart_morning_pitch
        Daypart.Lunch -> R.string.daypart_lunch_pitch
        Daypart.Night -> R.string.daypart_night_pitch
    }

@get:StringRes
val Daypart.pickRes: Int
    get() = when (this) {
        Daypart.Morning -> R.string.home_pick_morning
        Daypart.Lunch -> R.string.home_pick_lunch
        Daypart.Night -> R.string.home_pick_night
    }

@get:StringRes
val Daypart.alsoFlowingRes: Int
    get() = when (this) {
        Daypart.Morning -> R.string.home_also_flowing_morning
        Daypart.Lunch -> R.string.home_also_flowing_lunch
        Daypart.Night -> R.string.home_also_flowing_night
    }
