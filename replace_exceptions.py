import re
import os

# 定义文件路径
files_to_process = [
    r"D:\code\java\java\Fine_the_lost\src\main\java\com\gdut\service\impl\MessageServiceImpl.java",
    r"D:\code\java\java\Fine_the_lost\src\main\java\com\gdut\service\impl\NoteServiceImpl.java"
]

# 错误码映射表
error_code_mapping = {
    "对方不是您的好友": "NOT_FRIENDS",
    "好友ID不能为空": "BAD_REQUEST",
    "您不是该群成员": "NOT_GROUP_MEMBER",
    "群聊ID不能为空": "BAD_REQUEST",
    "您的入群申请正在审核中": "BAD_REQUEST",
    "您已经是该群成员": "BAD_REQUEST",
    "您没有权限审批该申请": "FORBIDDEN",
    "未找到该申请人的记录": "GROUP_NOT_EXIST",
    "该申请已处理过": "BAD_REQUEST",
    "无效的审批操作": "BAD_REQUEST",
    "您没有权限查看该群的待审申请": "FORBIDDEN",
    "群主不能退出群聊，请先转让群主或解散群聊": "FORBIDDEN",
    "群聊不存在": "GROUP_NOT_EXIST",
    "文件上传失败": "INTERNAL_SERVER_ERROR",
    "消息内容不能为空": "BAD_REQUEST",
    "笔记不存在": "NOTE_NOT_EXIST",
    "无权查看该笔记": "NOTE_NO_PERMISSION",
    "PDF笔记仅支持查看，不支持编辑": "BAD_REQUEST",
    "无权修改该笔记": "NOTE_NO_PERMISSION",
    "无权删除该笔记": "NOTE_NO_PERMISSION",
    "笔记已在回收站中": "BAD_REQUEST",
    "无权操作该笔记": "NOTE_NO_PERMISSION",
    "请指定好友用户": "BAD_REQUEST",
    "分享链接无效或已过期": "SHARE_CODE_INVALID",
    "该笔记未公开": "NOTE_NO_PERMISSION",
    "该笔记需要对好友可见，请使用共享笔记接口访问": "NOTE_NO_PERMISSION",
    "该笔记不是共享笔记": "NOTE_NO_PERMISSION",
    "您无权查看该笔记": "NOTE_NO_PERMISSION",
    "仅作者可以查看权限列表": "FORBIDDEN",
    "笔记内容为空，无法进行分析": "BAD_REQUEST",
}

def process_file(file_path):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 添加导入语句（如果还没有）
    if 'import com.gdut.common.exception.BusinessException;' not in content:
        # 在 package 声明后添加导入
        content = re.sub(
            r'(package com\.gdut\.service\.impl;)',
            r'\1\n\nimport com.gdut.common.exception.BusinessException;\nimport com.gdut.common.enums.ResultCode;',
            content
        )
    
    # 替换所有 throw new RuntimeException("xxx")
    def replace_exception(match):
        message = match.group(1)
        error_code = error_code_mapping.get(message, "INTERNAL_SERVER_ERROR")
        return f'throw new BusinessException(ResultCode.{error_code}, "{message}")'
    
    content = re.sub(r'throw new RuntimeException\("([^"]+)"\)', replace_exception, content)
    
    # 写回文件
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Processed: {file_path}")

# 处理所有文件
for file_path in files_to_process:
    process_file(file_path)

print("All files processed successfully!")
