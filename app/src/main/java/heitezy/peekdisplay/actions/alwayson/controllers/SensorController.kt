package heitezy.peekdisplay.actions.alwayson.controllers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorController(
    context: Context,
    private val onProximityChanged: (Boolean) -> Unit
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                val isProximate = event.values[0] != event.sensor.maximumRange
                onProximityChanged(isProximate)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start(delay: Int) {
        proximitySensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                delay,
                delay
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorEventListener)
    }
}
