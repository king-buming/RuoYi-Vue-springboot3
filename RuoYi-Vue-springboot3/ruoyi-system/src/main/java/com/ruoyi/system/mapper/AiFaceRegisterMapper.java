package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AiFaceRegister;

/**
 * AI人脸注册 数据层
 *
 * @author ruoyi
 */
public interface AiFaceRegisterMapper
{
    /**
     * 查询人脸注册列表（LEFT JOIN tb_worker + tb_worker_face）
     */
    public List<AiFaceRegister> selectFaceRegisterList(@Param("workerName") String workerName,
                                                        @Param("registerStatus") String registerStatus);

    /** 按 register_id 查询单条 */
    public AiFaceRegister selectFaceRegisterById(Long registerId);

    /** 按 worker_id + model_code 查询（判断是否已录入） */
    public AiFaceRegister selectByWorkerIdAndModelCode(@Param("workerId") Long workerId,
                                                        @Param("modelCode") String modelCode);

    /** 新增注册记录 */
    public int insertAiFaceRegister(AiFaceRegister entity);

    /** 更新注册记录 */
    public int updateAiFaceRegister(AiFaceRegister entity);

    /** 人脸照片变更后，按人员失效旧AI注册特征 */
    public int invalidateByWorkerId(@Param("workerId") Long workerId,
                                    @Param("faceImgUrl") String faceImgUrl,
                                    @Param("updateBy") String updateBy);

    /** 按 register_id 删除 */
    public int deleteAiFaceRegisterById(Long registerId);

    /** 批量删除 */
    public int deleteAiFaceRegisterByIds(Long[] registerIds);
}
