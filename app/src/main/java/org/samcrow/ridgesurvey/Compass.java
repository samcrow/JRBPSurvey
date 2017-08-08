/*
 * Copyright 2017 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Displays a compass
 */
public class Compass extends View {

    /**
     * The paint used to draw text
     */
    private Paint mNorthPaint;

    private Paint mBackgroundPaint;

    private Paint mSouthPaint;

    /**
     * The currently displayed heading, in the range [0, 360)
     */
    private double mHeading;

    private final Path mArrow = new Path();

    public Compass(Context context) {
        super(context);
        init(context);
    }

    public Compass(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Compass(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        final byte alpha = 100;
        mNorthPaint = new Paint();
        mNorthPaint.setColor(Color.argb(alpha, 0xFF, 0x0, 0x0));
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.argb(alpha, 0x30, 0x30, 0x30));
        mSouthPaint = new Paint();
        mSouthPaint.setColor(Color.argb(alpha, 0, 0, 0));
        mHeading = 0;
    }

    public double getHeading() {
        return mHeading;
    }

    public void setHeading(double heading) {
        final double actualHeading = (heading + 360.0) % 360.0;
        if (actualHeading != mHeading) {
            mHeading = actualHeading;
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float halfWidth = canvas.getWidth() / 2.0f;
        final float halfHeight = canvas.getHeight() / 2.0f;


        // Draw background
        canvas.drawCircle(halfWidth, halfHeight, halfWidth, mBackgroundPaint);

        // Move the origin to the center and rotate
        canvas.translate(halfWidth, halfHeight);
        canvas.rotate((float) -mHeading);

        // Draw the north arrow
        final float arrowWidth = canvas.getWidth() / 8.0f;
        mArrow.reset();
        mArrow.moveTo(0, -halfHeight);
        mArrow.lineTo(-arrowWidth / 2.0f, 0);
        mArrow.lineTo(arrowWidth / 2.0f, 0);
        mArrow.close();

        canvas.drawPath(mArrow, mNorthPaint);
        // Rotate 180º more and draw the south arrow
        canvas.rotate(180);
        canvas.drawPath(mArrow, mSouthPaint);
    }


}
