/*
 * SettingsCommonRecyclerViewAdapter.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import space.celestia.mobilecelestia.R
import space.celestia.mobilecelestia.common.*
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class SettingsCommonRecyclerViewAdapter(
    private val item: SettingsCommonItem,
    private val listener: SettingsCommonFragment.Listener?,
    private val dataSource: SettingsCommonFragment.DataSource?
) : SeparatorHeaderRecyclerViewAdapter(listOf()) {

    init {
        reload()
    }

    override fun itemViewType(item: RecyclerViewItem): Int {
        if (item is SettingsSliderItem)
            return ITEM_SLIDER

        if (item is SettingsActionItem)
            return ITEM_ACTION

        if (item is SettingsPreferenceSwitchItem)
            return ITEM_PREF_SWITCH

        if (item is SettingsUnknownTextItem)
            return ITEM_UNKNOWN_TEXT

        if (item is SettingsSwitchItem)
            return if (item.representation == SettingsSwitchItem.Representation.Switch) ITEM_SWITCH else ITEM_CHECKMARK

        if (item is SettingsPreferenceSelectionItem)
            return ITEM_PREF_SELECTION

        if (item is SettingsSelectionSingleItem)
            return ITEM_SINGLE_SELECTION

        return super.itemViewType(item)
    }

    override fun onItemSelected(item: RecyclerViewItem) {
        if (item is SettingsActionItem)
            listener?.onCommonSettingActionItemSelected(item.action)
        else if (item is SettingsUnknownTextItem)
            listener?.onCommonSettingUnknownAction(item.id)
        else if (item is SettingsSwitchItem && item.representation == SettingsSwitchItem.Representation.Checkmark) {
            val on = dataSource?.commonSettingSwitchState(item.key) ?: false
            listener?.onCommonSettingSwitchStateChanged(item.key, !on, item.volatile)
        } else if (item is SettingsPreferenceSelectionItem) {
            listener?.onCommonSettingSelectionRequested(item.key, item.options)
        }
    }

    override fun bindVH(holder: RecyclerView.ViewHolder, item: RecyclerViewItem) {
        val weakSelf = WeakReference(this)
        if (holder is SliderViewHolder && item is SettingsSliderItem) {
            val num = dataSource?.commonSettingSliderValue(item.key) ?: 0.0
            holder.configure(item.name, item.minValue, item.maxValue, num) { newValue ->
                val self = weakSelf.get() ?: return@configure
                self.listener?.onCommonSettingSliderItemChange(item.key, newValue)
            }
            return
        }
        if (holder is RadioButtonViewHolder && item is SettingsSelectionSingleItem) {
            val selected = dataSource?.commonSettingSelectionValue(item.key) ?: item.defaultSelection
            holder.configure(text = item.name, showTitle = item.showTitle, options = item.options.map { it.second }, checkedIndex = item.options.indexOfFirst { it.first == selected }) { newIndex ->
                val self = weakSelf.get() ?: return@configure
                self.listener?.onCommonSettingSelectionChanged(item.key, item.options[newIndex].first)
            }
            return
        }
        if (holder is CommonTextViewHolder) {
            when (item) {
                is SettingsActionItem -> {
                    holder.title.text = item.name
                }
                is SettingsUnknownTextItem -> {
                    holder.title.text = item.name
                }
                is SettingsPreferenceSelectionItem -> {
                    val selected = dataSource?.commonSettingPreferenceSelectionState(item.key) ?: item.defaultSelection
                    holder.title.text = item.name
                    holder.detail.text = item.options.firstOrNull { it.first == selected }?.second ?: ""
                    holder.detail.visibility = View.VISIBLE
                }
            }
            return
        }
        if (holder is SwitchViewHolder) {
            if (item is SettingsPreferenceSwitchItem) {
                holder.configure(item.name, item.subtitle, dataSource?.commonSettingPreferenceSwitchState(item.key) ?: item.defaultOn) { checked ->
                    val self = weakSelf.get() ?: return@configure
                    self.listener?.onCommonSettingPreferenceSwitchStateChanged(item.key, checked)
                }
            } else if (item is SettingsSwitchItem) {
                val on = dataSource?.commonSettingSwitchState(item.key) ?: false
                holder.configure(item.name, null, on) { newValue ->
                    val self = weakSelf.get() ?: return@configure
                    self.listener?.onCommonSettingSwitchStateChanged(item.key, newValue, item.volatile)
                }
            }
            return
        }
        if (holder is CheckboxViewHolder && item is SettingsSwitchItem) {
            val on = dataSource?.commonSettingSwitchState(item.key) ?: false
            holder.configure(item.name, on) { newValue ->
                val self = weakSelf.get() ?: return@configure
                self.listener?.onCommonSettingSwitchStateChanged(item.key, newValue, item.volatile)
            }
            return
        }
        super.bindVH(holder, item)
    }

    override fun createVH(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ITEM_SLIDER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.common_text_list_with_slider_item, parent,false)
            return SliderViewHolder(view)
        }
        if (viewType == ITEM_SINGLE_SELECTION) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.common_text_list_with_radio_button_item, parent,false)
            return RadioButtonViewHolder(view)
        }
        if (viewType == ITEM_ACTION || viewType == ITEM_UNKNOWN_TEXT || viewType == ITEM_PREF_SELECTION) {
            return CommonTextViewHolder(parent)
        }
        if (viewType == ITEM_PREF_SWITCH || viewType == ITEM_SWITCH) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.common_text_list_with_switch_item, parent,false)
            return SwitchViewHolder(view)
        }
        if (viewType == ITEM_CHECKMARK) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.common_text_list_with_checkbox_item, parent,false)
            return CheckboxViewHolder(view)
        }
        return super.createVH(parent, viewType)
    }

    inner class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView
            get() = itemView.findViewById(R.id.title)
        private val seekBar: Slider
            get() = itemView.findViewById(R.id.slider)

        private var progressCallback: ((Double) -> Unit)? = null

        init {
            val weakSelf = WeakReference(this)
            seekBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    val callback = weakSelf.get()?.progressCallback ?: return
                    callback(slider.value.toDouble())
                }
            })
        }

        fun configure(text: String, minValue: Double, maxValue: Double, value: Double, callback: (Double) -> Unit) {
            title.text = text
            seekBar.valueFrom = minValue.toFloat()
            seekBar.valueTo = maxValue.toFloat()
            // Avoid exceeding the range
            seekBar.value = max(minValue, min(maxValue, value)).toFloat()
            progressCallback = callback
        }
    }

    inner class SwitchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView
            get() = itemView.findViewById(R.id.title)
        val subtitle: TextView
            get() = itemView.findViewById(R.id.subtitle)
        val switch: MaterialSwitch
            get() = itemView.findViewById(R.id.accessory)

        fun configure(text:String, description: String?, isChecked: Boolean, stateChangeCallback: (Boolean) -> Unit) {
            switch.setOnCheckedChangeListener(null)
            title.text = text
            switch.isChecked = isChecked
            if (description != null) {
                subtitle.text = description
                subtitle.visibility = View.VISIBLE
            } else {
                subtitle.text = null
                subtitle.visibility = View.GONE
            }
            switch.setOnCheckedChangeListener { _, checked ->
                stateChangeCallback(checked)
            }
        }
    }

    inner class CheckboxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: MaterialCheckBox
            get() = itemView.findViewById(R.id.checkbox)

        fun configure(text:String, isChecked: Boolean, stateChangeCallback: (Boolean) -> Unit) {
            checkbox.setOnCheckedChangeListener(null)
            checkbox.text = text
            checkbox.isChecked = isChecked
            checkbox.setOnCheckedChangeListener { _, checked ->
                stateChangeCallback(checked)
            }
        }
    }

    fun reload() {
        val results = ArrayList<CommonSectionV2>()
        for (section in item.sections) {
            val sectionResults = ArrayList<RecyclerViewItem>()
            if (section.rows.size == 1 && section.rows[0] is SettingsDynamicListItem) {
                val item = section.rows[0] as SettingsDynamicListItem
                results.add(CommonSectionV2(item.createItems(), section.header, section.footer))
            } else {
                for (row in section.rows) {
                    if (row is SettingsDynamicListItem) {
                        throw RuntimeException("SettingsDynamicListItem should not be embedded in a multi-row section")
                    }
                    sectionResults.add(row)
                }
                results.add(CommonSectionV2(sectionResults, section.header, section.footer))
            }
        }
        updateSectionsWithHeader(results)
    }

    private companion object {
        const val ITEM_SLIDER           = 0
        const val ITEM_ACTION           = 1
        const val ITEM_PREF_SWITCH      = 2
        const val ITEM_SWITCH           = 3
        const val ITEM_CHECKMARK        = 4
        const val ITEM_UNKNOWN_TEXT     = 5
        const val ITEM_PREF_SELECTION   = 6
        const val ITEM_SINGLE_SELECTION = 7
    }
}
