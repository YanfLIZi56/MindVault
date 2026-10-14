package com.yanglizi.auth.async;

import com.yanglizi.auth.exception.SendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SendAsync {

    @Async
    public void sendPhone(String phone,String code){
        log.info("给{}发送短信,内容: {}",phone, code);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("发送手机短信异常", e);
            throw new SendException("发送手机短信失败");
        }
    }
}
