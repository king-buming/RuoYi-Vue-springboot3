package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.domain.HwPlan;

/**
 * 打卡记录 数据层
 *
 * @author ruoyi
 */
public interface HwAttendanceMapper
{
    public List<HwAttendance> selectHwAttendanceList(HwAttendance attendance);

    public HwAttendance selectHwAttendanceById(Long attendanceId);

    public int insertHwAttendance(HwAttendance attendance);

    public int updateHwAttendance(HwAttendance attendance);

    public int deleteHwAttendanceById(Long attendanceId);

    public int deleteHwAttendanceByIds(Long[] attendanceIds);

    public HwAttendance selectLastCheckIn(@Param("planId") Long planId, @Param("userId") Long userId);

    public HwAttendance selectLastCheckOut(@Param("planId") Long planId, @Param("userId") Long userId);

    public List<HwPlan> selectOvertimeUncheckedPlans(@Param("minutes") int minutes);

    public List<HwAttendance> selectOvertimeCheckIns(@Param("minutes") int minutes);
}
