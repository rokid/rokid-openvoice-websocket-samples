package com.rokid.speech.v1;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rokid.common.CustomSleep;
import com.rokid.common.SpeechSign;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import rokid.open.speech.Auth;
import rokid.open.speech.v1.SpeechTypes;
import rokid.open.speech.v1.SpeechV1;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * 作者: mashuangwei
 * 日期: 2017/9/18
 * 功能： 支持发送voice和文字
 */
@Slf4j
public class SpeechVt extends WebSocketClient {

    SpeechV1.SpeechRequest speechRequestStart;
    SpeechV1.SpeechRequest speechRequestVoi;
    SpeechV1.SpeechRequest speechRequestEnd;
    SpeechV1.SpeechRequest speechRequestText;

    private int sendId = 0;    //每次发送都是以这个值来表示建立的连接标识
    private boolean finishFlag = false;
    private boolean connectFlag = false;

    public SpeechVt(URI serverURI) {
        super(serverURI);
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        SSLSocketFactory factory = sslContext.getSocketFactory();
        try {
            this.setSocket(factory.createSocket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 认证登录
     * @param key
     * @param deviceTypeId
     * @param version
     * @param secret
     * @param deviceId
     */
    public void init(String key, String deviceTypeId, String version, String secret, String deviceId) {
        long time = System.currentTimeMillis();
        String src = "key=" + key + "&device_type_id=" + deviceTypeId + "&device_id=" + deviceId + "&service=speech&version=" + version + "&time=" + time + "&secret=" + secret;
        String sign = SpeechSign.getMD5(src);

        this.connect();
        for (int i = 0; i < 10; i++) {
            CustomSleep.sleep(0.5);
            if (connectFlag) {
                break;
            }
        }

        Auth.AuthRequest authRequest = Auth.AuthRequest.newBuilder().setDeviceId(deviceId).setDeviceTypeId(deviceTypeId).setKey(key).setService("speech").setVersion(version).setTimestamp("" + time).setSign(sign).build();
        this.send(authRequest.toByteArray());
    }

    /**
     * 模拟设备端不断的向服务器发送语音
     * @param codec
     * @param language
     * @param fileurl
     * @param activeWords
     * @param voice_power
     * @param intermediate_asr
     * @param no_nlp
     */
    public void sendDataByTime(String codec, String language, String fileurl, String activeWords, String voice_power, int intermediate_asr, int no_nlp) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("voice_power", voice_power);
        jsonObject.put("trigger_start", "0");       //用户使用时一般也是为0
        jsonObject.put("trigger_length", "9600");   //用户使用时一般也是为9600
        jsonObject.put("voice_trigger", activeWords);
        jsonObject.put("intermediate_asr", intermediate_asr);
        jsonObject.put("no_nlp", no_nlp);

        System.out.println("fro is " + jsonObject.toString());
        Random random = new Random();
        sendId = random.nextInt();
        speechRequestStart = SpeechV1.SpeechRequest.newBuilder().setId(sendId).setCodec(codec).setLang(language).setType(SpeechTypes.ReqType.START).setFrameworkOptions(jsonObject.toString()).build();
        this.send(speechRequestStart.toByteArray());

        FileInputStream fileInput = null;
        try {
            File file = new File(fileurl);
            byte[] buffer = new byte[9600];
            fileInput = new FileInputStream(file);
            int byteread = 0;
            // byteread表示一次读取到buffers中的数量。
            while ((byteread = fileInput.read(buffer)) != -1) {
                speechRequestVoi = SpeechV1.SpeechRequest.newBuilder().setId(sendId).setCodec(codec).setLang(language).setType(SpeechTypes.ReqType.VOICE).setVoice(ByteString.copyFrom(buffer)).build();
                this.send(speechRequestVoi.toByteArray());
                CustomSleep.sleep(0.3);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInput != null) {
                    fileInput.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        speechRequestEnd = SpeechV1.SpeechRequest.newBuilder().setId(sendId).setCodec(codec).setLang(language).setType(SpeechTypes.ReqType.END).build();
        this.send(speechRequestEnd.toByteArray());

        for (int i = 0; i < 10; i++) {
            if (finishFlag) {
                break;
            }
            CustomSleep.sleep(0.5);
        }

        System.out.println("send data finished!");

    }

    /**
     * 发送文本内容
     * @param tts
     */
    public void sendTts(String tts) {
        speechRequestText = SpeechV1.SpeechRequest.newBuilder().setId(new Random().nextInt()).setLang("zh").setAsr(tts).setType(SpeechTypes.ReqType.TEXT).build();
        this.send(speechRequestText.toByteArray());
        for (int i = 0; i < 10; i++) {
            if (finishFlag) {
                break;
            }
            CustomSleep.sleep(0.5);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        connectFlag = true;
        System.out.println("Connected");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("got: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        byte[] byteMessage = message.array();
        Auth.AuthResponse authResponse = null;
        SpeechV1.SpeechResponse spResponse;

        //第一次接收到的消息其实都是登录，这边只是巧合可以通过长度可以判断是不是登录。
        if (byteMessage.length == 2) {
            try {
                authResponse = Auth.AuthResponse.parseFrom(byteMessage);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

            System.out.println("Auth Result is " + authResponse.getResult());
            if (authResponse.getResult().equals(Auth.AuthErrorCode.AUTH_FAILED)) {
                System.out.println("Auth Result is " + authResponse.getResult());
                this.onClose(1006, "AUTH_FAILED", true);
            }
        } else {
            try {
                spResponse = SpeechV1.SpeechResponse.parseFrom(byteMessage);
                System.err.println("getAction: " + spResponse.getAction());
                System.err.println("getAsr: " + spResponse.getAsr());
                System.err.println("getExtra: " + spResponse.getExtra());
                System.err.println("getNlp: " + spResponse.getNlp());
                System.err.println("getResult: " + spResponse.getResult());
                log.info("getResult: {}", spResponse.getResult());
                finishFlag = spResponse.getFinish();
                System.err.println("getFinish: " + finishFlag);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected" + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

}
