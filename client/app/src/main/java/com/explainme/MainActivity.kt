package com.explainme
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


val IP = "192.168.0.104"

class MainActivity : AppCompatActivity() {
    val LOADING_SIZE = 10
    var loading = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fab.setOnClickListener { fab.isExpanded = !fab.isExpanded }
//        supportActionBar!!.setDisplayHomeAsUpEx`nabled(true)
//        supportActionBar!!.setDisplayShowHomeEnabled(true)
        val lm = LinearLayoutManager(this)
        val lectureAdapter = LectureAdapter(ArrayList(), this)
        recycler_view.apply {
            setHasFixedSize(false)
            layoutManager = lm
            adapter = lectureAdapter
            addOnScrollListener(object: OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = lm.childCount
                    val totalItemCount = lm.itemCount
                    val pastVisibleItems = lm.findFirstVisibleItemPosition()
                    if (dy > 0 && !loading && visibleItemCount + pastVisibleItems >= totalItemCount) {
                        loading = true
                        thread {
                            val lectures = loadLectures(totalItemCount, totalItemCount + LOADING_SIZE)
                            runOnUiThread {
                                lectureAdapter.addLectures(lectures)
                                loading = false
                            }
                        }
                        Log.i("ExplainMe", "Loading...")
                    }
                }
            })
        }
        thread {
            val lectures = loadLectures(0, LOADING_SIZE)
            runOnUiThread {
                lectureAdapter.addLectures(lectures)
                loading = false
            }
        }
        create_lecture_button.setOnClickListener {
            val title_field = sheet.title_field
            Log.i("ExplainMe", title_field.text.toString())
            if (title_field.text.toString().isEmpty() || description_field.text.toString().isEmpty()) {
                Snackbar.make(recycler_view, "Please fill title field", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val c = Calendar.getInstance()
            val mYear = c.get(Calendar.YEAR)
            val mMonth = c.get(Calendar.MONTH)
            val mDay = c.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { p0, year, month, day ->
                    val mHour = c.get(Calendar.HOUR)
                    val mMinute = c.get(Calendar.MINUTE)
                    TimePickerDialog(this,
                        TimePickerDialog.OnTimeSetListener { _, minute, hour ->
                            val cal = GregorianCalendar()
                            cal.timeZone = TimeZone.getTimeZone("GMT")
                            cal.timeZone = Calendar.getInstance().timeZone
                            cal.set(year, month, day, hour, minute)
                            Log.i("ExplainMe", (cal.timeInMillis / 1000).toString())
                            val time = cal.timeInMillis / 1000
                            Log.i("ExplainMe", time.toString())
                            val title = title_field.text.toString()
                            val description = description_field.text.toString()
                            val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)!!
                            val user = account.email!!
                            Log.i("ExplainMe", "Creating new lecture")
                            title_field.text?.clear()
                            description_field.text?.clear()
                            thread {
                                register_lecture(Lecture(title, description, user, time.toInt(), ""))
                                runOnUiThread {
                                    fab.isExpanded = false
                                }
                            }
                        }, mHour, mMinute, true).show()
                }, mYear, mMonth, mDay).show()
        }
    }

    override fun onBackPressed() {
        if (fab.isExpanded) {
            fab.isExpanded = false
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mymenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        signOut()
        return super.onOptionsItemSelected(item)
    }

    private fun register_lecture(lecture: Lecture) {
        try {
            val url = URL("http://$IP:8000")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.setRequestProperty("Content-Type", "application/json; utf-8")
            con.setRequestProperty("Accept", "application/json")
            con.doOutput = true
            val jsonObject = JSONObject()
            jsonObject.put("type", "add_lecture")
            jsonObject.put("title", lecture.title)
            jsonObject.put("description", lecture.description)
            jsonObject.put("author", lecture.author)
            jsonObject.put("time", lecture.time)
            Log.i("ExplainMe", jsonObject.toString())
            con.outputStream.use { os ->
                val input: ByteArray = jsonObject.toString().toByteArray()
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
                val str = response.toString()
                Log.i("ExplainMe", str)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLectures(begin: Int, end: Int): Array<Lecture> {
        try {
            val url = URL("http://$IP:8000")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.setRequestProperty("Content-Type", "application/json; utf-8")
            con.setRequestProperty("Accept", "application/json")
            con.doOutput = true
            val jsonObject = JSONObject()
            jsonObject.put("type", "get_lectures")
            jsonObject.put("begin", begin)
            jsonObject.put("end", end)
            con.outputStream.use { os ->
                val input: ByteArray = jsonObject.toString().toByteArray()
                os.write(input, 0, input.size)
            }
            var jsonArray: JSONArray? = null
            BufferedReader(
                InputStreamReader(con.inputStream, "utf-8")
            ).use { br ->
                val response = StringBuilder()
                var responseLine: String?
                while (br.readLine().also { responseLine = it } != null) {
                    response.append(responseLine!!.trim { it <= ' ' })
                }
                val str = response.toString()
                if (str[str.length - 1] != ']') {
                    Log.i("ExplainMe", "LOL:$str")
                    jsonArray = JSONArray("$str]")
                } else {
                    Log.i("ExplainMe", str)
                    jsonArray = JSONArray(str)
                }
            }
            return (0 until jsonArray!!.length()).map {
                val json = jsonArray!!.get(it) as JSONObject
                Lecture(
                    json.getString("title"),
                    json.getString("description"),
                    json.getString("author"),
                    json.getInt("time"),
                    json.getString("zoom_url")
                )
            }.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return arrayOf()
        }
    }

    private fun signOut() {
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                finish()
            }
    }

}