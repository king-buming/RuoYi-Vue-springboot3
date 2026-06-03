package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiFaceRegister;

/**
 * AI人脸注册 服务层
 *
 * @author ruoyi
 */
public interface IAiFaceRegisterService
{
    /** 查询人脸注册列表（含tb_worker联表数据） */
    public List<AiFaceRegister> selectFaceRegisterList(String workerName, String registerStatus);

    /** 按ID查询 */
    public AiFaceRegister selectFaceRegisterById(Long registerId);

    /**
     * 执行人脸录入
     * @param workerId 人员ID
     * @param modelCode AI模型编码
     * @return 受影响行数
     */
    public int registerFace(Long workerId, String modelCode);

    /** 取消注册 */
    public int cancelRegister(Long registerId);

    /** 批量录入 */
    public int batchRegister(Long[] workerIds, String modelCode);

    /** 删除注册记录 */
    public int deleteAiFaceRegisterByIds(Long[] registerIds);
}
