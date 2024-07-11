package com.wdbyte.weixin.service.impl;

import java.util.Arrays;

import com.wdbyte.weixin.model.ReceiveMessage;
import com.wdbyte.weixin.service.WeixinUserService;
import com.wdbyte.weixin.util.WeixinMsgUtil;
import com.wdbyte.weixin.util.WeixinQrCodeCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author 公众号：程序猿阿朗
 */
@Slf4j
@Service
public class WeixinUserServiceImpl implements WeixinUserService {

    @Value("${weixin.token}")
    private String token;

    @Override
    public void checkSignature(String signature, String timestamp, String nonce) {
        String[] arr = new String[] {token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder content = new StringBuilder();
        for (String str : arr) {
            content.append(str);
        }
        // 计算字符串的Hash值
        String tmpStr = DigestUtils.sha1Hex(content.toString());
        if (tmpStr.equals(signature)) {
            log.info("check success");
            return;
        }
        log.error("check fail");
        throw new RuntimeException("check fail");
    }

    @Override
    public String handleWeixinMsg(String requestBody) {
        ReceiveMessage receiveMessage = WeixinMsgUtil.msgToReceiveMessage(requestBody);
        // 扫码登录
        if (WeixinMsgUtil.isScanQrCode(receiveMessage)) {
            return handleScanLogin(receiveMessage);
        }
        // 关注
        if (WeixinMsgUtil.isEventAndSubscribe(receiveMessage)) {
            return receiveMessage.getReplyTextMsg("欢迎关注【程序猿阿朗】公众号");
        }
        return receiveMessage.getReplyTextMsg("收到（自动回复）");
    }

    /**
     * 处理扫码登录
     *
     * @param receiveMessage
     * @return
     */
    private String handleScanLogin(ReceiveMessage receiveMessage) {
        String qrCodeTicket = WeixinMsgUtil.getQrCodeTicket(receiveMessage);
        if (WeixinQrCodeCacheUtil.get(qrCodeTicket) == null) {
            String openId = receiveMessage.getFromUserName();
            WeixinQrCodeCacheUtil.put(qrCodeTicket, openId);
        }
        return receiveMessage.getReplyTextMsg("你已成功登录！");
    }
}
