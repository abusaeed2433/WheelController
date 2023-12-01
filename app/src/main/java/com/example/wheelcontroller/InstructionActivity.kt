package com.example.wheelcontroller

import android.R
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.wheelcontroller.classes.SingleTvAdapter
import com.example.wheelcontroller.databinding.ActivityInstructionBinding

class InstructionActivity : AppCompatActivity() {

    private var binding:ActivityInstructionBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionBinding.inflate(layoutInflater);
        setContentView(binding!!.root);
        showData();
    }



    private fun showData(){

        val list:Array<String> = arrayOf(
            "Login with your credentials.",
            "Click on connect. Give requested permission to continue.",
            "After connecting, controller page will be opened.",
            "You can control car via virtual remote on the bottom of the screen.",
            "For voice input, click on mic icon and enter your voice commands.",
            "Available voice commands are 'Samne jao`, 'Pichone jao', 'Bame jao', 'Dane jao', 'Thamo'",
            "Camera stream is available through on the top section of the page",
            "If any error occurs, Close the app and reopen it."
        )

        val adapter:SingleTvAdapter = SingleTvAdapter(list)
        binding?.rvConnect?.adapter = adapter

    }


}
