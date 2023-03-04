package com.example.httpproxyserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.httpproxyserver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnStart.setOnClickListener {
            val port = binding.etPort.text.toString().toIntOrNull() ?: 1081
            startService(HttpProxyService.newStartIntent(this, port))
            binding.etPort.setText(port.toString())
        }
        binding.btnStop.setOnClickListener {
            stopService(HttpProxyService.newStopIntent(this))
        }
        useSystemProxy = binding.cbUseSystemProxy.isChecked
        binding.cbUseSystemProxy.setOnCheckedChangeListener { _, isChecked ->
            useSystemProxy = isChecked
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(HttpProxyService.newStopIntent(this))
    }

}