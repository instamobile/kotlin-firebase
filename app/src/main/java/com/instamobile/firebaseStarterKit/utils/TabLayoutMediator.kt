package com.google.android.material.tabs

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.*
import java.lang.ref.WeakReference

/**
 * A mediator to link a TabLayout with a ViewPager2. The mediator will synchronize the ViewPager2's
 * position with the selected tab when a tab is selected, and the TabLayout's scroll position when
 * the user drags the ViewPager2. TabLayoutMediator will listen to ViewPager2's OnPageChangeCallback
 * to adjust tab when ViewPager2 moves. TabLayoutMediator listens to TabLayout's
 * OnTabSelectedListener to adjust VP2 when tab moves. TLM listens to RecyclerView's
 * AdapterDataObserver to recreate tab content when dataset changes.
 *
 *
 * Establish the link by creating an instance of this class, make sure the ViewPager2 has an
 * adapter and then call [.attach] on it. Instantiating a TabLayoutMediator will only create
 * the mediator object, [.attach] will link the TabLayout and the ViewPager2 together. When
 * creating an instance of this class, you must supply an implementation of [ ] in which you set the text of the tab, and/or perform any styling of the
 * tabs that you require. Changing ViewPager2's adapter will require a [.detach] followed by
 * [.attach] call. Changing the ViewPager2 or TabLayout will require a new instantiation of
 * TabLayoutMediator.
 */
class TabLayoutMediator(
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val autoRefresh: Boolean,
    private val tabConfigurationStrategy: TabConfigurationStrategy
) {
    private var adapter: RecyclerView.Adapter<*>? = null
    private var attached: Boolean = false

    private var onPageChangeCallback: TabLayoutOnPageChangeCallback? = null
    private var onTabSelectedListener: TabLayout.OnTabSelectedListener? = null
    private var pagerAdapterObserver: RecyclerView.AdapterDataObserver? = null

    /**
     * A callback interface that must be implemented to set the text and styling of newly created
     * tabs.
     */
    interface TabConfigurationStrategy {
        /**
         * Called to configure the tab for the page at the specified position. Typically calls [ ][TabLayout.Tab.setText], but any form of styling can be applied.
         *
         * @param tab The Tab which should be configured to represent the title of the item at the given
         * position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        fun onConfigureTab(tab: TabLayout.Tab, position: Int)
    }

    constructor(
        tabLayout: TabLayout,
        viewPager: ViewPager2,
        tabConfigurationStrategy: TabConfigurationStrategy
    ) : this(tabLayout, viewPager, true, tabConfigurationStrategy)

    /**
     * Link the TabLayout and the ViewPager2 together. Must be called after ViewPager2 has an adapter
     * set. To be called on a new instance of TabLayoutMediator or if the ViewPager2's adapter
     * changes.
     *
     * @throws IllegalStateException If the mediator is already attached, or the ViewPager2 has no
     * adapter.
     */
    fun attach() {
        check(!attached) { "TabLayoutMediator is already attached" }
        adapter = viewPager.adapter
        checkNotNull(adapter) { "TabLayoutMediator attached before ViewPager2 has an " + "adapter" }
        attached = true

        // Add our custom OnPageChangeCallback to the ViewPager
        onPageChangeCallback = TabLayoutOnPageChangeCallback(tabLayout)
        viewPager.registerOnPageChangeCallback(onPageChangeCallback!!)

        // Now we'll add a tab selected listener to set ViewPager's current item
        onTabSelectedListener = ViewPagerOnTabSelectedListener(viewPager)
        tabLayout.addOnTabSelectedListener(onTabSelectedListener!!)

        // Now we'll populate ourselves from the pager adapter, adding an observer if
        // autoRefresh is enabled
        if (autoRefresh) {
            // Register our observer on the new adapter
            pagerAdapterObserver = PagerAdapterObserver()
            adapter!!.registerAdapterDataObserver(pagerAdapterObserver!!)
        }

        populateTabsFromPagerAdapter()

        // Now update the scroll position to match the ViewPager's current item
        tabLayout.setScrollPosition(viewPager.currentItem, 0f, true)
    }

    /**
     * Unlink the TabLayout and the ViewPager. To be called on a stale TabLayoutMediator if a new one
     * is instantiated, to prevent holding on to a view that should be garbage collected. Also to be
     * called before [.attach] when a ViewPager2's adapter is changed.
     */
    fun detach() {
        adapter!!.unregisterAdapterDataObserver(pagerAdapterObserver!!)
        tabLayout.removeOnTabSelectedListener(onTabSelectedListener!!)
        viewPager.unregisterOnPageChangeCallback(onPageChangeCallback!!)
        pagerAdapterObserver = null
        onTabSelectedListener = null
        onPageChangeCallback = null
        adapter = null
        attached = false
    }

    internal fun populateTabsFromPagerAdapter() {
        tabLayout.removeAllTabs()

        if (adapter != null) {
            val adapterCount = adapter!!.itemCount
            for (i in 0 until adapterCount) {
                val tab = tabLayout.newTab()
                tabConfigurationStrategy.onConfigureTab(tab, i)
                tabLayout.addTab(tab, false)
            }
            // Make sure we reflect the currently set ViewPager item
            if (adapterCount > 0) {
                val lastItem = tabLayout.tabCount - 1
                val currItem = Math.min(viewPager.currentItem, lastItem)
                if (currItem != tabLayout.selectedTabPosition) {
                    tabLayout.selectTab(tabLayout.getTabAt(currItem))
                }
            }
        }
    }

    /**
     * A [ViewPager2.OnPageChangeCallback] class which contains the necessary calls back to the
     * provided [TabLayout] so that the tab position is kept in sync.
     *
     *
     * This class stores the provided TabLayout weakly, meaning that you can use [ ][ViewPager2.registerOnPageChangeCallback] without removing the
     * callback and not cause a leak.
     */
    private class TabLayoutOnPageChangeCallback internal constructor(tabLayout: TabLayout) :
        ViewPager2.OnPageChangeCallback() {
        private val tabLayoutRef: WeakReference<TabLayout> = WeakReference(tabLayout)
        private var previousScrollState: Int = 0
        private var scrollState: Int = 0

        init {
            reset()
        }

        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after
                // being dragged
                val updateText =
                    scrollState != SCROLL_STATE_SETTLING || previousScrollState == SCROLL_STATE_DRAGGING
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                val updateIndicator =
                    !(scrollState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE)
                tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
            }
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null
                && tabLayout.selectedTabPosition != position
                && position < tabLayout.tabCount
            ) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                val updateIndicator =
                    scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
            }
        }

        internal fun reset() {
            scrollState = SCROLL_STATE_IDLE
            previousScrollState = scrollState
        }
    }

    /**
     * A [TabLayout.OnTabSelectedListener] class which contains the necessary calls back to the
     * provided [ViewPager2] so that the tab position is kept in sync.
     */
    private class ViewPagerOnTabSelectedListener internal constructor(private val viewPager: ViewPager2) :
        TabLayout.OnTabSelectedListener {

        override fun onTabSelected(tab: TabLayout.Tab) {
            viewPager.setCurrentItem(tab.position, true)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            // No-op
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            // No-op
        }
    }

    private inner class PagerAdapterObserver internal constructor() :
        RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }
    }
}