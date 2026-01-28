package uss.code.auth.infra;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailTemplateGenerator {

    private static final String VERIFICATION_CODE_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: white;
                        border-radius: 10px;
                        padding: 40px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #333;
                        margin: 0;
                    }
                    .content {
                        color: #555;
                        line-height: 1.6;
                    }
                    .code-box {
                        background-color: #f8f9fa;
                        border: 2px solid #007bff;
                        border-radius: 8px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #007bff;
                        letter-spacing: 4px;
                    }
                    .footer {
                        margin-top: 30px;
                        text-align: center;
                        color: #888;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎓 Ultimate Sugang Simulator</h1>
                    </div>
                    <div class="content">
                        <p>안녕하세요,</p>
                        <p>회원가입을 위한 이메일 인증 코드입니다.</p>
                        <p>아래의 인증 코드를 입력하여 인증을 완료해주세요.</p>

                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>

                        <p>본 인증 코드는 본인이 요청한 경우에만 사용해주세요.</p>
                        <p>요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Ultimate Sugang Simulator. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """;

    public static String generateVerificationCodeTemplate(final String code) {
        return VERIFICATION_CODE_TEMPLATE.formatted(code);
    }
}
