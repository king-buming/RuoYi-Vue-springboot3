package com.ruoyi.common.service;

/**
 * 人脸匹配结果
 *
 * @author ruoyi
 */
public class FaceMatchResult
{
    private boolean matched;
    private double confidence;
    private String personName;
    private String errorMessage;
    private Long workerId;

    public static FaceMatchResult success(double confidence)
    {
        FaceMatchResult result = new FaceMatchResult();
        result.setMatched(true);
        result.setConfidence(confidence);
        return result;
    }

    public static FaceMatchResult success(double confidence, Long workerId)
    {
        FaceMatchResult result = new FaceMatchResult();
        result.setMatched(true);
        result.setConfidence(confidence);
        result.setWorkerId(workerId);
        return result;
    }

    public static FaceMatchResult fail(String errorMessage)
    {
        FaceMatchResult result = new FaceMatchResult();
        result.setMatched(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public boolean isMatched()
    {
        return matched;
    }

    public void setMatched(boolean matched)
    {
        this.matched = matched;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public void setConfidence(double confidence)
    {
        this.confidence = confidence;
    }

    public String getPersonName()
    {
        return personName;
    }

    public void setPersonName(String personName)
    {
        this.personName = personName;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Long getWorkerId()
    {
        return workerId;
    }

    public void setWorkerId(Long workerId)
    {
        this.workerId = workerId;
    }
}
