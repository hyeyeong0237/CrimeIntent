package com.example.crimeintent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(),
    CrimeListFragment.Callbacks {


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragement_container)

            if (currentFragment == null) {
                val fragment = CrimeListFragment.newInstance()
                supportFragmentManager.beginTransaction().add(R.id.fragement_container, fragment)
                    .commit()
            }
        }

    override fun onCrimeSelected(crimeID: UUID) {
        val fragement = CrimeFragement.newInstance(crimeID)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragement_container,fragement)
            .addToBackStack(null)
            .commit()
    }

}