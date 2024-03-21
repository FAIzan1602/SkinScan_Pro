package com.example.skinscanpro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        val cancerBtn=findViewById<Button>(R.id.cancer_btn)
        cancerBtn.setOnClickListener{
            val Intent=Intent(this,MainActivity::class.java)
            startActivity(Intent)
        }
        val diseaseBtn=findViewById<Button>(R.id.disease_btn)
        diseaseBtn.setOnClickListener{
            val Intent=Intent(this,Disease::class.java)
            startActivity(Intent)
        }
    }
}