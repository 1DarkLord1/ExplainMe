package com.explainme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val signInButton: SignInButton = findViewById(R.id.sign_in_button)
        signInButton.setSize(SignInButton.SIZE_STANDARD)
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, 0)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("ExplainMe", "onActivityResult")
        try {
            super.onActivityResult(requestCode, resultCode, data)
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)!!
            val photoUrl = account.photoUrl.toString()
            val email = account.email
            val displayName = account.displayName
            val jsonParam = JSONObject()
            jsonParam.put("type", "add_user")
            jsonParam.put("google_mail", email)
            jsonParam.put("photo_url", photoUrl)
            jsonParam.put("display_name", displayName)
            Log.i("ExplainMe", jsonParam.toString())
            thread { registerUser(jsonParam) }
        } catch (e: Exception) {
            e.printStackTrace()
            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, 0)
        }
    }
    private fun registerUser(user: JSONObject) {
        val url = URL("http://$IP:8000")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json; utf-8")
        con.setRequestProperty("Accept", "application/json")
        con.doOutput = true
        con.outputStream.use { os ->
            val input: ByteArray = user.toString().toByteArray()
            os.write(input, 0, input.size)
        }
        BufferedReader(
            InputStreamReader(con.inputStream, "utf-8")
        ).use { br ->
            val response = StringBuilder()
            var responseLine: String?
            while (br.readLine().also { responseLine = it } != null) {
                response.append(responseLine!!.trim { it <= ' ' })
            }
            println(response.toString())
        }
        runOnUiThread {
            Log.i("ExplainMe", "Lol")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}