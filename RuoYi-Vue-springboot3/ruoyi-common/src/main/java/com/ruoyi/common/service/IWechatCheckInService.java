package com.ruoyi.common.service;

/**
 * 微信公众号打卡服务接口
 *
 * @author ruoyi
 */
public interface IWechatCheckInService
{
    CheckInResult wechatCheckIn(String openId, Long planId, String checkType, double lat, double lng);
    boolean validateLocation(double checkLat, double checkLng, double siteLat, double siteLng, int maxDistanceMeters);
}
