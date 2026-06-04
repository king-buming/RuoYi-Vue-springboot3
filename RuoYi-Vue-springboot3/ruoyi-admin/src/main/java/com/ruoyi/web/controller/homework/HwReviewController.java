package com.ruoyi.web.controller.homework;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.HwReview;
import com.ruoyi.system.service.IHwReviewService;

/**
 * 作业审核
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/homework/review")
public class HwReviewController extends BaseController
{
    @Autowired
    private IHwReviewService hwReviewService;

    @PreAuthorize("@ss.hasPermi('homework:review:list')")
    @GetMapping("/list")
    public TableDataInfo list(HwReview hwReview)
    {
        startPage();
        List<HwReview> list = hwReviewService.selectHwReviewList(hwReview);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('homework:review:query')")
    @GetMapping(value = "/{reviewId}")
    public AjaxResult getInfo(@PathVariable Long reviewId)
    {
        return success(hwReviewService.selectHwReviewById(reviewId));
    }

    @PreAuthorize("@ss.hasPermi('homework:review:approve')")
    @Log(title = "作业审核", businessType = BusinessType.UPDATE)
    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody HwReview hwReview)
    {
        try
        {
            return toAjax(hwReviewService.approve(hwReview.getReviewId(), hwReview.getReviewOpinion()));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:review:reject')")
    @Log(title = "作业审核", businessType = BusinessType.UPDATE)
    @PutMapping("/reject")
    public AjaxResult reject(@RequestBody HwReview hwReview)
    {
        try
        {
            return toAjax(hwReviewService.reject(hwReview.getReviewId(), hwReview.getReviewOpinion()));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:review:reject')")
    @Log(title = "作业审核", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reviewIds}")
    public AjaxResult remove(@PathVariable Long[] reviewIds)
    {
        return toAjax(hwReviewService.deleteHwReviewByIds(reviewIds));
    }
}
