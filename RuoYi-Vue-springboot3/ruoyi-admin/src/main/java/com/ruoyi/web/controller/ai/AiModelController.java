package com.ruoyi.web.controller.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.ruoyi.system.domain.AiModel;
import com.ruoyi.system.service.IAiModelService;

/**
 * AI模型管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/model")
public class AiModelController extends BaseController
{
    @Autowired
    private IAiModelService aiModelService;

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModel model)
    {
        startPage();
        List<AiModel> list = aiModelService.selectAiModelList(model);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping(value = "/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId)
    {
        return success(aiModelService.selectAiModelById(modelId));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping(value = "/code/{modelCode}")
    public AjaxResult getByCode(@PathVariable String modelCode)
    {
        return success(aiModelService.selectAiModelByCode(modelCode));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:add')")
    @Log(title = "AI模型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiModel model)
    {
        try
        {
            model.setCreateBy(getUsername());
            return toAjax(aiModelService.insertAiModel(model));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "AI模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModel model)
    {
        model.setUpdateBy(getUsername());
        return toAjax(aiModelService.updateAiModel(model));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:deploy')")
    @Log(title = "AI模型", businessType = BusinessType.UPDATE)
    @PutMapping("/deploy/{modelId}")
    public AjaxResult deploy(@PathVariable Long modelId)
    {
        try
        {
            return toAjax(aiModelService.deployAiModel(modelId));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('ai:model:remove')")
    @Log(title = "AI模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        return toAjax(aiModelService.deleteAiModelByIds(modelIds));
    }
}
