package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.silkweb.R
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Override the onCreate method to set the content view to the activity_main layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkUserSession()
        replaceFragment(FeedFragment())
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.id_home -> replaceFragment(FeedFragment())
                R.id.id_search -> replaceFragment(SearchFragment())
                R.id.id_add -> {
                    val intent = Intent(this, PostActivity::class.java)
                    startActivity(intent)
                }
                R.id.id_bookmarks -> replaceFragment(FavoriteFragment())
                R.id.id_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }
    override fun onResume() {
        super.onResume()
        checkUserSession()
    }
    private fun checkUserSession() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val userDao = db.userDaoLocal()
            val user = userDao.getUser()
            if (user == null) {
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}