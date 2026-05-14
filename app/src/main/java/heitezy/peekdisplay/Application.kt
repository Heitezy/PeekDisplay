package heitezy.peekdisplay

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.color.DynamicColors

class Application : android.app.Application() {
    companion object {
        lateinit var requestQueue: RequestQueue
            private set
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        requestQueue = Volley.newRequestQueue(this)
    }
}
