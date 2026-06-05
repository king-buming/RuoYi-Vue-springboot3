package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class TbNotification
{
    private Long id;
    private Long workerId;
    private String type;
    private String title;
    private String content;
    private String isRead;
    private String bizType;
    private Long bizId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long v) { this.workerId = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getIsRead() { return isRead; }
    public void setIsRead(String v) { this.isRead = v; }
    public String getBizType() { return bizType; }
    public void setBizType(String v) { this.bizType = v; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long v) { this.bizId = v; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date v) { this.createTime = v; }
}
