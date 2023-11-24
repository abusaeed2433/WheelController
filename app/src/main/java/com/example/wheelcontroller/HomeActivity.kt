package com.example.wheelcontroller

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wheelcontroller.classes.DataSaver
import com.example.wheelcontroller.classes.Utility
import com.example.wheelcontroller.databinding.ActivityHomeBinding
import com.example.wheelcontroller.enums.Command
import com.example.wheelcontroller.listener.DatabaseListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant

class HomeActivity : AppCompatActivity() {

    private var binding:ActivityHomeBinding? = null
    private var isProcessing:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater);
        setContentView(binding!!.root);

    }

    private fun checkIDPass(id: String, pass: String, listener: DatabaseListener) {
        val ref = FirebaseDatabase.getInstance().reference.child("users").child(id)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var originalPass: String? = null
                val name = arrayOf<String?>(null)
                if (snapshot.exists()) {
                    originalPass = snapshot.child("password").value.toString()
                    name[0] = snapshot.child("name").value.toString()
                }
                if (pass == originalPass) {
                    val activeRef = FirebaseDatabase.getInstance().reference
                        .child("chats").child(id).child("rock/last_active")
                    activeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                try {
                                    val lastActive = snapshot.value.toString().toDouble()
                                    val curTimeStamp =
                                        (Instant.now().toEpochMilli() / 1000f).toDouble()
                                    if (curTimeStamp - lastActive <= 5000) { // 5s
                                        listener.onProcessDone(null, name[0])
                                        //binding.tvID.setText(name[0])
                                    } else {
                                        listener.onProcessDone(
                                            "Controller is not ready in wheelchair",
                                            null
                                        )
                                        DataSaver.getInstance(this@HomeActivity).clearIdPass()
                                    }
                                } catch (e: Exception) {
                                    listener.onProcessDone("Something went wrong", null)
                                    DataSaver.getInstance(this@HomeActivity).clearIdPass()
                                }
                            } else {
                                listener.onProcessDone(
                                    "Controller is not ready in wheelchair",
                                    null
                                )
                                DataSaver.getInstance(this@HomeActivity).clearIdPass()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            listener.onProcessDone("Something went wrong", null)
                            DataSaver.getInstance(this@HomeActivity).clearIdPass()
                        }
                    })
                } else {
                    listener.onProcessDone("Wrong password. Re-enter again", null)
                    DataSaver.getInstance(this@HomeActivity).clearIdPass()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onProcessDone(error.message, null)
                DataSaver.getInstance(this@HomeActivity).clearIdPass()
            }
        })
    }

}