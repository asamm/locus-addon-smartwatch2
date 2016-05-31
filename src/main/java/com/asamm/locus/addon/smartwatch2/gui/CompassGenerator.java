package com.asamm.locus.addon.smartwatch2.gui;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

public class CompassGenerator {

	// real width of c
	private int mWidth;
	private int mHeight;
	
	// main bitmap
	private Bitmap mBitmap;
	// canvas to draw
	private Canvas mCanvas;
	
	// define arrow shape
	private Path mPath;
	// paint to draw an arrow
	private Paint mPaintArrow;
	
	public CompassGenerator(int width, int height) {
		// set parameters
		this.mWidth = width;
		this.mHeight = height;
		
		// prepare required variables
		mBitmap = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
		
		// prepare path
		int step = width / 5;
		mPath = new Path();
		mPath.moveTo(-step, step);
		mPath.lineTo(0, -2 * step);
		mPath.lineTo(step, step);
		mPath.lineTo(0, 0);
		mPath.lineTo(-step, step);
		
		// prepare paint
		mPaintArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintArrow.setColor(Color.BLACK);
		mPaintArrow.setStyle(Style.FILL);
	}
	
	public Bitmap render(float angle) {
		// prepare canvas
		mCanvas.drawColor(Color.WHITE);
		
		// set correct location
		mCanvas.save();
		mCanvas.translate(mWidth / 2.0f, mHeight / 2.0f);
		mCanvas.rotate(angle);
		
		// draw compass
		mCanvas.drawPath(mPath, mPaintArrow);
		
		// restore state
		mCanvas.restore();
		
		// return image
		return mBitmap;
	}
}
