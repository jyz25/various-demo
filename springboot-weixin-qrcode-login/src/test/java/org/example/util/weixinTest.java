package org.example.util;


import com.wdbyte.weixin.util.HttpUtil;

/**
 * @auther Kindow
 * @date 2024/7/11
 * @project springboot-weixin-qrcode-login
 */
public class weixinTest {

    public static final String APP_ID = "wx6de284282166c40d";


    public static final String APP_SECRET = "a7e6d0ccd7ea5fe23cf0f996cf386ea2";


    public static void main(String[] args) {
        //https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET
        String api = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + APP_ID + "&secret=" + APP_SECRET;
        String result = HttpUtil.get(api);
        System.out.println(result);
    }


}
