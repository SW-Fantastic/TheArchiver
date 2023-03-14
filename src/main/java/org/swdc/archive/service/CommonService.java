package org.swdc.archive.service;


import info.monitorenter.cpdetector.io.*;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.universalchardet.UniversalDetector;
import org.swdc.fx.FXResources;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CommonService {

    private CodepageDetectorProxy detectorProxy;

    @Inject
    private FXResources resources;

    @PostConstruct
    public void initService() {
        detectorProxy = CodepageDetectorProxy.getInstance();
        detectorProxy.add(new ParsingDetector(false));
        detectorProxy.add(UnicodeDetector.getInstance());
        detectorProxy.add(JChardetFacade.getInstance());
        detectorProxy.add(ASCIIDetector.getInstance());
    }

    public void submit(Runnable runnable) {
        resources.getExecutor().submit(runnable);
    }

    /**
     * 对字符串的编码格式进行推断，
     * @param inputStream 一般是ByteArrayInputStream。
     * @param length 数据长度。
     * @return Charset类型。
     */
    public Charset getCharset(InputStream inputStream, int length) {
        try {

            HashMap<Charset,Integer> charsetMap = new HashMap<>();

            // 1. CodepageDetector进行判别。
            byte[] data = inputStream.readAllBytes();
            Charset charset = detectorProxy.detectCodepage(inputStream,length);

            charsetMap.put(charset,1);

            // 2. Google Universal进行判别
            UniversalDetector universalDetector = new UniversalDetector(null);
            universalDetector.handleData(data,0,data.length);
            universalDetector.dataEnd();

            // 3. jchardet 进行判别
            nsDetector dect = new nsDetector();
            dect.DoIt(data,data.length,false);

            dect.DataEnd();

            String prob[] = dect.getProbableCharsets();
            for (int i = 0; i < prob.length; i++) {
                charset = Charset.forName(prob[i]);
                if (charsetMap.containsKey(charset)) {
                    int count = charsetMap.get(charset);
                    charsetMap.put(charset,count + 1);
                } else {
                    charsetMap.put(charset,1);
                }
            }

            String charsetName = universalDetector.getDetectedCharset();
            if (charsetName != null) {
                Charset unCharset = Charset.forName(charsetName);
                if (charsetMap.containsKey(unCharset)) {
                    int count = charsetMap.get(unCharset);
                    charsetMap.put(unCharset,count);
                } else {
                    charsetMap.put(unCharset,1);
                }
            }

            // 进行先行判别，如果判断中存在以下Charset则直接断定为它。

            if (charsetMap.containsKey(StandardCharsets.UTF_8)) {
                return StandardCharsets.UTF_8;
            } else if (charsetMap.containsKey(StandardCharsets.ISO_8859_1)) {
                return StandardCharsets.ISO_8859_1;
            } else if (charsetMap.containsKey(Charset.forName("GB18030"))) {
                return Charset.forName("GB18030");
            } else if (charsetMap.containsKey(Charset.forName("GB2312"))) {
                return Charset.forName("GB2312");
            } else {
                // 不包含上述常见的Charset，根据Charset的出现次数判断。

                Map.Entry<Charset,Integer> selected = charsetMap.entrySet()
                        .stream()
                        .max(Comparator.comparingInt(Map.Entry::getValue))
                        .orElse(null);

                if (selected == null) {
                    return StandardCharsets.UTF_8;
                } else {
                    return selected.getKey();
                }

            }

        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    public Charset getCharset(File file) {
        try {
            URL url = file.toURI().toURL();
            return detectorProxy.detectCodepage(url);
        } catch (Exception e) {
            e.printStackTrace();
            return StandardCharsets.UTF_8;
        }
    }



}
