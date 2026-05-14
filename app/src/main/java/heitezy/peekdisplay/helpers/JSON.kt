package heitezy.peekdisplay.helpers

import org.json.JSONArray

internal object JSON {
    fun contains(
        jsonArray: JSONArray,
        key: String,
    ): Boolean {
        for (i in 0 until jsonArray.length()) {
            if (jsonArray.get(i) == key) return true
        }
        return false
    }

}
