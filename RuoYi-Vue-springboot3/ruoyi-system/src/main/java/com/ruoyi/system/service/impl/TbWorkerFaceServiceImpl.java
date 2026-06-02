package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.service.ITbWorkerFaceService;

/**
 * 人脸信息Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@Service
public class TbWorkerFaceServiceImpl implements ITbWorkerFaceService
{
    @Autowired
    private TbWorkerFaceMapper tbWorkerFaceMapper;

    /**
     * 查询人脸信息
     *
     * @param id 人脸信息主键
     * @return 人脸信息
     */
    @Override
    public TbWorkerFace selectTbWorkerFaceById(Long id)
    {
        return tbWorkerFaceMapper.selectTbWorkerFaceById(id);
    }

    /**
     * 查询人脸信息列表
     *
     * @param tbWorkerFace 人脸信息
     * @return 人脸信息
     */
    @Override
    public List<TbWorkerFace> selectTbWorkerFaceList(TbWorkerFace tbWorkerFace)
    {
        return tbWorkerFaceMapper.selectTbWorkerFaceList(tbWorkerFace);
    }

    /**
     * 新增人脸信息
     *
     * @param tbWorkerFace 人脸信息
     * @return 结果
     */
    @Override
    public int insertTbWorkerFace(TbWorkerFace tbWorkerFace)
    {
        tbWorkerFace.setCreateTime(DateUtils.getNowDate());
        return tbWorkerFaceMapper.insertTbWorkerFace(tbWorkerFace);
    }

    /**
     * 修改人脸信息
     *
     * @param tbWorkerFace 人脸信息
     * @return 结果
     */
    @Override
    public int updateTbWorkerFace(TbWorkerFace tbWorkerFace)
    {
        tbWorkerFace.setUpdateTime(DateUtils.getNowDate());
        return tbWorkerFaceMapper.updateTbWorkerFace(tbWorkerFace);
    }

    /**
     * 批量删除人脸信息
     *
     * @param ids 需要删除的人脸信息主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerFaceByIds(Long[] ids)
    {
        return tbWorkerFaceMapper.deleteTbWorkerFaceByIds(ids);
    }

    /**
     * 删除人脸信息信息
     *
     * @param id 人脸信息主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerFaceById(Long id)
    {
        return tbWorkerFaceMapper.deleteTbWorkerFaceById(id);
    }
}
