package com.combinedpublic.mobileclient

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.combinedpublic.mobileclient.Classes.CallManager
import com.combinedpublic.mobileclient.Classes.Configuration
import com.combinedpublic.mobileclient.Classes.User
import com.combinedpublic.mobileclient.services.Contact
import com.combinedpublic.mobileclient.ui.ContactsAdapter
import com.eggheadgames.siren.ISirenListener
import com.eggheadgames.siren.Siren
import com.eggheadgames.siren.SirenAlertType
import com.eggheadgames.siren.SirenVersionCheckType


class Main : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    internal val LOG_TAG = "MainActivityLogs"

    private val SIREN_JSON_DOCUMENT_URL = "https://api.myjson.com/bins/sfrck"

    lateinit var btn: Button
    lateinit var btnMenu: Button
    lateinit var mainConstraintLayout: ConstraintLayout
    lateinit var nameLbl:TextView
    lateinit var balanceLbl:TextView
    lateinit var depositLbl:TextView

    lateinit var loadingPanelMain:RelativeLayout

    var anim: Animation? = null
    var menuIsOpened: Boolean = false

    var user = User.getInstance()
    lateinit var contactsArr:ArrayList<Contact>

    lateinit var toolbarMain: Toolbar
    lateinit var contactListView: ListView

    lateinit var adapter:ContactsAdapter


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "suspendApp") {
                suspendApp()
            }
            else if (action == "unSuspendApp") {
                Log.d(LOG_TAG,"Receive broadcast: unSuspendApp")
                var toastMsg = intent.getStringExtra("toast")
                unSuspendApp(toastMsg)

            }
            else if (action == "refreshContacts") {
                contactsArr = intent.getParcelableArrayListExtra<Contact>("Contacts")
                if (!CallManager.getInstance().isMinimized) {
                    updateContacts(contactsArr)
                }
            }
            else if (action == "incomingCall") {
                if (!user._isCallingShowed){
                    handleShowCallingView()
                }
            }
            else if (action == "startCall") {
                handleShowVideoCallView()
            }
            else if (action == "Unauthorized") {
                Toast.makeText(applicationContext,"Already Authorized",Toast.LENGTH_SHORT).show()
                logOff()
            } else if (action == "UnauthorizedDevice") {
                Toast.makeText(applicationContext,"Logged in from another device.",Toast.LENGTH_SHORT).show()
                logOff()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        user._isMainShowed = true

        val filter = IntentFilter()
        filter.addAction("suspendApp")
        filter.addAction("unSuspendApp")
        filter.addAction("refreshContacts")
        filter.addAction("incomingCall")
        filter.addAction("startCall")
        filter.addAction("Unauthorized")
        filter.addAction("UnauthorizedDevice")
        registerReceiver(receiver, filter)

        val settings = getSharedPreferences("Settings", 0)
        user.name = settings.getString("name","Kashap")
        user.balance = settings.getString("balance","00.00")
        user.id = settings.getString("id","14")



        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn = findViewById(R.id.logOffBtn)
        btnMenu = findViewById(R.id.btnMenu)
        nameLbl = findViewById(R.id.nameLbl)
        balanceLbl = findViewById(R.id.balanceLbl)
        depositLbl = findViewById(R.id.depositLbl)

        btnMenu.setOnClickListener(this)
        btn.setOnClickListener(this)

        toolbarMain = findViewById(R.id.toolbarMain)

        loadingPanelMain = findViewById(R.id.loadingPanelMain)

        nameLbl.text = user.name
        balanceLbl.text = user.balance

        Log.d(LOG_TAG,"UserName is - "+user.name+", password is - "+user.password)

        contactsArr = ArrayList()

        this.contactListView = findViewById(R.id.contactListView)
        this.mainConstraintLayout = findViewById(R.id.mainConstraintLayout)


        var emptyList = java.util.ArrayList<Contact>()

        if (user.contacts != null && user.contacts!!.count() > 0) {
            if (user.contacts!!.count() < 10) {
                val contactsValue = 15 - user.contacts!!.count()
                    for (i in 0..contactsValue) {
                        val emptyContact = Contact("",0)
                        emptyList = user.contacts!!
                        emptyList.add(emptyContact)
                    }
            } else {
                emptyList = user.contacts!!
            }
        } else {
            for (i in 0..14) {
                val emptyContact = Contact("",0)
                emptyList.add(emptyContact)
            }
        }


        user.contacts = emptyList

        adapter = ContactsAdapter(this,user.contacts!!)
        if (!adapter.isEmpty) {
            Log.d("TAG", adapter.count.toString())
        }

        contactListView.adapter = adapter

        anim = AnimationUtils.loadAnimation(this, R.anim.open_menu)

        contactListView.onItemClickListener = this

        menuIsOpened = false

        //checkCurrentAppVersion()
        val siren = Siren.getInstance(applicationContext)
        //siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, "https://api.myjson.com/bins/13eclg")
    }

    override fun onDestroy() {
        try{
            unregisterReceiver(receiver)
        } catch (e:IllegalArgumentException ){
            Log.d(LOG_TAG,"Error: reciever alredy is unregistered : "+e.localizedMessage)
        }

        user._isMainShowed = false

        super.onDestroy()
    }

    private fun checkCurrentAppVersion() {
        Log.d(LOG_TAG,"checkCurrentAppVersion called")
        var siren = Siren.getInstance(applicationContext)
        siren.setSirenListener(sirenListener)
        //siren.setMajorUpdateAlertType(SirenAlertType.FORCE)
        //siren.setMinorUpdateAlertType(SirenAlertType.OPTION)
        //siren.setPatchUpdateAlertType(SirenAlertType.SKIP)
        //siren.setRevisionUpdateAlertType(SirenAlertType.NONE)
        siren.setVersionCodeUpdateAlertType(SirenAlertType.FORCE)
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL)
    }

    private val sirenListener = object : ISirenListener {
        val TAG = "sirenListener"
        override fun onDetectNewVersionWithoutAlert(message: String?) {
            Log.d(TAG, "onDetectNewVersionWithoutAlert: $message")
        }

        override fun onCancel() {
            Log.d(TAG, "onCancel")
        }

        override fun onShowUpdateDialog() {
            Log.d(TAG, "onShowUpdateDialog")
        }

        override fun onSkipVersion() {
            Log.d(TAG, "onSkipVersion")
        }

        override fun onLaunchGooglePlay() {
            Log.d(TAG, "onLaunchGooglePlay")
        }

        override fun onError(e: Exception?) {
            Log.d(TAG, "onError")
            if (e != null) {
                e?.printStackTrace()
            }
          }
        }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.logOffBtn -> logOff()
            R.id.btnMenu -> openMenu()
            R.id.depositLbl -> openUrl(Configuration.urlAddFunds())
            else -> {
            }
        }
    }

    fun openUrl(url: String) {
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        if (menuIsOpened) {
            openMenu()
        } else {
            Log.d("TAG_ContactList", "itemClick: position = $i, id = $l")
            Log.d("TAG_ContactList", user.contacts!![i].displayname)

            if (user.contacts!![i].id!! > 0) {
                val intent = Intent()
                intent.action = "call"
                intent.putExtra("contactId",user.contacts!![i].id!!.toString())
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                sendBroadcast(intent)

                CallManager.getInstance()._isInitiator = true
                CallManager.getInstance()._contactName = user.contacts!![i].displayname!!
                handleShowCallingView()
                sendMsg("suspendApp")
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        Log.d("TAG_ContactList", "itemSelect: position = $i, id = $l")
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {
        Log.d("TAG_ContactList", "itemSelect: nothing")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Check if no view has focus:
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return super.onTouchEvent(event)
    }

    override fun onBackPressed() {
        //super.onBackPressed();
    }

    fun showCallingViewAsInitiator() {
        val intentMain = Intent(this@Main, Calling::class.java)
        intentMain.putExtra("isInitiator",CallManager.getInstance()._isInitiator)
        intentMain.putExtra("contactName",CallManager.getInstance()._contactName)
        Log.d(LOG_TAG,"Configuration.VisiTel.isActivityCallingVisible "+Configuration.CombinedPublic.isActivityCallingVisible()+" , Configuration.VisiTel.isActivityVideoCallVisible "+
                Configuration.CombinedPublic.isActivityVideoCallVisible())
        if (!Configuration.CombinedPublic.isActivityCallingVisible() && !Configuration.CombinedPublic.isActivityVideoCallVisible()) {
            intentMain.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            overridePendingTransition (0, 0)
            startActivityForResult(intentMain, 1)
        }
    }

    fun showCallingViewAsIncomming() {
        val intentMain = Intent(this@Main, Calling::class.java)
        intentMain.putExtra("isInitiator",CallManager.getInstance()._isInitiator)
        intentMain.putExtra("contactName",CallManager.getInstance()._contactName)
        Log.d(LOG_TAG,"Configuration.VisiTel.isActivityCallingVisible "+Configuration.CombinedPublic.isActivityCallingVisible()+" , Configuration.VisiTel.isActivityVideoCallVisible "+
                Configuration.CombinedPublic.isActivityVideoCallVisible())
        if (!Configuration.CombinedPublic.isActivityCallingVisible() && !Configuration.CombinedPublic.isActivityVideoCallVisible()) {
            intentMain.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            overridePendingTransition (0, 0)
            startActivityForResult(intentMain, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            unSuspendApp("")
        }
    }


    override fun onPause() {
        sendMsg("appPause")
        CallManager.getInstance().isMinimized = true
        super.onPause()
    }

    override fun onResume() {
        sendMsg("appResume")
        CallManager.getInstance().isMinimized = false
        Log.d(LOG_TAG,"onResume")
        User.getInstance()._isUrlOpen = false
        //"appPause"
        if (CallManager.getInstance()._isIncommingStarted) {
            sendMsg("repeatRaCall")
            CallManager.getInstance()._isIncommingStarted = false
        }
        super.onResume()
        unSuspendApp("")
        if (!CallManager.getInstance().isMinimized && !contactsArr.isEmpty()) {
            updateContacts(contactsArr)
        }
    }

    fun logOff() {
        sendMsg("logOff")
        val settings = getSharedPreferences("Settings", 0)
        val editor = settings.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.apply()
        user.isLoggedIn = false
        finish()
        user._isMainShowed = false
    }

    fun openMenu() {

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        if (!menuIsOpened) {
            mainConstraintLayout.animate()
                    .setDuration(300)
                    .translationX(width / 1.5f)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            mainConstraintLayout.translationX = width / 1.5f
                            mainConstraintLayout.scaleX = 0.9f
                            mainConstraintLayout.scaleY = 0.9f
                        }
                    })
            toolbarMain.background = ContextCompat.getDrawable(this, R.drawable.bar_rounded_corners)
            contactListView.background = ContextCompat.getDrawable(this, R.drawable.list_rounded_corners)
            contactListView.setPadding(20, 0, 0, 0)
            menuIsOpened = true

        } else {
            mainConstraintLayout.animate()
                    .setDuration(300)
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            mainConstraintLayout.translationX = 0f
                            mainConstraintLayout.scaleX = 1f
                            mainConstraintLayout.scaleY = 1f
                        }
                    })
            toolbarMain.background = ContextCompat.getDrawable(this, R.color.main_toolbar_color)
            contactListView.background = ContextCompat.getDrawable(this, R.color.main_list_color)
            menuIsOpened = false
        }
    }

    fun suspendApp() {
        this.loadingPanelMain.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun unSuspendApp(str: String?) {
        this.loadingPanelMain.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        if (str != null && str != "") {
                Toast.makeText(applicationContext,"Error: "+ str,Toast.LENGTH_SHORT).show()
        }
    }

    fun handleShowCallingView() {

        User.getInstance()._isCallingShowed = true
        User.getInstance()._isUrlOpen = true
        if (CallManager.getInstance()._isInitiator) {
            showCallingViewAsInitiator()
        } else {
            showCallingViewAsIncomming()
        }

    }

    fun handleShowVideoCallView() {
        val intentMain = Intent(this@Main, VideoCall::class.java)
        startActivity(intentMain)
    }

    fun updateContacts(contactsArr: ArrayList<Contact>) {
        Log.d(LOG_TAG,"Update Contacts is Execute")

        var emptyList = ArrayList<Contact>()

        /*contactsArr[0].displayname = "Manit Chanthavong"
        contactsArr[1].displayname = "Guanwei Liu"
        contactsArr[2].displayname = "Artur Kashapov"*/

        if (contactsArr != null && contactsArr!!.count() > 0) {
            if (contactsArr!!.count() < 10) {
                val contactsValue = 15 - contactsArr!!.count()
                emptyList = contactsArr
                for (i in 0..contactsValue) {
                    val emptyContact = Contact("",0)
                    emptyList.add(emptyContact)
                }
            } else {
                emptyList = contactsArr
            }
        } else {
            for (i in 0..14) {
                val emptyContact = Contact("",0)
                emptyList.add(emptyContact)
            }
        }


        this@Main.runOnUiThread {
            user.contacts!!.clear()
            user.contacts!!.addAll(emptyList)
            adapter.notifyDataSetChanged()
            contactListView.invalidateViews()
            contactListView.refreshDrawableState()
        }
    }

    private fun sendMsg(str: String){
        val intent = Intent()
        intent.action = str
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }
}



