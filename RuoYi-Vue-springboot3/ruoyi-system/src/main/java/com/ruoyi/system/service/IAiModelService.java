package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiModel;

/**
 * AI模型 服务层
 *
 * @author ruoyi
 */
public interface IAiModelService
{
    public List<AiModel> selectAiModelList(AiModel model);

    public AiModel selectAiModelById(Long modelId);

    public AiModel selectAiModelByCode(String modelCode);

    public int insertAiModel(AiModel model);

    public int updateAiModel(AiModel model);

    public int deleteAiModelById(Long modelId);

    public int deleteAiModelByIds(Long[] modelIds);

    public int deployAiModel(Long modelId);
}
