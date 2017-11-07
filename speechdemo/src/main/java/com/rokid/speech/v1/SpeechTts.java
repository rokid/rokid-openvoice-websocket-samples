package com.rokid.speech.v1;

import com.google.protobuf.InvalidProtocolBufferException;
import com.rokid.common.CustomSleep;
import com.rokid.common.SpeechSign;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import rokid.open.speech.Auth;
import rokid.open.speech.v1.SpeechTypes;
import rokid.open.speech.v1.Tts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 作者: mashuangwei
 * 日期: 2017/9/22
 */
@Data
@Slf4j
public class SpeechTts extends WebSocketClient {

    protected int sendId = 0;
    private volatile boolean finishFlag = false;

    private boolean hasVoice = false;
    private boolean hasFinish = true;
    private String action = "";
    private boolean finish = false;
    private boolean hasText = false;
    private String result = "";
    private boolean hasId = false;
    boolean connectFlag = false;
    String filename = System.getProperty("user.dir") + "/src/main/resources/files/" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ".opu2";

    public SpeechTts(URI serverURI) {
        super(serverURI);
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            this.setSocket(factory.createSocket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(String key, String deviceTypeId,  String secret, String deviceId) {
        long time = System.currentTimeMillis();
        String beforeSign = "key=" + key + "&device_type_id=" + deviceTypeId + "&device_id=" + deviceId + "&service=tts&version=1.0&time=" + time + "&secret=" + secret;
        String sign = SpeechSign.getMD5(beforeSign);

        this.connect();
        for (int i = 0; i < 10; i++) {
            CustomSleep.sleep(0.5);
            if (connectFlag) {
                break;
            }
        }
        Auth.AuthRequest authRequest = Auth.AuthRequest.newBuilder().setDeviceId(deviceId).setDeviceTypeId(deviceTypeId).setKey(key).setService("tts").setVersion("1.0").setTimestamp("" + time).setSign(sign).build();
        this.send(authRequest.toByteArray());
        CustomSleep.sleep(1);

    }

    public void sendTts(String tts) {
        Tts.TtsRequest ttsRequest = Tts.TtsRequest.newBuilder().setId(new Random().nextInt()).setDeclaimer("zh").setCodec("pcm").setText(tts).build();
        this.send(ttsRequest.toByteArray());
        for (int i = 0; i < 10; i++) {
            if (finishFlag) {
                finishFlag = false;
                break;
            }
            CustomSleep.sleep(0.5);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("Connected");
        connectFlag = true;
    }

    @Override
    public void onMessage(String message) {
        log.info("got: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        byte[] byteMessage = message.array();
        Auth.AuthResponse authResponse = null;
        Tts.TtsResponse ttsResponse = null;

        if (byteMessage.length == 2) {
            try {
                authResponse = Auth.AuthResponse.parseFrom(byteMessage);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

            log.info("Auth Result is " + authResponse.getResult());
            if (authResponse.getResult().equals(Auth.AuthErrorCode.AUTH_FAILED)) {
                log.info("Auth Result is " + authResponse.getResult());
                this.onClose(1006, "AUTH_FAILED", true);
            }
        } else {
            try {
                ttsResponse = Tts.TtsResponse.parseFrom(byteMessage);
                log.info("getFinish: {}" , ttsResponse.getFinish());
                log.info("getText: {}" ,ttsResponse.getText());
                log.info("getVoice: {}" ,ttsResponse.getVoice());
                log.info("hasVoice: {}" ,ttsResponse.hasVoice());
                log.info("hasFinish: {}" ,ttsResponse.hasFinish());
                log.info("hasText: {}" ,ttsResponse.hasText());
                log.info("hasId: {}" ,ttsResponse.hasId());
                log.info("getResult: {}" , ttsResponse.getResult());
                log.info("---------------------------------------------" );

                if (ttsResponse.hasVoice() && !ttsResponse.getFinish()){
                    try {
                        File outfile = new File(filename);
                        if (!outfile.exists()) {
                            outfile.createNewFile();
                        }
                        DataOutputStream fw = new DataOutputStream(new FileOutputStream(outfile,true));
                        fw.write(ttsResponse.getVoice().toByteArray());
                        fw.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                hasFinish = ttsResponse.hasFinish();
                hasId = ttsResponse.hasId();
                hasText = ttsResponse.hasText();
                hasVoice = ttsResponse.hasVoice();
                result = SpeechTypes.SpeechErrorCode.forNumber(ttsResponse.getResult().getNumber()).toString();
                finish = finishFlag = ttsResponse.getFinish();

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Disconnected" + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }


}
