package com.dandrzas.devicemonitor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class PlotterView extends View {

    private float[] data = new float[30];
    private int textColor = Color.RED;
    private float textDimension = 0;
    private TextPaint textPaint;
    private Paint paint;
    private float PixZeroX, PixZeroY, PixPerPointX, PixPerPointY, ScaleY;

    public PlotterView(Context context) {
        super(context);
        init(null, 0);
    }

    public PlotterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PlotterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PlotterView, defStyle, 0);

        textColor = a.getColor(
                R.styleable.PlotterView_textColor,
                textColor);

        textDimension = a.getDimension(
                R.styleable.PlotterView_textDimension,
                textDimension);

        for (int i = 0; i < 30; i++) {
            data[i] = 0;
        }
        a.recycle();

        // Set up a default TextPaint object
        textPaint = new TextPaint();
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.LEFT);

        paint = new Paint();

        // Update TextPaint and text measurements from attributes
        setTextPaint();
    }

    private void setTextPaint() {
        textPaint.setTextSize(textDimension);
        textPaint.setColor(textColor);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int contentWidth = getWidth();
        int contentHeight = getHeight();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        PixZeroX = (float) 0;
        PixZeroY = (float) contentHeight;
        PixPerPointX = (float) contentWidth / 30;
        PixPerPointY = (float) contentHeight / 10;

        // Wyznaczenie skali
        float dataMax = data[0];

        for (int i = 1; i < 30; i++) {
            if (data[i] > dataMax) dataMax = data[i];
        }
        ScaleY = (float) 1.2 * dataMax / 10;


        // Rysowanie danych
        for (int i = 0; i < 30; i++) {
            float xStart = PixZeroX + i * PixPerPointX;
            float xStop = PixZeroX + (i + 1) * PixPerPointX;
            float y = PixZeroY - (data[29 - i] * PixPerPointY / ScaleY);
            canvas.drawLine(xStart, y, xStop, y, paint);    // linia pozioma
            if (i <= 28)
                canvas.drawLine(xStop, y, xStop, PixZeroY - (data[28 - i] * PixPerPointY / ScaleY), paint);    // linia pionowa
        }

        // Rysowanie skali osi Y
        for (int i = 0; i <= 30; i++) {
            canvas.drawText(Integer.toString((int) (i * ScaleY)),
                    (int) (PixZeroX + 0.1 * PixPerPointX),
                    PixZeroY - i * PixPerPointY,
                    textPaint);
        }
    }

    public void setData(float[] data) {
        this.data = data;
    }
}
