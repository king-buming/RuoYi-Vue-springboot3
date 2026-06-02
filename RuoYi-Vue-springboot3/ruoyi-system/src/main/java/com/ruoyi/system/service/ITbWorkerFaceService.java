package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerFace;

/**
 * 人脸信息Service接口
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public interface ITbWorkerFaceService
{
    /**
     * 查询人脸信息
     *
     * @param id 人脸信息主键
     * @return 人脸信息
     */
    public TbWorkerFace selectTbWorkerFaceById(Long id);

    /**
     * 查询人脸信息列表
     *
     * @param tbWorkerFace 人脸信息
     * @return 人脸信息集合
     */
    public List<TbWorkerFace> selectTbWorkerFaceList(TbWorkerFace tbWorkerFace);

    /**
     * 新增人脸信息
     *
     * @param tbWorkerFace 人脸信息
     * @return 结果
     */
    public int insertTbWorkerFace(TbWorkerFace tbWorkerFace);

    /**
     * 修改人脸信息
     *
     * @param tbWorkerFace 人脸信息
     * @return 结果
     */
    public int updateTbWorkerFace(TbWorkerFace tbWorkerFace);

    /**
     * 批量删除人脸信息
     *
     * @param ids 需要删除的人脸信息主键集合
     * @return 结果
     */
    public int deleteTbWorkerFaceByIds(Long[] ids);

    /**
     * 删除人脸信息信息
     *
     * @param id 人脸信息主键
     * @return 结果
     */
    public int deleteTbWorkerFaceById(Long id);
}
