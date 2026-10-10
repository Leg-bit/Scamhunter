
package com.legbit.scamhunter

import android.telecom.Call
import android.telecom.CallScreeningService

class CallGuardService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val blocked = number.isNotBlank() &&
            SpamDetector.isBlocked(this, number)

        val response = CallResponse.Builder()
            .setDisallowCall(blocked)
            .setRejectCall(blocked)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}
