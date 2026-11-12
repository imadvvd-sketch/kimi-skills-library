package app.ghars.service

/** Decides whether the user has wandered off to another app. Pure, so it can be tested. */
object LeavePolicy {
    /**
     * Away means: our app is not on screen, yet the screen is on, unlocked, and no call is in progress.
     * Screen off is the goal, not a violation. A locked screen means they only glanced at the phone.
     */
    fun isAway(
        appInForeground: Boolean,
        screenInteractive: Boolean,
        keyguardLocked: Boolean,
        inCall: Boolean,
    ): Boolean = !appInForeground && screenInteractive && !keyguardLocked && !inCall
}
