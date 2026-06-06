package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.service.FaceMatchResult;
import com.ruoyi.common.service.IFaceRecognitionService;
import com.ruoyi.common.service.IWechatCheckInService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.mapper.HwAttendanceMapper;
import com.ruoyi.system.mapper.HwPlanMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.service.IHwAttendanceService;

/**
 * 打卡记录 服务层处理
 *
 * @author ruoyi
 */
@Service
public class HwAttendanceServiceImpl implements IHwAttendanceService
{
    @Autowired
    private HwAttendanceMapper hwAttendanceMapper;

    @Autowired
    private HwPlanMapper hwPlanMapper;

    @Autowired
    private TbWorkerMapper tbWorkerMapper;

    @Autowired
    private TbWorkerFaceMapper tbWorkerFaceMapper;

    @Autowired(required = false)
    private IFaceRecognitionService faceRecognitionService;

    @Autowired(required = false)
    private IWechatCheckInService wechatCheckInService;

    @Override
    public List<HwAttendance> selectHwAttendanceList(HwAttendance attendance)
    {
        return hwAttendanceMapper.selectHwAttendanceList(attendance);
    }

    @Override
    public HwAttendance selectHwAttendanceById(Long attendanceId)
    {
        return hwAttendanceMapper.selectHwAttendanceById(attendanceId);
    }

    @Override
    public int insertHwAttendance(HwAttendance attendance)
    {
        return hwAttendanceMapper.insertHwAttendance(attendance);
    }

    /**
     * 执行打卡校验（人脸识别 + GPS定位）
     */
    private void doCheckInValidation(HwAttendance attendance, HwPlan plan)
    {
        // AI人脸比对校验
        if ("0".equals(attendance.getCheckMethod()) && faceRecognitionService != null)
        {
            if (StringUtils.isEmpty(attendance.getFaceImage()))
            {
                attendance.setCheckStatus("1");
                attendance.setFailReason("人脸打卡需要上传抓拍图片");
                return;
            }
            TbWorker worker = tbWorkerMapper.selectTbWorkerById(attendance.getUserId());
            if (worker == null)
            {
                attendance.setCheckStatus("1");
                attendance.setFailReason("未找到该人员信息");
                return;
            }
            TbWorkerFace queryFace = new TbWorkerFace();
            queryFace.setWorkerId(attendance.getUserId());
            List<TbWorkerFace> faces = tbWorkerFaceMapper.selectTbWorkerFaceList(queryFace);
            if (faces.isEmpty() || StringUtils.isEmpty(faces.get(0).getFaceImgUrl()))
            {
                attendance.setCheckStatus("1");
                attendance.setFailReason("未找到该人员的人脸底图，请先在人员管理中上传");
                return;
            }
            FaceMatchResult result = faceRecognitionService.compareFace(
                attendance.getFaceImage(), faces.get(0).getFaceImgUrl());
            if (!result.isMatched())
            {
                attendance.setCheckStatus("1");
                attendance.setFailReason("人脸识别不匹配：" + result.getErrorMessage());
                return;
            }
        }

        // GPS位置校验
        if ("1".equals(attendance.getCheckMethod()) && wechatCheckInService != null)
        {
            if (StringUtils.isEmpty(attendance.getLocation()))
            {
                attendance.setCheckStatus("2");
                attendance.setFailReason("公众号打卡需要GPS位置信息");
                return;
            }
            try
            {
                String[] coords = attendance.getLocation().split(",");
                double checkLng = Double.parseDouble(coords[0].trim());
                double checkLat = Double.parseDouble(coords[1].trim());
                if (plan.getSiteLatitude() != null && plan.getSiteLongitude() != null)
                {
                    boolean inRange = wechatCheckInService.validateLocation(
                        checkLat, checkLng, plan.getSiteLatitude().doubleValue(),
                        plan.getSiteLongitude().doubleValue(), 500);
                    if (!inRange)
                    {
                        attendance.setCheckStatus("2");
                        attendance.setFailReason("GPS位置超出施工点500米范围");
                    }
                }
            }
            catch (Exception e)
            {
                attendance.setCheckStatus("2");
                attendance.setFailReason("GPS坐标格式解析失败");
            }
        }
    }

