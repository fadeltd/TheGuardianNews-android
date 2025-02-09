package com.antarikshc.theguardiannews.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import com.antarikshc.theguardiannews.R
import com.antarikshc.theguardiannews.datasource.NewsLoader
import com.antarikshc.theguardiannews.model.NewsData
import com.antarikshc.theguardiannews.util.Master
import com.antarikshc.theguardiannews.util.openBrowser
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<ArrayList<NewsData>> {

    // Global params
    private var searchNewsUrl: String? = null

    private var scrollState = 0
    private var loaderManager: LoaderManager? = null

    private lateinit var searchNewsAdapter: CustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val searchIntent = intent

        //set it as action bar to retrieve it back
        setSupportActionBar(search_activity_toolbar)

        //Add back button in Toolbar
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            // set the title as the search query on toolbar
            title = searchIntent.getStringExtra("title").capitalize()
        }

        searchNewsUrl = searchIntent.getStringExtra("url")

        load_spin.visibility = View.VISIBLE
        search_news_list.animate().alpha(0.1f).duration = 400

        //initiate CustomAdapter and set it for worldNewsList
        searchNewsAdapter = CustomAdapter(applicationContext, ArrayList())
        search_news_list.adapter = searchNewsAdapter

        //set empty text view for a proper msg to user
        search_news_list.emptyView = empty_view

        loaderManager = supportLoaderManager

        if (Master.checkNet(this@SearchActivity) && searchNewsAdapter.count == 0) {
            executeLoader()
        } else {
            load_spin.visibility = View.GONE
            empty_view.setText(R.string.no_network)
        }

        search_news_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            openBrowser(searchNewsAdapter.getItem(position))
        }

        search_refresh.setOnRefreshListener {
            empty_view.text = ""

            SEARCH_NEWS_LOADER += 1
            search_refresh.isRefreshing = true

            if (Master.checkNet(this@SearchActivity)) {
                executeLoader()
            } else {
                load_spin.visibility = View.GONE
                empty_view.setText(R.string.no_network)
            }

            destroyLoader(SEARCH_NEWS_LOADER - 1)

            search_refresh.isRefreshing = false
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    //This is to prevent listview losing the scroll position
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            outState.putInt("CURRENT_SCROLL", search_news_list.firstVisiblePosition)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            scrollState = savedInstanceState.getInt("CURRENT_SCROLL")
            search_news_list.setSelection(scrollState)
        }
    }

    //Initiate and destroy loader methods to be called after search is submitted
    private fun executeLoader() {
        loaderManager?.initLoader(SEARCH_NEWS_LOADER, null, this)
    }

    private fun destroyLoader(id: Int) {
        loaderManager?.destroyLoader(id)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): androidx.loader.content.Loader<ArrayList<NewsData>> {
        return NewsLoader(this, searchNewsUrl)
    }

    override fun onLoadFinished(loader: androidx.loader.content.Loader<ArrayList<NewsData>>, news: ArrayList<NewsData>?) {
        empty_view.setText(R.string.no_news)

        // Clear the adapter of previous books data
        searchNewsAdapter.clear()

        load_spin.visibility = View.GONE

        if (news != null && news.isNotEmpty()) {
            searchNewsAdapter.addAll(news)
            search_news_list.animate().alpha(1.0f).duration = 400
            search_news_list.setSelection(scrollState)
        }
    }

    override fun onLoaderReset(loader: androidx.loader.content.Loader<ArrayList<NewsData>>) {
        searchNewsAdapter.clear()
    }

    companion object {
        //we are using different loaders for each tab
        private var SEARCH_NEWS_LOADER = 30
    }
}
