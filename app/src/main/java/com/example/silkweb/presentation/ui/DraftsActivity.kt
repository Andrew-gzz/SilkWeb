package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.local.PostEntity
import com.example.silkweb.presentation.adapter.DraftAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DraftsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DraftAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drafts)

        recyclerView = findViewById(R.id.id_recyclerDrafts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        cargarBorradores()

        // Botón para regresar
        val btnBack = findViewById<android.widget.ImageButton>(R.id.id_back)
        btnBack.setOnClickListener { finish() }
    }
    override fun onResume() {
        super.onResume()
        cargarBorradores()
    }
    private fun abrirParaEditar(draft: PostEntity) {
        val intent = Intent(this, PostActivity::class.java)
        intent.putExtra("draft_id", draft.id)
        startActivity(intent)
    }
    private fun cargarBorradores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DraftsActivity)
            val drafts = db.postDaoLocal().getDrafts()

            withContext(Dispatchers.Main) {
                adapter = DraftAdapter(drafts) { draft ->
                    abrirParaEditar(draft)
                }
                recyclerView.adapter = adapter
            }
        }
    }

}