    @Override
    public HwAttendance checkIn(HwAttendance attendance)
    {
        // 1. 校验作业计划存在且状态为待执行(1)或进行中(2)
        HwPlan plan = hwPlanMapper.selectHwPlanById(attendance.getPlanId());
        if (plan == null)
        {
            throw new ServiceException("作业计划不存在");
        }
        if (!"1".equals(plan.getStatus()) && !"2".equals(plan.getStatus()))
        {
            if ("4".equals(plan.getStatus()))
                throw new ServiceException("该计划已取消，无法打卡");
            else if ("3".equals(plan.getStatus()))
                throw new ServiceException("该计划已完成，无法打卡");
            else
                throw new ServiceException("作业计划状态不允许打卡");
        }
        // 2. 校验是否已进场未离场
        HwAttendance lastCheckIn = hwAttendanceMapper.selectLastCheckIn(attendance.getPlanId(), attendance.getUserId());
        if (lastCheckIn != null)
        {
            throw new ServiceException("该人员已进场打卡，请先完成离场打卡");
        }
        // 3. 设置打卡类型为进场
        attendance.setCheckType("0");
        attendance.setCheckTime(new Date());
        attendance.setCheckStatus("0");
        attendance.setCreateBy(SecurityUtils.getUsername());
        // 4. 打卡校验（人脸/GPS）
        doCheckInValidation(attendance, plan);
        // 5. 写入记录（无论校验结果都写入，失败原因记录在checkStatus和failReason中）
        hwAttendanceMapper.insertHwAttendance(attendance);
        // 6. 首次进场打卡时，将计划状态从待执行(1)自动切换为进行中(2)
        if ("0".equals(attendance.getCheckStatus()) && "1".equals(plan.getStatus()))
        {
            plan.setStatus("2");
            plan.setUpdateBy("SYSTEM");
            hwPlanMapper.updateHwPlan(plan);
        }
        return attendance;
    }

    @Override
    public HwAttendance checkOut(HwAttendance attendance)
    {
        // 1. 校验作业计划存在且状态为待执行(1)或进行中(2)
        HwPlan plan = hwPlanMapper.selectHwPlanById(attendance.getPlanId());
        if (plan == null)
        {
            throw new ServiceException("作业计划不存在");
        }
        if (!"1".equals(plan.getStatus()) && !"2".equals(plan.getStatus()))
        {
            if ("4".equals(plan.getStatus()))
                throw new ServiceException("该计划已取消，无法打卡");
            else if ("3".equals(plan.getStatus()))
                throw new ServiceException("该计划已完成，无法打卡");
            else
                throw new ServiceException("作业计划状态不允许打卡");
        }
        // 2. 校验有进场记录
        HwAttendance lastCheckIn = hwAttendanceMapper.selectLastCheckIn(attendance.getPlanId(), attendance.getUserId());
        if (lastCheckIn == null)
        {
            throw new ServiceException("未找到进场打卡记录，请先进场打卡");
        }
        // 2.5 校验未重复离场
        HwAttendance lastCheckOut = hwAttendanceMapper.selectLastCheckOut(attendance.getPlanId(), attendance.getUserId());
        if (lastCheckOut != null && !lastCheckOut.getCheckTime().before(lastCheckIn.getCheckTime()))
        {
            throw new ServiceException("该人员已离场打卡，请勿重复操作");
        }
        // 3. 设置离场时间（取当前时间），MySQL DATETIME 仅存秒级精度
        if (attendance.getCheckTime() == null)
        {
            attendance.setCheckTime(new Date());
        }
        long outSec = attendance.getCheckTime().getTime() / 1000;
        long inSec = lastCheckIn.getCheckTime().getTime() / 1000;
        if (outSec < inSec)
        {
            throw new ServiceException("离场时间不能早于进场时间");
        }
        // 4. 设置打卡类型为离场
        attendance.setCheckType("1");
        attendance.setCheckStatus("0");
        attendance.setCreateBy(SecurityUtils.getUsername());
        // 5. 打卡校验（人脸/GPS）
        doCheckInValidation(attendance, plan);
        // 6. 写入记录
        hwAttendanceMapper.insertHwAttendance(attendance);
        return attendance;
    }

    @Override
    public int deleteHwAttendanceById(Long attendanceId)
    {
        return hwAttendanceMapper.deleteHwAttendanceById(attendanceId);
    }

    @Override
    public int deleteHwAttendanceByIds(Long[] attendanceIds)
    {
        return hwAttendanceMapper.deleteHwAttendanceByIds(attendanceIds);
    }
}
