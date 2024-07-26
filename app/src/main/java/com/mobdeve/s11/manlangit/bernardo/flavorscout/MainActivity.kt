package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var openFilterBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.openFilterBtn = findViewById(R.id.openFilterBtn)
        openFilterBtn.setOnClickListener {
            openFilter()
        }

    }

    private fun openFilter() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.filter_bottomsheet_layout, null)

        val distanceSeekBar: SeekBar = bottomSheetView.findViewById(R.id.distanceSeekBar)
        val distanceTv: TextView = bottomSheetView.findViewById(R.id.distanceNumTv)

        val star5: RadioButton = bottomSheetView.findViewById(R.id.star5)
        val star4: RadioButton = bottomSheetView.findViewById(R.id.star4)
        val star3: RadioButton = bottomSheetView.findViewById(R.id.star3)
        val star2: RadioButton = bottomSheetView.findViewById(R.id.star2)
        val star1: RadioButton = bottomSheetView.findViewById(R.id.star1)

        val radioButtons = listOf(star5, star4, star3, star2, star1)

        for (radioButton in radioButtons) {
            radioButton.setOnClickListener {
                for (rb in radioButtons) {
                    rb.isChecked = rb == radioButton
                }
            }
        }

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                distanceTv.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }
}