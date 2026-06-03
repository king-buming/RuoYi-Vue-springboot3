package com.ruoyi.common.service;

/**
 * AI推理结果
 *
 * @author ruoyi
 */
public class AiInferenceResult
{
    private boolean success;
    private String modelCode;
    private String resultJson;
    private long processingTimeMs;
    private String errorMessage;

    public static AiInferenceResult success(String modelCode, String resultJson, long processingTimeMs)
    {
        AiInferenceResult r = new AiInferenceResult();
        r.setSuccess(true);
        r.setModelCode(modelCode);
        r.setResultJson(resultJson);
        r.setProcessingTimeMs(processingTimeMs);
        return r;
    }

    public static AiInferenceResult fail(String modelCode, String errorMessage)
    {
        AiInferenceResult r = new AiInferenceResult();
        r.setSuccess(false);
        r.setModelCode(modelCode);
        r.setErrorMessage(errorMessage);
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString()
    {
        return "AiInferenceResult{success=" + success + ", modelCode='" + modelCode + '\''
            + ", processingTimeMs=" + processingTimeMs + '}';
    }
}
