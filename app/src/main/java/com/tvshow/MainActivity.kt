package com.tvshow

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TextView.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    var searchView: TextView? = null
    var imageView: ImageView? = null
    var infoLayout: LinearLayout? = null
    var searchCancelView: ImageView? = null
    var showData: ShowData? = null
    var databaseHelper: DatabaseHelper? = null
    var progressBar: ProgressBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchView = findViewById(R.id.search_bar)
        imageView = findViewById(R.id.image)
        infoLayout = findViewById(R.id.infoLayout)
        searchCancelView = findViewById(R.id.searchCancelView)
        progressBar = findViewById(R.id.progressBar)
        searchCancelView?.setOnClickListener {
            searchView?.text = ""
        }

        infoLayout?.visibility = GONE

        databaseHelper = DatabaseHelper(this)

/*
        imageView?.setOnClickListener {
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra("data", showData)
            startActivity(intent)
        }
*/
        searchView?.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                closeKeyboard()
                getData(searchView?.text.toString())
                return@OnEditorActionListener true
            }
            false
        })


        searchView?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (searchView?.text.toString().isNotEmpty()) {
                    searchCancelView?.visibility = View.VISIBLE
                } else {
                    searchCancelView?.visibility = View.GONE
                }
            }
        })
    }

    private fun getData(qwerty: String) {
        val data = databaseHelper?.getValueFromKeyword(qwerty)
        if (data != null) {
            showData = data
            setValueToUi(showData!!)

            imageView?.visibility = View.VISIBLE
            if (showData?.imageUrl != null) {
                DownloadImageTask(showData?.imageUrl, showData?.id).execute()
            } else {
                imageView?.setImageBitmap(null)
            }
        } else {
            FetchDataFromApiLocalTask(qwerty).execute()
        }
    }


    @SuppressLint("StaticFieldLeak")
    inner class FetchDataFromApiLocalTask(private val qString: String?) :
            AsyncTask<String, String, String>() {
        override fun doInBackground(vararg p0: String?): String? {
            val url = URL("http://api.tvmaze.com/singlesearch/shows?q=$qString")
            val httpURLConnection = url.openConnection() as HttpURLConnection
            var response: String? = null
            try {
                httpURLConnection.connect()
                val responseCode: Int = httpURLConnection.responseCode
                if (responseCode == 200) {
                    response = httpURLConnection.inputStream.bufferedReader().readText()
                }
            } catch (ex: Exception) {
                ex.stackTrace
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            progressBar?.visibility = View.GONE
            println(result)
            if (result != null) {
                showData = ShowData()
                showData?.parseJson(result)
                showData?.keyWord = qString
                databaseHelper?.addToDB(showData!!, result)
                setValueToUi(showData!!)
                DownloadImageTask(showData?.imageUrl, showData?.id).execute()
                infoLayout?.visibility = View.VISIBLE
            } else {
                imageView?.setImageBitmap(null)
                val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok") { dialog: DialogInterface, _: Int -> dialog.cancel() }
                alertDialog.setMessage("Unable To Find TV Show")
                alertDialog.show()
            }
        }

        override fun onPreExecute() {
            progressBar?.visibility = View.VISIBLE
            infoLayout?.visibility = View.GONE
            //imageView?.visibility = View.GONE
        }

    }


    @SuppressLint("StaticFieldLeak")
    inner class DownloadImageTask(private val imageUrl: String?, private val id: String?) :
            AsyncTask<String, String, Bitmap>() {
        override fun doInBackground(vararg p0: String?): Bitmap? {
            val bitmapFromStorage = getImageFromStorage("$id.PNG")
            if (bitmapFromStorage != null) {
                return bitmapFromStorage
            }
            try {
                val url = URL(imageUrl)
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connect()
                val responseCode: Int = httpURLConnection.responseCode
                if (responseCode == 200) {
                    val bitmap = BitmapFactory.decodeStream(httpURLConnection.inputStream)
                    saveToInternalStorage(bitmap, id)
                    return bitmap
                }
            } catch (ex: Exception) {
                println("Image download Exception $ex")
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            imageView?.visibility = View.VISIBLE
            imageView?.setImageBitmap(result)
        }

        override fun onPreExecute() {
            imageView?.setImageBitmap(null)
        }

    }

    fun saveToInternalStorage(bitmapImage: Bitmap, id: String?) {
        val cw = ContextWrapper(this)
        val directory = cw.getDir("image", Context.MODE_PRIVATE)
        val path = "$id.PNG"
        val mypath = File(directory, path)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(mypath)
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun getImageFromStorage(path: String): Bitmap? {
        val cw = ContextWrapper(this)
        val directory = cw.getDir("image", Context.MODE_PRIVATE)
        try {
            val f = File(directory, path)
            return BitmapFactory.decodeStream(FileInputStream(f))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun setValueToUi(showData: ShowData) {
        infoLayout?.visibility = VISIBLE
        findViewById<TextView>(R.id.name).text = showData.name
        findViewById<TextView>(R.id.duration).text = showData.runtime?.toString() + " min"

        val ratingView = findViewById<TextView>(R.id.rating)
        if (showData.rating != null
                && !showData.rating.isNullOrEmpty()
                && showData.rating != "null") {
            ratingView.text = showData.rating
            ratingView.visibility = View.VISIBLE
        } else {
            ratingView.visibility = View.GONE
        }

        val gendreView = findViewById<TextView>(R.id.gendreView)
        if (showData.gendres != null && !showData.gendres.isNullOrEmpty()) {
            gendreView.visibility = View.VISIBLE
            gendreView.text = showData.gendres
        } else {
            gendreView.visibility = View.GONE
        }

        findViewById<TextView>(R.id.language).text = showData.language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findViewById<TextView>(R.id.summary).text = Html.fromHtml(showData.summary, Html.FROM_HTML_MODE_COMPACT)
        } else {
            findViewById<TextView>(R.id.summary).text = Html.fromHtml(showData.summary)
        }
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = simpleDateFormat.parse(showData.premiered!!)
        if (date!!.after(Date(System.currentTimeMillis()))) {
            findViewById<TextView>(R.id.daysWhen).text = (-getDifferenceDays(date, Date(System.currentTimeMillis()))).toString() + " Days" + " Yet To Premier"
        } else {
            findViewById<TextView>(R.id.daysWhen).text = getDifferenceDays(date, Date(System.currentTimeMillis())).toString() + " Days" + " Since Premier"
        }
    }


    private fun closeKeyboard() {
        searchView?.clearFocus()
        val inputMethodManager: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(searchView?.getWindowToken(), 0)
    }


    fun getDifferenceDays(d1: Date, d2: Date): Long {
        val diff = d2.time - d1.time
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    }

}
