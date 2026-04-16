package com.gdut.constant;

/**
 * 邮件相关常量
 */
public class EmailConstants {

    /**
     * 验证码邮件主题
     */
    public static final String VERIFICATION_CODE_SUBJECT = "【Fine The Lost】验证码登录";

    /**
     * 验证码邮件HTML模板 - 使用占位符 {CODE} 替换为实际验证码
     */
    public static final String VERIFICATION_CODE_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #f5f5f5;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        padding: 30px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        color: #333;
                        border-bottom: 2px solid #4CAF50;
                        padding-bottom: 15px;
                        margin-bottom: 20px;
                    }
                    .content {
                        color: #666;
                        line-height: 1.6;
                    }
                    .code-box {
                        background-color: #f0f0f0;
                        border-left: 4px solid #4CAF50;
                        padding: 15px;
                        margin: 20px 0;
                        text-align: center;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #4CAF50;
                        letter-spacing: 5px;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 15px;
                        border-top: 1px solid #eee;
                        color: #999;
                        font-size: 12px;
                        text-align: center;
                    }
                    .warning {
                        color: #ff6b6b;
                        font-size: 14px;
                        margin-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Fine The Lost - 验证码登录</h2>
                    </div>
                    <div class="content">
                        <p>您好！</p>
                        <p>您正在使用验证码登录 Fine The Lost 系统，您的验证码是：</p>
                        <div class="code-box">
                            <div class="code">{CODE}</div>
                        </div>
                        <p>验证码有效期为 <strong>5分钟</strong>，请尽快使用。</p>
                        <p class="warning">⚠️ 重要提示：如果您没有请求此验证码，请忽略此邮件，您的账户是安全的。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复</p>
                        <p>&copy; 2026 Fine The Lost. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """;

    /**
     * 验证码占位符
     */
    public static final String CODE_PLACEHOLDER = "{CODE}";
}
