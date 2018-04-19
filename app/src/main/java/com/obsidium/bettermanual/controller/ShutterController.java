package com.obsidium.bettermanual.controller;

import com.obsidium.bettermanual.ActivityInterface;
import com.obsidium.bettermanual.MainActivity;
import com.obsidium.bettermanual.capture.CaptureModeBulb;
import com.obsidium.bettermanual.model.ExposureModeModel;
import com.obsidium.bettermanual.model.Model;
import com.obsidium.bettermanual.model.ShutterModel;
import com.sony.scalar.hardware.CameraEx;

public class ShutterController extends TextViewController<ShutterModel> {

    public interface ShutterSpeedEvent
    {
        void onChanged();
    }

    private static ShutterController shutterController = new ShutterController();

    public static ShutterController GetInstance()
    {
        return shutterController;
    }

    private ShutterSpeedEvent shutterSpeedEventListner;
    private ActivityInterface activityInterface;

    public void bindActivityInterface(ActivityInterface activityInterface)
    {
        this.activityInterface = activityInterface;
    }


    @Override
    public void toggle() {
        if (ExposureModeController.GetInstance().getExposureMode() == ExposureModeModel.ExposureModes.aperture && !model.getValue().equals("BULB") && activityInterface != null)
            activityInterface.loadFragment(MainActivity.FRAGMENT_MIN_SHUTTER);
        else if (model.getValue().equals("BULB") && CaptureModeBulb.GetInstance() != null)
            CaptureModeBulb.GetInstance().toggle();

    }

    @Override
    public int getNavigationHelpID() {
        return 0;
    }

    public CameraEx.ShutterSpeedInfo getShutterSpeedInfo()
    {
        if (model != null)
            return model.getShutterSpeedInfo();
        return null;
    }

    public void setShutterSpeedEventListner(ShutterSpeedEvent eventListner)
    {
        this.shutterSpeedEventListner = eventListner;
    }

    @Override
    public void onValueChanged() {
        super.onValueChanged();
        if (shutterSpeedEventListner != null)
            shutterSpeedEventListner.onChanged();
    }
}
