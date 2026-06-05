-- 消息通知表
USE `ry-vue`;
DROP TABLE IF EXISTS tb_notification;
CREATE TABLE tb_notification (
  id bigint NOT NULL AUTO_INCREMENT, worker_id bigint NOT NULL,
  type varchar(30) NOT NULL, title varchar(200) NOT NULL, content varchar(500) DEFAULT '',
  is_read char(1) DEFAULT '0', biz_type varchar(30) DEFAULT '', biz_id bigint DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id), KEY idx_worker (worker_id), KEY idx_unread (worker_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';
