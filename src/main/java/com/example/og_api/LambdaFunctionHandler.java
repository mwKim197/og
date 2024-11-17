package com.example.og_api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.og_api.api.og.model.OgData;
import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LambdaFunctionHandler implements RequestHandler<InputStream, OutputStream> {


    @Override
    public OutputStream handleRequest(InputStream inputStream, Context context) {
        // 요청 데이터를 읽기 위한 StringBuilder
        StringBuilder inputData = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputData.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 요청 본문을 OgData 객체로 변환
        String requestBody = inputData.toString();
        OgData urlData = new Gson().fromJson(requestBody, OgData.class);

        // 파싱된 데이터 확인
        System.out.println("URL: " + urlData.getUrl());

        // OpenGraph 메타 태그를 추출할 변수 준비
        OgData ogData = new OgData();

        try {
            // URL을 Jsoup로 처리하여 OpenGraph 메타 태그 추출
            Document doc = Jsoup.connect(urlData.getUrl()).get();

            // OpenGraph 메타 태그에서 데이터를 추출
            for (Element meta : doc.select("meta[property^=og:]")) {
                String property = meta.attr("property"); // 예: "og:title"
                String content = meta.attr("content");  // 예: "사이트 제목"

                // 각 OpenGraph 속성에 맞춰 OgData 설정
                if ("og:title".equals(property)) {
                    ogData.setTitle(content);
                } else if ("og:description".equals(property)) {
                    ogData.setDescription(content);
                } else if ("og:regDate".equals(property)) {
                    ogData.setRegDate(content);
                } else if ("og:image".equals(property)) {
                    ogData.setImage(content);
                } else if ("og:type".equals(property)) {
                    ogData.setType(content);
                } else if ("og:site_name".equals(property)) {
                    ogData.setSite_name(content);
                } else if ("og:url".equals(property)) {
                    ogData.setUrl(content);
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException("Error fetching OpenGraph data", ex);
        }

        // 응답을 JSON 형식으로 변환하여 OutputStream에 작성
        String responseBody = new Gson().toJson(ogData);

        try {
            OutputStream outputStream = System.out;  // Lambda 응답은 OutputStream에 작성
            outputStream.write(responseBody.getBytes(StandardCharsets.UTF_8));
            return outputStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}