package com.nimmaguru.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Nimma Guru.
 * Based on idea-architecture.md Section 4.
 *
 * P-NAV-01: @Serializable requires kotlinx.serialization plugin.
 * P-NAV-02: Pass only IDs, not full objects, via navigation args.
 */

// Bottom nav tabs
@Serializable object HomeRoute
@Serializable object DiscoverRoute
@Serializable object CalendarRoute
@Serializable object ProfileRoute

// Detail screens
@Serializable data class GuruDetailRoute(val guruId: String)
@Serializable data class SessionDetailRoute(val sessionId: String, val guruId: String)
@Serializable data class PostAppreciationRoute(val guruId: String)

// Auth
@Serializable object PhoneEntryRoute
@Serializable data class OtpVerifyRoute(val phoneNumber: String)
@Serializable object BasicOnboardingRoute
@Serializable object GuruOnboardingRoute

// Sessions
@Serializable object CreateSessionRoute
