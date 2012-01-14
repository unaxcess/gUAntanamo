package org.ua2.guantanamo.gui;

import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

abstract class NavGestureDetector extends SimpleOnGestureListener {

	private int thresholdX;
	private int thresholdY;

	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
	private static final String TAG = NavGestureDetector.class.getName();
	
	public NavGestureDetector(int thresholdX, int thresholdY) {
		this.thresholdX = thresholdX;
		this.thresholdY = thresholdY;
	}

	private int getDirection(float p1, float p2, int threshold, float velocity) {
		if(Math.abs(velocity) > SWIPE_THRESHOLD_VELOCITY) {
			if(p2 - p1 > threshold) {
				Log.d(TAG, "getDirection " + p2 + " - " + p1 + " > " + threshold + " exit 1");
				return 1;
			} else if(p1 - p2 > threshold) {
				Log.d(TAG, "getDirection " + p1 + " - " + p2 + " > " + threshold + " exit -1");
				return -1;
			}
		}
		
		Log.d(TAG, "getDirection " + p1 + " -vs- " + p2 + " @ " + velocity + " exit 0");
		return 0;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		NavType direction = null;
		
		if(thresholdX > 0 && getDirection(e1.getX(), e2.getX(), thresholdX, velocityX) == 1 && getDirection(e1.getY(), e2.getY(), thresholdY / 2, velocityY) == 0) {
			// Right
			direction = NavType.PREV_SIBLING;
			
		} else if(thresholdX > 0 && getDirection(e1.getX(), e2.getX(), thresholdX, velocityX) == -1 && getDirection(e1.getY(), e2.getY(), thresholdY / 2, velocityY) == 0) {
			// Left
			direction = NavType.NEXT_SIBLING;
			
		} else if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX / 2) == 0 && thresholdY > 0 && getDirection(e1.getY(), e2.getY(), thresholdY, velocityY) == 1) {
			// Down
			direction = NavType.PREV_PARENT;
			
		} else if(getDirection(e1.getX(), e2.getX(), thresholdX, velocityX / 2) == 0 && thresholdY > 0 && getDirection(e1.getY(), e2.getY(), thresholdY, velocityY) == -1) {
			// Up
			direction = NavType.NEXT_PARENT;
			
		}
		
		Log.i(NavGestureDetector.TAG, "Fling " + e1.getX() + " -> " + e2.getX() + " @ " + velocityX + " , " + e1.getY() + " -> " + e2.getY() + " @ " + velocityY + " -> " + direction);
		
		if(direction != null) {
			performAction(direction);
			
			return true;
		}
	
		return super.onFling(e1, e2, velocityX, velocityY);
	}

	protected abstract void performAction(NavType direction);
}
