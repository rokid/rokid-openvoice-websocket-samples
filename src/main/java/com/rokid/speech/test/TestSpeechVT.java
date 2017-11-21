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

    String key = ""; //登录开放平台创建语音设备可以生成
    String deviceTypeId = "";  //登录开放平台创建语音设备可以生成
    String version = "1.0";
    String secret = "";   //登录开放平台创建语音设备可以生成
    String deviceId = "";   //真机设备背面可以查询到
    String url = "wss://apigwws.open.rokid.com/api";

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
