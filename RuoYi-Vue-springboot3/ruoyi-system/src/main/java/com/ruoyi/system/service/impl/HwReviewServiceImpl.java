package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.domain.HwReview;
import com.ruoyi.system.mapper.HwPlanMapper;
import com.ruoyi.system.mapper.HwReviewMapper;
import com.ruoyi.system.service.IHwReviewService;

/**
 * 作业审核 服务层处理
 *
 * @author ruoyi
 */
@Service
public class HwReviewServiceImpl implements IHwReviewService
{
    @Autowired
    private HwReviewMapper hwReviewMapper;

    @Autowired
    private HwPlanMapper hwPlanMapper;

    @Override
    public List<HwReview> selectHwReviewList(HwReview hwReview)
    {
        return hwReviewMapper.selectHwReviewList(hwReview);
    }

    @Override
    public HwReview selectHwReviewById(Long reviewId)
    {
        return hwReviewMapper.selectHwReviewById(reviewId);
    }

    @Override
    public HwReview selectHwReviewByPlanId(Long planId)
    {
        return hwReviewMapper.selectHwReviewByPlanId(planId);
    }

    @Override
    public int insertHwReview(HwReview hwReview)
    {
        hwReview.setCreateBy(SecurityUtils.getUsername());
        return hwReviewMapper.insertHwReview(hwReview);
    }

    @Override
    public int updateHwReview(HwReview hwReview)
    {
        hwReview.setUpdateBy(SecurityUtils.getUsername());
        return hwReviewMapper.updateHwReview(hwReview);
    }

    @Override
    public int deleteHwReviewById(Long reviewId)
    {
        return hwReviewMapper.deleteHwReviewById(reviewId);
    }

    @Override
    public int deleteHwReviewByIds(Long[] reviewIds)
    {
        return hwReviewMapper.deleteHwReviewByIds(reviewIds);
    }

    @Override
    public int approve(Long reviewId, String opinion)
    {
        HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
        if (review == null)
        {
            throw new ServiceException("审核记录不存在");
        }
        if (!"0".equals(review.getReviewStatus()))
        {
            throw new ServiceException("该记录已审核，请勿重复操作");
        }
        // 更新审核记录
        review.setReviewStatus("1");
        review.setReviewOpinion(opinion);
        review.setReviewer(SecurityUtils.getUsername());
        review.setReviewTime(new Date());
        review.setUpdateBy(SecurityUtils.getUsername());
        hwReviewMapper.updateHwReview(review);
        // 更新作业计划状态：待审核(0) → 待执行(1)
        HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
        if (plan == null)
        {
            throw new ServiceException("关联的作业计划不存在");
        }
        plan.setStatus("1");
        plan.setUpdateBy(SecurityUtils.getUsername());
        return hwPlanMapper.updateHwPlan(plan);
    }

    @Override
    public int reject(Long reviewId, String opinion)
    {
        HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
        if (review == null)
        {
            throw new ServiceException("审核记录不存在");
        }
        if (!"0".equals(review.getReviewStatus()))
        {
            throw new ServiceException("该记录已审核，请勿重复操作");
        }
        // 更新审核记录
        review.setReviewStatus("2");
        review.setReviewOpinion(opinion);
        review.setReviewer(SecurityUtils.getUsername());
        review.setReviewTime(new Date());
        review.setUpdateBy(SecurityUtils.getUsername());
        hwReviewMapper.updateHwReview(review);
        // 更新作业计划状态：待审核(0) → 已取消(4)
        HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
        if (plan == null)
        {
            throw new ServiceException("关联的作业计划不存在");
        }
        plan.setStatus("4");
        plan.setUpdateBy(SecurityUtils.getUsername());
        return hwPlanMapper.updateHwPlan(plan);
    }
}
