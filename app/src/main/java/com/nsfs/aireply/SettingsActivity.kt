package com.nsfs.aireply

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nsfs.aireply.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etUrl.setText(Prefs.getUrl(this))
        binding.etToken.setText(Prefs.getToken(this))
        binding.etN.setText(Prefs.getN(this).toString())

        binding.btnSave.setOnClickListener {
            Prefs.setUrl(this, binding.etUrl.text.toString().trim())
            Prefs.setToken(this, binding.etToken.text.toString().trim())
            val n = binding.etN.text.toString().toIntOrNull() ?: 4
            Prefs.setN(this, n)
            finish()
        }
    }
}
