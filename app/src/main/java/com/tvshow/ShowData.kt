package com.tvshow

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

class ShowData : Serializable {

    var id: String? = null
    var day: String? = null
    var officialSite: String? = null
    var name: String? = null
    var gendres: String? = null
    var summary: String? = null
    var rating: String? = null
    var runtime: Int? = null
    var language: String? = null
    var premiered: String? = null
    var imageUrl: String? = null
    var keyWord: String? = null


    fun parseJson(jsonString: String?) {
        val jObject = JSONObject(jsonString!!)
        id = jObject.getString("id")
        name = jObject.getString("name")
        runtime = jObject.optInt("runtime")
        premiered = jObject.getString("premiered");
        summary = jObject.getString("summary");
        language = jObject.getString("language");
        officialSite = jObject.getString("officialSite");
        imageUrl = jObject.optJSONObject("image")?.getString("original")
        gendres = parseGendre(jObject.getJSONArray("genres"))
        rating = jObject.optJSONObject("rating")?.getString("average")
    }


    fun parseGendre(jsonArray: JSONArray?): String? {
        var gendres = ""
        var commas = ""
        if (jsonArray != null) {
            val length = jsonArray.length()
            for (i in 0 until length) {
                gendres = gendres + commas + jsonArray.optString(i)
                commas = "     "
            }
        }
        return gendres
    }

}
