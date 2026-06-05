package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.service.AiInferenceResult;
import com.ruoyi.common.service.IAiInferenceService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AiFaceRegister;
import com.ruoyi.system.domain.AiModel;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.mapper.AiFaceRegisterMapper;
import com.ruoyi.system.mapper.AiModelMapper;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.service.IAiFaceRegisterService;

/**
 * AI人脸注册 服务层处理
 *
 * @author ruoyi
 */
@Service
public class AiFaceRegisterServiceImpl implements IAiFaceRegisterService
{
    @Autowired
    private AiFaceRegisterMapper faceRegisterMapper;

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private TbWorkerMapper workerMapper;

    @Autowired
    private TbWorkerFaceMapper workerFaceMapper;

    @Autowired
    private IAiInferenceService aiInferenceService;

    @Override
    public List<AiFaceRegister> selectFaceRegisterList(String workerName, String registerStatus)
    {
        return faceRegisterMapper.selectFaceRegisterList(workerName, registerStatus);
    }

    @Override
    public AiFaceRegister selectFaceRegisterById(Long registerId)
    {
        return faceRegisterMapper.selectFaceRegisterById(registerId);
    }

    @Override
    public int registerFace(Long workerId, String modelCode)
    {
        // 1. 校验AI模型存在且为人脸匹配类型
        AiModel model = aiModelMapper.selectAiModelByCode(modelCode);
        if (model == null)
        {
            throw new ServiceException("AI模型[" + modelCode + "]不存在");
        }
        if (!"face_recognition".equals(model.getModelType()))
        {
            throw new ServiceException("模型[" + modelCode + "]不是人脸匹配类型，无法用于人脸注册");
        }

        // 2. 检查是否已录入
        AiFaceRegister existing = faceRegisterMapper.selectByWorkerIdAndModelCode(workerId, modelCode);
        if (existing != null && "1".equals(existing.getRegisterStatus()))
        {
            throw new ServiceException("该人员已录入到模型[" + model.getModelName() + "]，无需重复操作");
        }

        // 3. 校验人员存在
        TbWorker worker = workerMapper.selectTbWorkerById(workerId);
        if (worker == null)
        {
            throw new ServiceException("人员不存在");
        }

        // 4. 查询该人员的人脸照片（取最新一条）
        TbWorkerFace query = new TbWorkerFace();
        query.setWorkerId(workerId);
        List<TbWorkerFace> faces = workerFaceMapper.selectTbWorkerFaceList(query);
        String faceImgUrl = null;
        if (!faces.isEmpty())
        {
            faceImgUrl = faces.get(0).getFaceImgUrl();
        }
        if (faceImgUrl == null || faceImgUrl.isEmpty())
        {
            throw new ServiceException("该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集");
        }

        // 5. 调用AI推理服务提取特征向量
        AiInferenceResult result = aiInferenceService.extractFaceFeature(modelCode, faceImgUrl);
        if (!result.isSuccess())
        {
            // 特征提取失败
            AiFaceRegister failRecord = (existing != null) ? existing : new AiFaceRegister();
            failRecord.setWorkerId(workerId);
            failRecord.setWorkerName(worker.getWorkerName());
            failRecord.setFaceImgUrl(faceImgUrl);
            failRecord.setModelCode(modelCode);
            failRecord.setModelName(model.getModelName());
            failRecord.setRegisterStatus("2");
            failRecord.setFailReason(result.getErrorMessage());
            failRecord.setUpdateBy(SecurityUtils.getUsername());

            if (existing != null)
            {
                faceRegisterMapper.updateAiFaceRegister(failRecord);
            }
            else
            {
                failRecord.setCreateBy(SecurityUtils.getUsername());
                faceRegisterMapper.insertAiFaceRegister(failRecord);
            }
            throw new ServiceException(result.getErrorMessage());
        }

        // 6. 写入/更新注册记录
        AiFaceRegister record = (existing != null) ? existing : new AiFaceRegister();
        record.setWorkerId(workerId);
        record.setWorkerName(worker.getWorkerName());
        record.setFaceImgUrl(faceImgUrl);
        record.setFaceFeature(result.getResultJson());
        record.setModelCode(modelCode);
        record.setModelName(model.getModelName());
        record.setRegisterStatus("1");
        record.setRegisterTime(new Date());
        record.setFailReason("");

        int rows;
        if (existing != null)
        {
            record.setUpdateBy(SecurityUtils.getUsername());
            rows = faceRegisterMapper.updateAiFaceRegister(record);
        }
        else
        {
            record.setCreateBy(SecurityUtils.getUsername());
            rows = faceRegisterMapper.insertAiFaceRegister(record);
        }

        // 7. 同步更新 tb_worker.face_status
        if ("0".equals(worker.getFaceStatus()))
        {
            worker.setFaceStatus("1");
            worker.setUpdateBy(SecurityUtils.getUsername());
            workerMapper.updateTbWorker(worker);
        }

        return rows;
    }

    @Override
    public int cancelRegister(Long registerId)
    {
        AiFaceRegister record = faceRegisterMapper.selectFaceRegisterById(registerId);
        if (record == null)
        {
            throw new ServiceException("注册记录不存在");
        }
        if (!"1".equals(record.getRegisterStatus()))
        {
            throw new ServiceException("该记录当前状态不是已录入，无需取消");
        }
        record.setRegisterStatus("0");
        record.setFaceFeature(null);
        record.setRegisterTime(null);
        record.setFailReason("操作员手动取消注册");
        record.setUpdateBy(SecurityUtils.getUsername());
        return faceRegisterMapper.updateAiFaceRegister(record);
    }

    @Override
    public int batchRegister(Long[] workerIds, String modelCode)
    {
        int successCount = 0;
        StringBuilder errors = new StringBuilder();
        for (Long workerId : workerIds)
        {
            try
            {
                registerFace(workerId, modelCode);
                successCount++;
            }
            catch (ServiceException e)
            {
                if (errors.length() > 0) errors.append("; ");
                TbWorker w = workerMapper.selectTbWorkerById(workerId);
                String name = w != null ? w.getWorkerName() : String.valueOf(workerId);
                errors.append(name).append(": ").append(e.getMessage());
            }
        }
        if (successCount == 0 && errors.length() > 0)
        {
            throw new ServiceException("批量录入全部失败: " + errors.toString());
        }
        if (errors.length() > 0)
        {
            // 有部分失败，仍返回成功数，失败信息通过Controller的msg返回
        }
        return successCount;
    }

    @Override
    public int deleteAiFaceRegisterByIds(Long[] registerIds)
    {
        return faceRegisterMapper.deleteAiFaceRegisterByIds(registerIds);
    }
}
