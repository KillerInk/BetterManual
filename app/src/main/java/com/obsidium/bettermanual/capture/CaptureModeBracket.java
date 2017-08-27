package com.obsidium.bettermanual.capture;

import android.util.Pair;

import com.github.ma1co.pmcademo.app.DialPadKeysEvents;
import com.obsidium.bettermanual.ActivityInterface;
import com.obsidium.bettermanual.CameraUtil;
import com.obsidium.bettermanual.views.ExposureModeView;
import com.sony.scalar.hardware.CameraEx;

public class CaptureModeBracket extends CaptureMode implements  CameraEx.ShutterSpeedChangeListener, DialPadKeysEvents {

    // Bracketing
    private int             m_bracketStep;  // in 1/3 stops
    private int             m_bracketMaxPicCount;
    private int             m_bracketPicCount;
    private int             m_bracketShutterDelta;
    private Pair<Integer, Integer> m_bracketNextShutterSpeed;
    private int             m_bracketNeutralShutterIndex;

    private final int BRACKET_NON = 0;
    private final int BRACKET_STEP = 1;
    private final int BRACKET_PICCOUNT = 2;
    private int currentDialMode = BRACKET_NON;


    public CaptureModeBracket(ActivityInterface activityInterface)
    {
        super(activityInterface);
    }

    @Override
    public void reset() {
        calcMaxBracketPicCount();
        updateBracketPicCount();
    }

    @Override
    public void prepare() {
        if (isActive())
            abort();
        else
        {
            if (activityInterface.getExposureMode().get() != ExposureModeView.ExposureModes.manual)
            {
                activityInterface.showMessageDelayed("Scene mode must be set to manual");
                return;
            }
            if (activityInterface.getIso().getCurrentIso() == 0)
            {
                activityInterface.showMessageDelayed("ISO must be set to manual");
                return;
            }

            activityInterface.setLeftViewVisibility(false);

            m_bracketPicCount = 3;
            m_bracketStep = 3;
            m_bracketShutterDelta = 0;
            updateBracketStep();

            // Remember current shutter speed
            m_bracketNeutralShutterIndex = CameraUtil.getShutterValueIndex(activityInterface.getCamera().getShutterSpeed());
        }
    }

    @Override
    public void startShooting() {
        activityInterface.hideHintMessage();
        activityInterface.hideMessage();
        // Take first picture at set shutter speed
        activityInterface.getCamera().takePicture();
    }

    @Override
    public void abort() {
        activityInterface.getMainHandler().removeCallbacks(m_countDownRunnable);
        //m_handler.removeCallbacks(m_timelapseRunnable);
        isActive = false;
        activityInterface.showMessageDelayed("Bracketing finished");
        activityInterface.getCamera().startPreview();
        activityInterface.getCamera().startDisplay();

        // Update controls
        activityInterface.hideHintMessage();
        activityInterface.setLeftViewVisibility(true);
        activityInterface.getExposureMode().updateImage();
        activityInterface.getDriveMode().updateImage();

        activityInterface.setActiveViewFlag(activityInterface.getPreferences().getViewFlags(activityInterface.getActiveViewsFlag()));
        activityInterface.updateViewVisibility();

        // Reset to previous shutter speed
        final int shutterDiff = m_bracketNeutralShutterIndex - CameraUtil.getShutterValueIndex(activityInterface.getCamera().getShutterSpeed());
        if (shutterDiff != 0)
            activityInterface.getCamera().adjustShutterSpeed(-shutterDiff);
    }

    @Override
    public void onShutter(int i) {
        if (i == 0)
        {
            if (--m_bracketPicCount == 0)
                abort();
            else
            {
                m_bracketShutterDelta += m_bracketStep;
                final int shutterIndex = CameraUtil.getShutterValueIndex(activityInterface.getCamera().getShutterSpeed());
                if (m_bracketShutterDelta % 2 == 0)
                {
                    // Even, reduce shutter speed
                    m_bracketNextShutterSpeed = new Pair<Integer, Integer>(CameraUtil.SHUTTER_SPEEDS[shutterIndex + m_bracketShutterDelta][0],
                            CameraUtil.SHUTTER_SPEEDS[shutterIndex + m_bracketShutterDelta][1]);
                    activityInterface.getCamera().adjustShutterSpeed(-m_bracketShutterDelta);
                }
                else
                {
                    // Odd, increase shutter speed
                    m_bracketNextShutterSpeed = new Pair<Integer, Integer>(CameraUtil.SHUTTER_SPEEDS[shutterIndex - m_bracketShutterDelta][0],
                            CameraUtil.SHUTTER_SPEEDS[shutterIndex - m_bracketShutterDelta][1]);
                    activityInterface.getCamera().adjustShutterSpeed(m_bracketShutterDelta);
                }
            }
        }
        else
        {
            abort();
        }
    }

