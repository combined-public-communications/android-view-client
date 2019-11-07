package com.combinedpublic.mobileclient.ui

import android.content.Context
import androidx.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.combinedpublic.mobileclient.R
import com.combinedpublic.mobileclient.services.Contact
import java.util.*


class ContactsAdapter(private val mContext: Context, @LayoutRes list: ArrayList<Contact>) : ArrayAdapter<Contact>(mContext, 0, list) {
    private var contactList = ArrayList<Contact>()

    init {
        contactList = list
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItem = convertView
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.contact_list_item, parent, false)

        val currentContact = contactList[position]

        val name = listItem!!.findViewById(R.id.textView_name) as TextView
        name.setText(currentContact.displayname)

        val isOnline = listItem!!.findViewById(R.id.indicatorImageView) as ImageView
        if (currentContact.id != null && currentContact.id > 0) {
            isOnline.visibility = View.VISIBLE
        } else {
            isOnline.visibility = View.GONE
        }

        return listItem
    }
}