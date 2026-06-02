package com.ruoyi.web.controller.homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.service.IHwPlanService;

/**
 * 手机端作业计划接口
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/wechat/plan")
public class WechatPlanController extends BaseController
{
    @Autowired
    private IHwPlanService hwPlanService;

    @PostMapping("/create")
    public AjaxResult create(@Validated @RequestBody HwPlan hwPlan)
    {
        hwPlan.setCreateBy(getUsername());
        hwPlan.setStatus("0");
        return toAjax(hwPlanService.insertHwPlan(hwPlan));
    }

    @GetMapping("/myList")
    public TableDataInfo myList()
    {
        startPage();
        return getDataTable(hwPlanService.selectHwPlanList(new HwPlan()));
    }
}
