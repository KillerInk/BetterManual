package com.obsidium.bettermanual.views;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.obsidium.bettermanual.ActivityInterface;
import com.obsidium.bettermanual.OnSwipeTouchListener;

public abstract class BaseTextView extends TextView
{

    protected ActivityInterface activityInterface;

    public BaseTextView(Context context) {
        super(context);
    }

    public BaseTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SwipeTouchListener getSwipeTouchListner()
    {
        return new SwipeTouchListener(getContext());
    }

    public void setActivityInterface(ActivityInterface activityInterface)
    {
        this.activityInterface = activityInterface;
    }

    private class SwipeTouchListener extends OnSwipeTouchListener
    {
        private int m_lastDistance;
        private int m_accumulatedDistance;

        public SwipeTouchListener(Context context)
        {
            super(context);
        }

        @Override
        public boolean onScrolled(float distanceX, float distanceY)
        {
            final int distance = (int)(Math.abs(distanceX) > Math.abs(distanceY) ? distanceX : -distanceY);
            if ((m_lastDistance > 0) != (distance > 0))
                m_accumulatedDistance = distance;
            else
                m_accumulatedDistance += distance;
            m_lastDistance = distance;
            if (Math.abs(m_accumulatedDistance) > 10)
            {
                for (int i = Math.abs(m_accumulatedDistance); i > 10; i -= 10)
                {
                    BaseTextView.this.onScrolled(distance);
                }
                m_accumulatedDistance = 0;
                return true;
            }

            return false;
        }

        @Override
        public boolean onClick()
        {
            return BaseTextView.this.onClick();
        }
    }

    public abstract void onScrolled(int distance);
    public abstract boolean onClick();
}