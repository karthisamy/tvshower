package com.tvshow

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class WebActivity : AppCompatActivity() {

    var webView: WebView? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val showData = intent.getSerializableExtra("data") as ShowData
        title = showData.name
        webView = findViewById(R.id.webView)
        webView?.webViewClient = WebViewClient()
        webView?.settings?.javaScriptEnabled = true
        webView?.loadUrl(showData.officialSite!!)
    }

}