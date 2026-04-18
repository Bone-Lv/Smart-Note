-- 为笔记表添加类型字段，用于区分Markdown和PDF笔记
ALTER TABLE note ADD COLUMN note_type VARCHAR(20) DEFAULT 'MARKDOWN' COMMENT '笔记类型：MARKDOWN/PDF';
