package com.easypan.entity.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AlipayConfig {

    @PostConstruct
    public void init() {
        Config config = new Config();
        // 协议固定为 https
        config.protocol = "https";
        // ✅ 沙箱网关（正确地址，不要改）
        config.gatewayHost = "openapi-sandbox.dl.alipaydev.com/gateway.do";
        // 签名类型固定为 RSA2
        config.signType = "RSA2";
        // 1. 沙箱 APPID（保持不变）
        config.appId = "9021000162629598";

        // 2. 商户私钥（已更新为你提供的最新私钥）
        config.merchantPrivateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCnSTTUwT+8ITUim5pEnl2QAkUSRnAzKfjNLPTenuhrrVj56StUQFfTCiL7enIfE1W//oEEDHjcQWBPz5f2C8E8MHjRdE9+UCgfwOu0qvWtBwsLvlsg905gAxOc5HUecaUXAPu0ae1GOszw4Ko6o4440ogXksoTMnINjTYXudyhI+wXqaL6o8UOy1vDRx73qiOZvXbBmBhqPMXjZIqAef4JyKC1qa1biMTckOkc6YcFigepGfbuIcOmNHvAfhm+AKfaKlXnHwY/r0cAUivS3efZLgn0WnkZfR0xysB42XRdo7fKGGw4DKI8c6UF9rVa17hgsJeRGTspOosNu0iHdcw1AgMBAAECggEBAIIupS+AaBIGoawZ/Xay60rbiVcMe9pFKnq5JqJsdaD+GMT7IJ4DKJVySQD9K/xk9pFgBJTbA98b/VB+09pFEY6+hfcyFX7N8DutzYAenuuVFJKCA7bm3hY3rrakz0Pu2rPJLxgaau0Ba2V04OSbTV2I8SGvORSxaDKFlvflyv2Wi+ssdfTgApPWfmrhuoPd0gUxmk1bOlwb+6lBrEEjYskTVA6ROWQhQQe4bjkIRwAh1umfLllFDZbu32dQcHngf+51wLqgYUvTlyxp97nV8pzfT9ZJmuN3wzcX/YEz8/uFHPqFtuWG9atQztYgV5vJUofkIfWjcXDr8Qfaw3voWa0CgYEA5tepUAUuKCsbJ02gHWubM6/9QGzG2sDHoiBKQlqBXPB1Dcmbr285IF1Z8Gd9qLRfcpQaJukwgEjS7y+dHloN8nUGPwZ4wGwUNB2XfhJdt4uNayNzCMomemvLNri4ytLlNHLpUAfoj+/p0Qok+2CPN/6REXPBdxKCHccDqWgSrjcCgYEAuYRdddzhCcIgpAGZv13taEvA7XLARCzyaamggYb8LbAkXqocqLruUtHRiFRfJNBi+KbXNHPQ/QgOnr2ae+nS01tTC8HmQb/swZmGI4klsJAJMXpitPn35jI6UtYKXBLRTbgOnJiOtcC+HtwqUng/hmfXkKBqTsGB3ru1QCuSAvMCgYA6K9CD1Lpb7vjpv5sArvQuY0P7by3xRDFKfr6X1/zPcdzde5ea7vlDWaLnfPzifYmr8vxgH9VUDoXxNltuoZzwI2NOkI/vVY0F0Qsy/ufU7/rUK9u6NE7b/Lv57zTKD7korWhz/Xoyg2+1eMv2+M8+eg0Oi9AN9IELhfI7R3mlowKBgD8ZgXx8U2KxT87Y6IWJmh12Xrmk5TaLHyzHUFueIRodJlG4hKTzZ+ptiVpYcEAiTPeML/icSAI4gliF3wy0nEvdbSK7oejNMecro89kb5hddXMJ0JcDysjWXrYcLUvh7qUDrJyg2zGAzGnMsux4EAtUOKLtnRsKgHTU3bJk33X3AoGAE/GheBJ8HXNO7Q72TunWbsYd1tsdMM2Wnu6hRfrE3trp4ndLN/AbQX1IQ9D1jweBN+Uk0OYjvWdSuB6rKPIEBDtpi+dCTZMXwcuC+uBdhuiOleRcT/5qhCSIuRT6xHQFj57y+/vRqXXhfF1+U6CuCiQ/RxPNzmP+wOh2DZ0Q8BU=";

        // 3. 支付宝公钥（已更新为你提供的公钥）
        config.alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7bH1/1Y9wHcCSu4dDubY7Ao3tzQ9UpkWtDkGQLxgc11UqiZNZgyB9Dht+ip0EjrN6+i8bN9Pq5e1oIFW44eCzs3Zok6QrmjgIFaLrfw2ijiCQbhio/siai2LNvI1T+H6x1SofkhEtqMKnj/rvF/PItJjvBgayVWJL6gkg4akjaTzxqDyZl5tz8O6IpNWxopoxU8W+WvAl3HF0rwrZsCo3BoNSZZHBVj1o761QpmnCJPzbRdXmL5NxhO8On6yKhJa68h+GPa1RjZj++aAU4sddZc1EU87e70IbPAMDhLSwiYiqq1DxKma28didAO60taNDDGvKCAGq1IIyoBtJMGp4wIDAQAB";

        // 4. 回调地址（保持不变）
        config.notifyUrl = "https://lisa-overartificial-janet.ngrok-free.dev/api/aliPay/notify";

        // 初始化支付宝 SDK
        Factory.setOptions(config);

        // 打印日志确认配置加载
        System.out.println("✅ 支付宝 SDK 初始化成功！");
        System.out.println("🔍 当前 AppID: " + config.appId);
        System.out.println("🔍 当前网关： " + config.gatewayHost);
    }
}