package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.HwAttendance;

/**
 * 打卡记录 服务层
 *
 * @author ruoyi
 */
public interface IHwAttendanceService
{
    public List<HwAttendance> selectHwAttendanceList(HwAttendance attendance);

    public HwAttendance selectHwAttendanceById(Long attendanceId);

    public int insertHwAttendance(HwAttendance attendance);

    public HwAttendance checkIn(HwAttendance attendance);

    public HwAttendance checkOut(HwAttendance attendance);

    public int deleteHwAttendanceById(Long attendanceId);

    public int deleteHwAttendanceByIds(Long[] attendanceIds);
}
