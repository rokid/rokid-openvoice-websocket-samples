package com.rokid.speech.test;

import com.rokid.speech.v1.SpeechVt;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 作者: mashuangwei
 * 日期: 2017/8/22
 */

public class TestSpeechVT {

    String key = "9C4D6BEB6448468FB73E75A2C33E6ADE";
    String deviceTypeId = "98EA4B548AEB4A329D21615B9ED060E5";
    String version = "1.0";
    String secret = "32F7A304CE8740C5BD61F587F7DD7B88";
    String deviceId = "0202021716000025";
    String url = "wss://apigwws-dev.open.rokid.com/api";

    @Test
    public void testSpeecht() {
        SpeechVt webSock = null;
        try {
            webSock = new SpeechVt(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        webSock.init(key, deviceTypeId, version, secret, deviceId);
        webSock.sendTts("若琪打开车门");
        webSock.close();

    }

    @Test
    public void testSpeechv() {
        SpeechVt webSock = null;
        try {
            webSock = new SpeechVt(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSock.init(key, deviceTypeId, version, secret,deviceId);
        String path = System.getProperty("user.dir") + "/src/main/resources/files/car.pcm";
        webSock.sendDataByTime("pcm", "zh", path,"若琪","100",0,1);
        webSock.close();
    }

}
