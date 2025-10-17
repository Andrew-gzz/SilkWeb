package com.example.silkweb.presentation.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.presentation.adapter.DraftAdapter
import com.example.silkweb.presentation.adapter.DraftModel

class DraftsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DraftAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drafts)

        recyclerView = findViewById(R.id.id_recyclerFeed)

        // 📘 Lista temporal de borradores (mock)
        val draftList = listOf(
            DraftModel("andres_dev", "Borrador ${1}", "1"),
            DraftModel("carlitos_09", "Borrador ${2}", "2"),
            DraftModel("maria_lrn", "Borrador ${3}", "3"),
            DraftModel("gis_team", "Borrador ${4}", "4")
        )

        adapter = DraftAdapter(draftList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Botón para regresar
        val btnBack = findViewById<android.widget.ImageButton>(R.id.id_back)
        btnBack.setOnClickListener { finish() }
    }
}