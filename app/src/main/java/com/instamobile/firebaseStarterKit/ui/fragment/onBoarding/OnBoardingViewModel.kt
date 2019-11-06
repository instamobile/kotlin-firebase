package com.instamobile.firebaseStarterKit.ui.fragment.onBoarding

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.instamobile.firebaseStarterKit.model.SlideContent
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.R

class OnBoardingViewModel(application: Application) : AndroidViewModel(application) {
    private val list = listOf(
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_fast)!!,
            "Move Fast",
            "Use our starter kit in Kotlin to build your apps faster"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_kotlin)!!,
            "Learn Kotlin",
            "Learning Kotlin practically by working on a real project"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_firebase)!!,
            "Learn Firebase",
            "Learn how to use Firebase as a backend for your Kotlin app"
        ),
        SlideContent(
            ContextCompat.getDrawable(application.applicationContext, R.drawable.ic_save_time)!!,
            "Save Time",
            "Save a few days of development by starting with our app template"
        )
    )

    private val _dataSet = MutableLiveData<List<SlideContent>>().apply { value = list }
    val dataSet: LiveData<List<SlideContent>>
        get() = _dataSet

    private val _buttonVisiability = MutableLiveData<Boolean>().apply { value = false }
    val buttonVisiability: LiveData<Boolean>
        get() = _buttonVisiability

    val pagerCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            _buttonVisiability.value = position == list.size - 1
            super.onPageSelected(position)
        }
    }
    private val _startNavigation = MutableLiveData<Boolean>().apply { value = false }
    val startNavigation: LiveData<Boolean>
        get() = _startNavigation

    fun navigateToAuth() {
        _startNavigation.value = true
    }

    fun doneNavigation() {
        _startNavigation.value = false
    }
}