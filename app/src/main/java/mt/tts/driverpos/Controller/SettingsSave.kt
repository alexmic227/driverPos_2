package mt.tts.driverpos.Controller

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_delivery_note.*
import kotlinx.android.synthetic.main.activity_delivery_note.scr_02_Main
import kotlinx.android.synthetic.main.activity_settings.*
import mt.tts.driverpos.R


class SettingsSave : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settings: SharedPreferences = applicationContext.getSharedPreferences("driverpos", MODE_PRIVATE)

        //Loading of settings
        val driverID: String = settings.getString("driverid", "").toString()
        DriverID.setText(driverID)

        val shareEmail: String = settings.getString("shareemail", "").toString()
        ShareEmail.setText(shareEmail)

        val passCode: String = settings.getString("passcode", "").toString()
        LockPass.setText(passCode)

        saveBtn.setOnClickListener() {
            var drvid = DriverID.text.toString()
            var shareeml = ShareEmail.text.toString()
            var passwrd = LockPass.text.toString()

            //saving of settings
            val edit = settings.edit()
            edit.putString("driverid", drvid.trim())
            edit.putString("shareemail", shareeml.trim())
            edit.putString("passcode", passwrd.trim())
            edit.apply()
        }

        scr_02_Main.setOnClickListener {
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
        }

    }
}