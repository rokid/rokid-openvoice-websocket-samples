package com.rokid.speech.test;

import com.rokid.common.FileTool;
import com.rokid.speech.v1.SpeechTts;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 作者: mashuangwei
 * 日期: 2017/9/22
 * 功能：根据输入的语言内容生成语音文件
 */

public class TestSpeechTts {
    String key = "";  //  登录自己的开放平台账号然后在语音设备里面可以查看到，是用户自己创建的
    String secret = ""; // 登录自己的开放平台账号然后在语音设备里面可以查看到，是用户自己创建的
    String deviceTypeId = "98EA4B548AEB4A329D21615B9ED060E5";  //
    String deviceId = "";  //设备序列号，可以在自己的pebble设备上进行查看，保密需要没有在demo中给出
    String url = "wss://apigwws-dev.open.rokid.com/api";   //正式环境为wss://apigwws.open.rokid.com/api

    @Test
    public void testTts() {
        SpeechTts speechTts = null;
        try {
            speechTts = new SpeechTts(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        speechTts.init(key, deviceTypeId, secret, deviceId);
        speechTts.sendTts("若琪打开车门");
        speechTts.close();
        Assert.assertTrue(speechTts.isFinish(),"getFinish返回的不为true");
        Assert.assertTrue(!speechTts.isHasVoice(),"hasVoice返回的不为false");
        Assert.assertTrue(speechTts.isHasFinish(),"hasFinish返回的不为true");
        Assert.assertTrue(!speechTts.isHasText(),"hasText返回的不为false");
        Assert.assertTrue(speechTts.isHasId(),"hasId返回的不为true");
        Assert.assertTrue(FileTool.getFileSize(speechTts.getFilename()) == 65226,"生成的语音文件大小为0");
        Assert.assertTrue(speechTts.getResult().equalsIgnoreCase("SUCCESS"),"getResult返回的不为SUCCESS");
        FileTool.deleteFile(speechTts.getFilename());

    }

}
