package com.example.httpproxyserver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.httpproxyserver.databinding.ActivityMainBinding
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
    var started = false
    private var future: Future<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnStart.setOnClickListener {
            if (!started || future?.isDone == true) {
                val port = binding.etPort.text.toString().toIntOrNull() ?: 1081
                future = executorService.submit(HttpProxy(port))
                binding.etPort.setText(port.toString())
                started = true
            } else {
                Toast.makeText(this, "正在运行中", Toast.LENGTH_SHORT).show()
            }
        }

    }
}