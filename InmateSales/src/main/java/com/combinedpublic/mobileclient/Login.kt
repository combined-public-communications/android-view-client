package com.combinedpublic.mobileclient

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.combinedpublic.mobileclient.Classes.CallManager
import com.combinedpublic.mobileclient.Classes.Configuration
import com.combinedpublic.mobileclient.Classes.CpcApplication
import com.combinedpublic.mobileclient.Classes.User
import com.combinedpublic.mobileclient.services.ConnectionManager


class Login : AppCompatActivity(), View.OnClickListener {

    internal val LOG_TAG = "LoginActivityLogs"


    lateinit var btnLogin: Button
    lateinit var btnRegister: Button
    lateinit var editTextLogin: EditText
    lateinit var editTextPassword: EditText
    lateinit var loadingPanelLogin: RelativeLayout
    lateinit var tvForget: TextView

    private val PERMISSION_REQUEST_CODE = 200

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "successfulLogin") {
                Log.d(LOG_TAG,"We Get successfulLogin !!")

                successfulLogin()

            } else if (action == "suspendApp") {
                suspendApp()
            } else if (action == "unSuspendApp") {
                var toastMsg = intent.getStringExtra("toast")
                unSuspendApp(toastMsg)
            } else if (action == Intent.ACTION_SCREEN_ON) {
                startService()
            } else if (action == Intent.ACTION_SCREEN_OFF) {
                User.getInstance()._isRestarted = true
                stopService()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Log.d(LOG_TAG,"ApplicationId :"+BuildConfig.APPLICATION_ID
                +"\nPackageName :"+ applicationContext.packageName
                +"\nJava package :"+ BuildConfig::class.java.getPackage().toString())

        val filter = IntentFilter()
        filter.addAction("successfulLogin")
        filter.addAction("suspendApp")
        filter.addAction("unSuspendApp")
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, filter)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        editTextLogin = findViewById(R.id.editTextLogin)
        editTextPassword = findViewById(R.id.editTextPassword)
        loadingPanelLogin = findViewById(R.id.loadingPanelLogin)
        tvForget = findViewById(R.id.textViewForget)

        if (User.getInstance().userName != null && User.getInstance().password != null){
            editTextLogin.setText(User.getInstance().userName, TextView.BufferType.EDITABLE)
            editTextPassword.setText(User.getInstance().password, TextView.BufferType.EDITABLE)
        }

        User.getInstance()._isAllPermsGranted = false

        if (!checkPermission(Manifest.permission.CAMERA) ||
                !checkPermission(Manifest.permission.RECORD_AUDIO) ||
                !checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                !checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val perms = arrayOf(Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermission(perms)
        }

        //val isDebuggable = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnLogin -> login(editTextLogin.text.toString(),editTextPassword.text.toString())
            R.id.btnRegister -> openUrl(Configuration.urlWebSite())
            R.id.textViewForget -> openUrl(Configuration.urlForgotPassword())
            else -> {
            }
        }
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


    fun login(login: String, password: String) {
        Log.d(LOG_TAG,"Login - "+login+" Password - "+password)

        if (login.isNotEmpty() && password.isNotEmpty()) {
            val intent = Intent()
            intent.action = "login"
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            intent.putExtra("login",login)
            intent.putExtra("password",password)
            sendBroadcast(intent)
        }

    }

    fun successfulLogin() {
        if (!User.getInstance()._isMainShowed) {

            val intentMain = Intent(this, Main::class.java)
            startActivity(intentMain)

        }
    }

    fun showToast(str: String){
        try {
            Toast.makeText(applicationContext,str,Toast.LENGTH_SHORT).show()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun startService(){
        if (CpcApplication.IS_APP_IN_FOREGROUND && !User.getInstance()._isService) {

            var intent = Intent(this, ConnectionManager::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

        }
    }

    fun stopService() {
        if (!CallManager.getInstance().isStarted && !User.getInstance()._isUrlOpen) {
            stopService(Intent(this, ConnectionManager::class.java))
        }
    }


    fun openUrl(url: String) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                showToast("It is not possible to open web page.")
            } else {
                User.getInstance()._isUrlOpen = true

                intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                this.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            showToast("You don't have any browser to open web page.")
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed();
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onPause() {
        sendMsg("appPause")
        CallManager.getInstance().isMinimized = true
        super.onPause()
    }

    override fun onResume() {

        startService()

        sendMsg("appResume")
        CallManager.getInstance().isMinimized = false
        User.getInstance()._isUrlOpen = false
        super.onResume()
        editTextLogin.postDelayed(ShowKeyboard(), 500) //250 sometimes doesn't run if returning from LockScreen

        if (User.getInstance().userName.isNullOrEmpty() && User.getInstance().password.isNullOrEmpty()){
            return
        }

        login(User.getInstance().userName, User.getInstance().password)

    }

    private inner class ShowKeyboard : Runnable {
        override fun run() {
            editTextLogin.isFocusableInTouchMode = true
            editTextLogin.requestFocus()
            var imm:InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextLogin, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun sendMsg(str: String){
        val intent = Intent()
        intent.action = str
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }

    fun suspendApp() {
        this.loadingPanelLogin.visibility = View.VISIBLE
        this.editTextLogin.isEnabled = false
        this.editTextPassword.isEnabled = false
        this.btnLogin.isEnabled = false
        this.editTextPassword.isFocusable = false
        this.editTextLogin.isFocusable = false
    }

    fun unSuspendApp(str: String?) {
        this.loadingPanelLogin.visibility = View.GONE
        this.editTextLogin.isEnabled = true
        this.editTextPassword.isEnabled = true
        this.btnLogin.isEnabled = true
        this.editTextPassword.isFocusableInTouchMode = true
        this.editTextLogin.isFocusableInTouchMode = true

        if (str != null && str != "") {
            showToast("Error: "+ str)
        }

    }

    private fun checkPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) === PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permissionArray: Array<String>) {
        ActivityCompat.requestPermissions(this, permissionArray,
                PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_SHORT).show()

                    User.getInstance()._isAllPermsGranted = true

                    // main logic
                } else {

                    User.getInstance()._isAllPermsGranted = false

                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
