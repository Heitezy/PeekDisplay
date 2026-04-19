package heitezy.peekdisplay.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.P

class PickUpService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenOffWakeLock: PowerManager.WakeLock? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null

    private val WAKELOCK_TAG = "PickUp:WakeLock"
    private val NOTIFICATION_ID = 1

    private val filteredGravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)

    private var pickUpThreshold = 2.0
    private var proximityNear = 0

    private var hasProximitySensor = false
    
    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        hasProximitySensor = proximitySensor != null

        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, 
            WAKELOCK_TAG
        )

        screenOffWakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            "$WAKELOCK_TAG:ScreenOn"
        )

        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "$WAKELOCK_TAG:Proximity"
        )

        loadSettings()

        startForegroundService()

        registerSensors()
    }
    
    private fun loadSettings() {
        val prefs = P(getDefaultSharedPreferences(this))
        when (prefs.get(P.PICKUP_SENSITIVITY, P.PICKUP_SENSITIVITY_DEFAULT)) {
            "1" -> pickUpThreshold = 1.5
            "2" -> pickUpThreshold = 2.0
            "3" -> pickUpThreshold = 3.0
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, ForegroundService.CHANNEL_ID)
            .setContentText(getString(R.string.service_text))
            .setSmallIcon(R.drawable.ic_always_on_black)
            .setShowWhen(false)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun registerSensors() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it, 
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        
        if (hasProximitySensor) {
            proximitySensor?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }

        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }
    }
    
    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        if (screenOffWakeLock?.isHeld == true) {
            screenOffWakeLock?.release()
        }
        
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock?.release()
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                filteredGravity[0] = filteredGravity[0] * 0.8f + event.values[0] * 0.2f
                filteredGravity[1] = filteredGravity[1] * 0.8f + event.values[1] * 0.2f
                filteredGravity[2] = filteredGravity[2] * 0.8f + event.values[2] * 0.2f

                linearAcceleration[0] = event.values[0] - filteredGravity[0]
                linearAcceleration[1] = event.values[1] - filteredGravity[1]
                linearAcceleration[2] = event.values[2] - filteredGravity[2]

                detectPickUp(linearAcceleration[1])
            }
            
            Sensor.TYPE_PROXIMITY -> {
                proximityNear = event.values[0].toInt()
            }
        }
    }
    
    private fun detectPickUp(acceleration: Float) {
        if (!powerManager.isInteractive && proximityNear > 0) {
            if (acceleration >= pickUpThreshold) {
                wakeUpScreen()
            }
        }
    }
    
    private fun wakeUpScreen() {
        if (screenOffWakeLock?.isHeld == false) {
            if (heitezy.peekdisplay.receivers.CombinedServiceReceiver.isAlwaysOnRunning) {
                return
            }

            val timeout = P(getDefaultSharedPreferences(this)).get(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
            val acquireDuration = if (timeout > 0) timeout * 1000L else 5000L

            screenOffWakeLock?.acquire(acquireDuration)
            
            val intent = Intent(this, AlwaysOn::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            heitezy.peekdisplay.receivers.CombinedServiceReceiver.isAlwaysOnRunning = true
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        unregisterSensors()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, PickUpService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PickUpService::class.java)
            context.stopService(intent)
        }
    }
}
