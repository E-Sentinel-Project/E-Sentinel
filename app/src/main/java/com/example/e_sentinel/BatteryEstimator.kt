package com.example.e_sentinel

import kotlin.math.roundToInt

object BatteryEstimator {

    // -----------------------------
    // Assumed conservative power values (mW)
    // -----------------------------
    private const val POWER_IDLE_MW = 50.0
    private const val POWER_SENSOR_MW = 150.0
    private const val POWER_INFERENCE_MW = 300.0
    private const val POWER_NETWORK_MW = 800.0

    // -----------------------------
    // Duty cycle ratios (must sum to 1.0)
    // -----------------------------
    private const val DUTY_IDLE = 0.85
    private const val DUTY_SENSOR = 0.10
    private const val DUTY_INFERENCE = 0.04
    private const val DUTY_NETWORK = 0.01

    // Nominal smartphone battery voltage (V)
    private const val BATTERY_VOLTAGE = 3.8

    /**
     * Computes average power consumption (mW)
     */
    fun computeAveragePowerMw(): Double {
        return (POWER_IDLE_MW * DUTY_IDLE) +
                (POWER_SENSOR_MW * DUTY_SENSOR) +
                (POWER_INFERENCE_MW * DUTY_INFERENCE) +
                (POWER_NETWORK_MW * DUTY_NETWORK)
    }

    /**
     * Computes estimated battery drain per hour (mAh)
     */
    fun computeBatteryDrainPerHourMah(): Double {
        val avgPowerMw = computeAveragePowerMw()
        return avgPowerMw / BATTERY_VOLTAGE
    }

    /**
     * Pretty formatted string for logs / paper
     */
    fun getReport(): String {
        val power = computeAveragePowerMw()
        val drain = computeBatteryDrainPerHourMah()

        return """
            Battery Estimation Report
            -------------------------
            Avg Power Consumption : ${"%.2f".format(power)} mW
            Estimated Drain Rate  : ${"%.2f".format(drain)} mAh/hour
            Duty Cycle Model Used : YES
        """.trimIndent()
    }
}
