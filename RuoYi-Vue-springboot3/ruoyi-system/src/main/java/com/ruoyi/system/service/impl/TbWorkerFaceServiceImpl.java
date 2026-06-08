package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.mapper.AiFaceRegisterMapper;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
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
    @Autowired
    private TbWorkerMapper tbWorkerMapper;
    @Autowired
    private AiFaceRegisterMapper aiFaceRegisterMapper;

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
        if (tbWorkerFace.getWorkerId() == null) {
            tbWorkerFace.setCreateTime(DateUtils.getNowDate());
            return tbWorkerFaceMapper.insertTbWorkerFace(tbWorkerFace);
        }

        TbWorkerFace query = new TbWorkerFace();
        query.setWorkerId(tbWorkerFace.getWorkerId());
        List<TbWorkerFace> existingFaces = tbWorkerFaceMapper.selectTbWorkerFaceList(query);
        existingFaces.sort((a, b) -> {
            Long aid = a.getId() == null ? 0L : a.getId();
            Long bid = b.getId() == null ? 0L : b.getId();
            return bid.compareTo(aid);
        });

        if (existingFaces.isEmpty()) {
            tbWorkerFace.setCreateTime(DateUtils.getNowDate());
            int rows = tbWorkerFaceMapper.insertTbWorkerFace(tbWorkerFace);
            markWorkerFaceChanged(tbWorkerFace.getWorkerId(), tbWorkerFace.getFaceImgUrl(), tbWorkerFace.getUpdateBy());
            return rows;
        }

        TbWorkerFace savedFace = existingFaces.get(0);
        savedFace.setFaceImgUrl(tbWorkerFace.getFaceImgUrl());
        savedFace.setFaceFeature("");
        savedFace.setCollectTime(tbWorkerFace.getCollectTime());
        savedFace.setRemark(tbWorkerFace.getRemark());
        savedFace.setUpdateBy(tbWorkerFace.getUpdateBy());
        savedFace.setUpdateTime(DateUtils.getNowDate());
        int rows = tbWorkerFaceMapper.updateTbWorkerFace(savedFace);
        tbWorkerFace.setId(savedFace.getId());

        for (int i = 1; i < existingFaces.size(); i++) {
            tbWorkerFaceMapper.deleteTbWorkerFaceById(existingFaces.get(i).getId());
        }
        markWorkerFaceChanged(savedFace.getWorkerId(), savedFace.getFaceImgUrl(), tbWorkerFace.getUpdateBy());
        return rows;
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
        Long workerId = tbWorkerFace.getWorkerId();
        if (workerId == null && tbWorkerFace.getId() != null) {
            TbWorkerFace old = tbWorkerFaceMapper.selectTbWorkerFaceById(tbWorkerFace.getId());
            if (old != null) {
                workerId = old.getWorkerId();
            }
        }
        tbWorkerFace.setUpdateTime(DateUtils.getNowDate());
        int rows = tbWorkerFaceMapper.updateTbWorkerFace(tbWorkerFace);
        if (workerId != null && tbWorkerFace.getFaceImgUrl() != null && !tbWorkerFace.getFaceImgUrl().trim().isEmpty()) {
            markWorkerFaceChanged(workerId, tbWorkerFace.getFaceImgUrl(), tbWorkerFace.getUpdateBy());
        }
        return rows;
    }

    private void markWorkerFaceChanged(Long workerId, String faceImgUrl, String updateBy)
    {
        if (workerId == null || faceImgUrl == null || faceImgUrl.trim().isEmpty()) {
            return;
        }
        TbWorker worker = tbWorkerMapper.selectTbWorkerById(workerId);
        if (worker != null) {
            worker.setFaceStatus("1");
            worker.setAuditStatus("0");
            tbWorkerMapper.updateTbWorker(worker);
        }
        aiFaceRegisterMapper.invalidateByWorkerId(workerId, faceImgUrl, updateBy);
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
