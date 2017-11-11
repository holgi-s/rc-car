// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.library.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;

public class ZoomPanTextureView extends TextureView
{
    private GestureDetectorCompat panDetector;
    private ScaleGestureDetector scaleDetector;
    private int fitWidth, fitHeight;
    private PointF fitZoom = new PointF(1, 1);
    private PointF pan = new PointF(0, 0);
    private float zoom = 1;
    private float minZoom = 0.1f;
    private float maxZoom = 10;

    //******************************************************************************
    // ZoomPanTextureView
    //******************************************************************************
    public ZoomPanTextureView(Context context)
    {
        super(context);
        initialize(context);
    }

    //******************************************************************************
    // ZoomPanTextureView
    //******************************************************************************
    public ZoomPanTextureView(Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    //******************************************************************************
    // ZoomPanTextureView
    //******************************************************************************
    public ZoomPanTextureView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    //******************************************************************************
    // initialize
    //******************************************************************************
    private void initialize(Context context)
    {
        // create the gesture recognizers
        panDetector = new GestureDetectorCompat(context, new PanListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    //******************************************************************************
    // onTouchEvent
    //******************************************************************************
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
		return panDetector.onTouchEvent(event) || scaleDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    //******************************************************************************
    // setZoomRange
    //******************************************************************************
    public void setZoomRange(float minZoom, float maxZoom)
    {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    //******************************************************************************
    // setVideoSize
    //******************************************************************************
    public void setVideoSize(int videoWidth, int videoHeight)
    {
        // get the aspect ratio
        float aspectRatio = (float)videoHeight / videoWidth;

        // get the view size
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // get the fitted size
        if (viewHeight > (int)(viewWidth * aspectRatio))
        {
            fitWidth = viewWidth;
            fitHeight = (int)(viewWidth * aspectRatio);
        }
        else
        {
            fitWidth = (int)(viewHeight / aspectRatio);
            fitHeight = viewHeight;
        }

        // get the fitted zoom
        fitZoom.x = (float)fitWidth / viewWidth;
        fitZoom.y = (float)fitHeight / viewHeight;

        // clear the transform
		setZoomPan(1, 0, 0);
    }

    //******************************************************************************
    // setZoom
    //******************************************************************************
    public void setZoom(float newZoom)
    {
        zoom = newZoom;
        checkPan();
        setTransform();
    }

	//******************************************************************************
    // setPan
    //******************************************************************************
    public void setPan(PointF newPan)
    {
        pan = newPan;
        checkPan();
        setTransform();
    }

	//******************************************************************************
	// setPan
	//******************************************************************************
	public void setPan(float newPanX, float newPanY)
	{
		setPan(new PointF(newPanX, newPanY));
	}

	//******************************************************************************
	// setZoomPan
	//******************************************************************************
	public void setZoomPan(float newZoom, PointF newPan)
	{
		zoom = newZoom;
		pan = newPan;
		checkPan();
		setTransform();
	}

	//******************************************************************************
	// setZoomPan
	//******************************************************************************
	public void setZoomPan(float newZoom, float newPanX, float newPanY)
	{
		setZoomPan(newZoom, new PointF(newPanX, newPanY));
	}

	//******************************************************************************
    // checkPan
    //******************************************************************************
    private void checkPan()
    {
        PointF maxPan = getMaxPan();

        if (maxPan.x == 0) pan.x = 0;
        else if (pan.x < -maxPan.x) pan.x = -maxPan.x;
        else if (pan.x > maxPan.x) pan.x = maxPan.x;

        if (maxPan.y == 0) pan.y = 0;
        else if (pan.y < -maxPan.y) pan.y = -maxPan.y;
        else if (pan.y > maxPan.y) pan.y = maxPan.y;
    }

    //******************************************************************************
    // getMaxPan
    //******************************************************************************
    private PointF getMaxPan()
    {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        return new PointF(Math.max(Math.round((fitWidth * zoom - viewWidth) / 2), 0),
                          Math.max(Math.round((fitHeight * zoom - viewHeight) / 2), 0));
    }

    //******************************************************************************
    // setTransform
    //******************************************************************************
    private void setTransform()
    {
        // get the view size
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // scale relative to the center
        Matrix transform = new Matrix();
        transform.postScale(fitZoom.x * zoom, fitZoom.y * zoom, viewWidth / 2, viewHeight / 2);

        // add the panning
        if (pan.x != 0 || pan.y != 0)
        {
            transform.postTranslate(pan.x, pan.y);
        }

        // set the transform
        setTransform(transform);
        invalidate();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // PanListener
    ////////////////////////////////////////////////////////////////////////////////
    private class PanListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if (e2.getPointerCount() == 1 && zoom > 1)
            {
                setPan(pan.x - distanceX, pan.y - distanceY);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
			setZoomPan(1, 0, 0);
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // ScaleListener
    ////////////////////////////////////////////////////////////////////////////////
    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener
    {
        private float startZoom = 1;
        private PointF center = new PointF();

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float newZoom = startZoom * detector.getScaleFactor();
            newZoom = Math.max(minZoom, Math.min(newZoom, maxZoom));
            if (newZoom != zoom)
            {
                PointF offset = new PointF(detector.getFocusX() - center.x, detector.getFocusY() - center.y);
                PointF focus = new PointF((offset.x - pan.x) / zoom, (offset.y - pan.y) / zoom);
				setZoomPan(newZoom, offset.x - focus.x * newZoom, offset.y - focus.y * newZoom);
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            startZoom = zoom;
            center.x = getWidth() / 2;
            center.y = getHeight() / 2;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector)
        {
        }
    }
}
