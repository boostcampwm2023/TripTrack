package com.boostcampwm2023.snappoint.presentation.videoedit

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.boostcampwm2023.snappoint.R
import com.boostcampwm2023.snappoint.presentation.util.toDisplayTime


@BindingAdapter("startTime", "endTime", "recentTime", requireAll = true)
fun setEditTime(view: TextView, startTime: Long, endTime: Long, recentTime: Long,){
    Log.d("TAG", "setEditTime: $startTime, $endTime, $recentTime")
    val wholeTime = (endTime - startTime).toDisplayTime()
    val presentTime = (recentTime - startTime).toDisplayTime()
    view.setText("$presentTime / $wholeTime")
}

@BindingAdapter("isPlaying")
fun setSrc(view: ImageView, isPlaying: Boolean){
    if(isPlaying){
        view.setImageResource(R.drawable.icon_player_pause)
    }else{
        view.setImageResource(R.drawable.icon_player_play)
    }
}
