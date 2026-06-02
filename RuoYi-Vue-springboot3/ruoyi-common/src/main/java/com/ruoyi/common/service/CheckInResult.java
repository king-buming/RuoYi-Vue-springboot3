package com.ruoyi.common.service;

/**
 * 微信打卡结果
 *
 * @author ruoyi
 */
public class CheckInResult
{
    private boolean success;
    private boolean locationValid;
    private double distance;
    private String message;

    public static CheckInResult success(double distance)
    {
        CheckInResult r = new CheckInResult();
        r.setSuccess(true);
        r.setLocationValid(true);
        r.setDistance(distance);
        r.setMessage("打卡成功");
        return r;
    }

    public static CheckInResult fail(String message)
    {
        CheckInResult r = new CheckInResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isLocationValid() { return locationValid; }
    public void setLocationValid(boolean locationValid) { this.locationValid = locationValid; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
