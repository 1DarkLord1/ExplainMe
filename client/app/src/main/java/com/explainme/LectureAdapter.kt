package com.explainme
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.lecture_card.view.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


class LectureAdapter(private val lectures: ArrayList<Lecture>,
                     private val context: Activity) :
    RecyclerView.Adapter<LectureAdapter.LectureHolder>() {

    class LectureHolder(val cardView: CardView, var expanded: Boolean = false) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): LectureHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.lecture_card, parent, false) as CardView
        val holder = LectureHolder(cardView)
        cardView.setOnClickListener {
            TransitionManager.beginDelayedTransition(context.recycler_view)
            if (!holder.expanded) {
                cardView.description.visibility = View.VISIBLE
                cardView.time_left.visibility = View.VISIBLE
                cardView.url.visibility = View.VISIBLE
            } else {
                cardView.description.visibility = View.GONE
                cardView.time_left.visibility = View.GONE
                cardView.url.visibility = View.GONE
            }
            holder.expanded = !holder.expanded
        }
        return holder
    }

    override fun onBindViewHolder(holder: LectureHolder, position: Int) {
        holder.cardView.title_field.text = lectures[position].title
        holder.cardView.description.text = lectures[position].description
        val diff = (lectures[position].time - System.currentTimeMillis() / 1000) / 60
        val hour = diff / 60
        val minute = diff % 60
        holder.cardView.time_left.text = "Starts in %02d:%02d".format(hour, minute)
        holder.cardView.url.autoLinkMask = 0
        holder.cardView.url.isClickable = true
        holder.cardView.url.movementMethod = LinkMovementMethod.getInstance()
        holder.cardView.url.text = Html.fromHtml("<a href='${lectures[position].url}'>${lectures[position].url}</a>")
        holder.cardView.url.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(lectures[position].url))
            context.startActivity(browserIntent)
        }
        thread{
            val user = getUser(lectures[position].author)
            context.runOnUiThread {
                holder.cardView.author_field.text = user.display_name
                if (user.photo_url != "null") {
                    Picasso.with(context).load(user.photo_url).into(holder.cardView.user_photo)
                }
            }
        }
    }

    override fun getItemCount() = lectures.size

    fun addLectures(newLectures: Array<Lecture>) {
        lectures.addAll(newLectures)
        notifyItemRangeInserted(itemCount - newLectures.size + 1, itemCount)
    }

    private fun getUser(mail: String): User {
        val url = URL("http://$IP:8000")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json; utf-8")
        con.setRequestProperty("Accept", "application/json")
        con.doOutput = true
        val jsonObject = JSONObject()
        jsonObject.put("type", "get_user_by_mail")
        jsonObject.put("google_mail", mail)
        con.outputStream.use { os ->
            val input: ByteArray = jsonObject.toString().toByteArray()
            os.write(input, 0, input.size)
        }
        var result: JSONObject? = null
        BufferedReader(InputStreamReader(con.inputStream, "utf-8")).use { br ->
            val response = StringBuilder()
            var responseLine: String?
            while (br.readLine().also { responseLine = it } != null) {
                response.append(responseLine!!.trim { it <= ' ' })
            }
            val str = response.toString()
            result = if (str[str.length - 1] != '}') {
                Log.i("ExplainMe", "KEK!$str")
                JSONObject("$str}")
            } else {
                Log.i("ExplainMe", str)
                JSONObject(str)
            }
        }
        return User(
            mail,
            result!!.getString("photo_url"),
            result!!.getString("display_name")
        )
    }
}