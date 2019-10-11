package us.visitel.mobileclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageButton;


public class DisabledButton extends Button {
    private boolean enabled;

    public DisabledButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        enabled = true;
        this.setTextColor(Color.WHITE);
    }

    public void setEnabled(boolean a) {
        enabled = a;
        if (enabled)
            this.setTextColor(Color.WHITE);
        else
            this.setTextColor(Color.GRAY);
        this.invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
