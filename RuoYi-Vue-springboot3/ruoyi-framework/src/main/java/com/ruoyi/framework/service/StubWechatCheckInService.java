package com.ruoyi.framework.service;

import org.springframework.stereotype.Component;
import com.ruoyi.common.service.CheckInResult;
import com.ruoyi.common.service.IWechatCheckInService;

/**
 * 微信打卡服务桩实现
 *
 * @author ruoyi
 */
@Component
public class StubWechatCheckInService implements IWechatCheckInService
{
    private static final double EARTH_RADIUS = 6371000.0;

    @Override
    public CheckInResult wechatCheckIn(String openId, Long planId, String checkType, double lat, double lng)
    {
        return CheckInResult.fail("微信公众号打卡服务尚未配置");
    }

    @Override
    public boolean validateLocation(double checkLat, double checkLng, double siteLat, double siteLng, int maxDistanceMeters)
    {
        double dLat = Math.toRadians(siteLat - checkLat);
        double dLng = Math.toRadians(siteLng - checkLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(checkLat)) * Math.cos(Math.toRadians(siteLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        return distance <= maxDistanceMeters;
    }
}
