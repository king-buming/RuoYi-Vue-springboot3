package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AiModel;
import com.ruoyi.system.mapper.AiModelMapper;
import com.ruoyi.system.service.IAiModelService;

/**
 * AI模型 服务层处理
 *
 * @author ruoyi
 */
@Service
public class AiModelServiceImpl implements IAiModelService
{
    @Autowired
    private AiModelMapper aiModelMapper;

    @Override
    public List<AiModel> selectAiModelList(AiModel model)
    {
        return aiModelMapper.selectAiModelList(model);
    }

    @Override
    public AiModel selectAiModelById(Long modelId)
    {
        return aiModelMapper.selectAiModelById(modelId);
    }

    @Override
    public AiModel selectAiModelByCode(String modelCode)
    {
        return aiModelMapper.selectAiModelByCode(modelCode);
    }

    @Override
    public int insertAiModel(AiModel model)
    {
        // 校验 modelCode 唯一性
        AiModel existing = aiModelMapper.selectAiModelByCode(model.getModelCode());
        if (existing != null)
        {
            throw new ServiceException("模型编码[" + model.getModelCode() + "]已存在");
        }
        model.setCreateBy(SecurityUtils.getUsername());
        return aiModelMapper.insertAiModel(model);
    }

    @Override
    public int updateAiModel(AiModel model)
    {
        model.setUpdateBy(SecurityUtils.getUsername());
        return aiModelMapper.updateAiModel(model);
    }

    @Override
    public int deleteAiModelById(Long modelId)
    {
        return aiModelMapper.deleteAiModelById(modelId);
    }

    @Override
    public int deleteAiModelByIds(Long[] modelIds)
    {
        return aiModelMapper.deleteAiModelByIds(modelIds);
    }

    @Override
    public int deployAiModel(Long modelId)
    {
        AiModel model = aiModelMapper.selectAiModelById(modelId);
        if (model == null)
        {
            throw new ServiceException("AI模型不存在");
        }
        if (!"0".equals(model.getStatus()))
        {
            throw new ServiceException("只有未部署状态的模型才能执行部署");
        }
        model.setStatus("1");
        model.setUpdateBy(SecurityUtils.getUsername());
        return aiModelMapper.updateAiModel(model);
    }
}
