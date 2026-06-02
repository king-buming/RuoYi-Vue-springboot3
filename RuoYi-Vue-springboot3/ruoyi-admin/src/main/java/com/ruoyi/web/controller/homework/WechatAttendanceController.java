package com.ruoyi.web.controller.homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.service.IHwAttendanceService;

/**
 * 手机端打卡接口
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/wechat/attendance")
public class WechatAttendanceController extends BaseController
{
    @Autowired
    private IHwAttendanceService hwAttendanceService;

    @PostMapping("/checkIn")
    public AjaxResult checkIn(@RequestBody HwAttendance hwAttendance)
    {
        try
        {
            hwAttendanceService.checkIn(hwAttendance);
            return success();
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PostMapping("/checkOut")
    public AjaxResult checkOut(@RequestBody HwAttendance hwAttendance)
    {
        try
        {
            hwAttendanceService.checkOut(hwAttendance);
            return success();
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @GetMapping("/myList")
    public TableDataInfo myList()
    {
        startPage();
        HwAttendance query = new HwAttendance();
        query.setUserId(getUserId());
        return getDataTable(hwAttendanceService.selectHwAttendanceList(query));
    }
}