    @Override
    public void increment() {
        if (m_bracketStep < 9)
        {
            ++m_bracketStep;
            updateBracketStep();
        }
    }

    @Override
    public void decrement() {
        if (m_bracketStep > 1)
        {
            --m_bracketStep;
            updateBracketStep();
        }
    }

    public void decrementPicCount()
    {
        if (m_bracketPicCount > 3)
        {
            m_bracketPicCount -= 2;
            updateBracketPicCount();
        }
    }

    public void incrementPicCount()
    {
        if (m_bracketPicCount < m_bracketMaxPicCount)
        {
            m_bracketPicCount += 2;
            updateBracketPicCount();
        }
    }

    protected void calcMaxBracketPicCount()
    {
        final int index = CameraUtil.getShutterValueIndex(activityInterface.getCamera().getShutterSpeed());
        final int maxSteps = Math.min(index, CameraUtil.SHUTTER_SPEEDS.length - 1 - index);
        m_bracketMaxPicCount = (maxSteps / m_bracketStep) * 2 + 1;
    }

    protected void updateBracketStep()
    {

        final int mod = m_bracketStep % 3;
        final int ev;
        if (mod == 0)
            ev = 0;
        else if (mod == 1)
            ev = 3;
        else
            ev = 7;
        activityInterface.showMessage(String.format("%d.%dEV", m_bracketStep / 3, ev));
    }

    protected void updateBracketPicCount()
    {
        activityInterface.showMessage(String.format("%d pictures", m_bracketPicCount));
    }


    @Override
    public void onShutterSpeedChange(CameraEx.ShutterSpeedInfo shutterSpeedInfo, CameraEx cameraEx)
    {
        activityInterface.getShutter().updateShutterSpeed(shutterSpeedInfo.currentShutterSpeed_n, shutterSpeedInfo.currentShutterSpeed_d);
        if (m_bracketNextShutterSpeed != null)
        {

            if (shutterSpeedInfo.currentShutterSpeed_n == m_bracketNextShutterSpeed.first &&
                    shutterSpeedInfo.currentShutterSpeed_d == m_bracketNextShutterSpeed.second)
            {
                // Focus speed adjusted, take next picture
                activityInterface.getCamera().takePicture();
            }
        }
    }

    @Override
    public boolean onUpperDialChanged(int value) {
        if (currentDialMode == BRACKET_STEP)
        {
            if (value <0)
                decrement();
            else
                increment();
        }
        else
        if (currentDialMode == BRACKET_PICCOUNT)
        {
            if (value < 0)
                decrementPicCount();
            else
                incrementPicCount();
        }
        return false;
    }

    @Override
    public boolean onLowerDialChanged(int value) {
        return false;
    }

    @Override
    public boolean onUpKeyDown() {
        return false;
    }

    @Override
    public boolean onUpKeyUp() {
        return false;
    }

    @Override
    public boolean onDownKeyDown() {
        return false;
    }

    @Override
    public boolean onDownKeyUp() {
        return false;
    }

    @Override
    public boolean onLeftKeyDown() {
        return false;
    }

    @Override
    public boolean onLeftKeyUp() {
        return false;
    }

    @Override
    public boolean onRightKeyDown() {
        return false;
    }

    @Override
    public boolean onRightKeyUp() {
        return false;
    }

    @Override
    public boolean onEnterKeyDown() {
        if (currentDialMode == BRACKET_NON)
        {
            prepare();
            currentDialMode = BRACKET_STEP;
            updateBracketStep();
        }
        else if(currentDialMode == BRACKET_STEP)
        {
            activityInterface.showHintMessage("\uE4CD to set picture count, \uE04C to confirm");
            currentDialMode = BRACKET_PICCOUNT;
            reset();
            updateBracketPicCount();
        }
        else
        {
            activityInterface.getDialHandler().setDefaultListner();
            startCountDown();
            currentDialMode = BRACKET_NON;
        }

        return false;
    }

    @Override
    public boolean onEnterKeyUp() {
        return false;
    }
}
