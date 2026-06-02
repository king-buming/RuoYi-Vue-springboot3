package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.mapper.HwAttendanceMapper;

/**
 * 作业打卡超时监控定时任务
 *
 * @author ruoyi
 */
@Component("hwAttendanceTimeoutJob")
public class HwAttendanceTimeoutJob
{
    private static final Logger log = LoggerFactory.getLogger(HwAttendanceTimeoutJob.class);

    @Autowired
    private HwAttendanceMapper hwAttendanceMapper;

    public void execute()
    {
        // 1. 进场超时检测：计划作业时间超过30分钟未进场打卡
        List<HwPlan> uncheckedPlans = hwAttendanceMapper.selectOvertimeUncheckedPlans(30);
        for (HwPlan plan : uncheckedPlans)
        {
            HwAttendance record = new HwAttendance();
            record.setPlanId(plan.getPlanId());
            record.setCheckType("0");
            record.setCheckStatus("2");
            record.setFailReason("进场超时");
            record.setCheckTime(new Date());
            record.setCreateBy("SYSTEM");
            hwAttendanceMapper.insertHwAttendance(record);
            log.info("进场超时记录已生成，计划ID：{}", plan.getPlanId());
        }

        // 2. 离场超时检测：进场超过480分钟（8小时）未离场打卡
        List<HwAttendance> overtimeCheckIns = hwAttendanceMapper.selectOvertimeCheckIns(480);
        for (HwAttendance checkIn : overtimeCheckIns)
        {
            HwAttendance record = new HwAttendance();
            record.setPlanId(checkIn.getPlanId());
            record.setUserId(checkIn.getUserId());
            record.setUserName(checkIn.getUserName());
            record.setCheckType("1");
            record.setCheckStatus("2");
            record.setFailReason("离场超时");
            record.setCheckTime(new Date());
            record.setCreateBy("SYSTEM");
            hwAttendanceMapper.insertHwAttendance(record);
            log.info("离场超时记录已生成，人员：{}", checkIn.getUserName());
        }
    }
}
