package com.example.wheelcontroller

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wheelcontroller.classes.DataSaver
import com.example.wheelcontroller.classes.EachLog
import com.example.wheelcontroller.classes.LogAdapter
import com.example.wheelcontroller.databinding.ActivityLogBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private lateinit var logAdapter:LogAdapter
    private val allLogs: MutableList<EachLog> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLogAdapter()
        readlLogs()
    }

    private fun initLogAdapter() {
        logAdapter = LogAdapter(this, allLogs)
        binding.rvLogs.adapter = logAdapter
    }


    private fun readlLogs() {
        val id = DataSaver.getInstance(this).id
        val logRef = FirebaseDatabase.getInstance().reference.child("chats/$id/rock/logs")

        logRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val type = snapshot.child("type").value.toString()
                val message = snapshot.child("message").value.toString()
                val ts = snapshot.child("timestamp").value.toString()
                var timestamp: String? = "-- -- ---"
                try {
                    val realTs = (ts.toDouble() * 1000).toLong()
                    timestamp = getFormattedTS(realTs)
                } catch (ignored: Exception) {
                }
                val log = EachLog(type, message, timestamp)
                updateLogInAdapter(log)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getFormattedTS(ts: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
        val pattern = "EEE MMM dd'\n'hh:mm:ssa"
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return formatter.format(ldt).uppercase(Locale.getDefault())
    }

    private fun updateLogInAdapter(log: EachLog) {
        allLogs.add(log)
        binding.myProgressLog.visibility = View.GONE
        logAdapter.notifyItemInserted(allLogs.size - 1)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.rvLogs.smoothScrollToPosition(allLogs.size - 1)
        }, 120)
    }


}