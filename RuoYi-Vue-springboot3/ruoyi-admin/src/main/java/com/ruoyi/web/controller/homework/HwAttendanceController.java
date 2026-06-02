package com.ruoyi.web.controller.homework;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.service.IHwAttendanceService;

/**
 * 作业打卡
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/homework/attendance")
public class HwAttendanceController extends BaseController
{
    @Autowired
    private IHwAttendanceService hwAttendanceService;

    @PreAuthorize("@ss.hasPermi('homework:attendance:list')")
    @GetMapping("/list")
    public TableDataInfo list(HwAttendance attendance)
    {
        startPage();
        List<HwAttendance> list = hwAttendanceService.selectHwAttendanceList(attendance);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('homework:attendance:query')")
    @GetMapping(value = "/{attendanceId}")
    public AjaxResult getInfo(@PathVariable Long attendanceId)
    {
        return success(hwAttendanceService.selectHwAttendanceById(attendanceId));
    }

    @PreAuthorize("@ss.hasPermi('homework:attendance:checkIn')")
    @Log(title = "作业打卡", businessType = BusinessType.INSERT)
    @PostMapping("/checkIn")
    public AjaxResult checkIn(@RequestBody HwAttendance attendance)
    {
        try
        {
            hwAttendanceService.checkIn(attendance);
            return success();
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:attendance:checkOut')")
    @Log(title = "作业打卡", businessType = BusinessType.INSERT)
    @PostMapping("/checkOut")
    public AjaxResult checkOut(@RequestBody HwAttendance attendance)
    {
        try
        {
            hwAttendanceService.checkOut(attendance);
            return success();
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:attendance:remove')")
    @Log(title = "作业打卡", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attendanceIds}")
    public AjaxResult remove(@PathVariable Long[] attendanceIds)
    {
        return toAjax(hwAttendanceService.deleteHwAttendanceByIds(attendanceIds));
    }
}
