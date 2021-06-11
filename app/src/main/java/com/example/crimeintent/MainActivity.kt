package com.example.crimeintent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragement_container)

        if(currentFragment == null){
            val fragment = CrimeFragement()
            supportFragmentManager.beginTransaction().add(R.id.fragement_container, fragment).commit()
        }
    }
}