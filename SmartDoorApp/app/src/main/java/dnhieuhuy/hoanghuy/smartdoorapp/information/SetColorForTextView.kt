package dnhieuhuy.hoanghuy.smartdoorapp.information

import android.content.Context
import android.graphics.Color
import android.widget.TextView

/**
 * Created by Administrator on 04/11/2017.
 */
class SetColorForTextView(context: Context)
{
    private val context = context
    fun setColorTV(string: String, textView: TextView)
    {
        if(string.contains("ERROR") || string.contains("NO IP"))
        {
            textView.setTextColor(Color.RED)
        }else
            textView.setTextColor(context.resources.getColor(android.R.color.holo_green_dark))
    }
}