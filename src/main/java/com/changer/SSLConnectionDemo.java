package com.changer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.changer.Constants.HTTPS_PROTOCOL;

public class SSLConnectionDemo {
    private static final String HOST = "api.nasa.gov";
    private static final String PATH = "/mars-photos/api/v1/rovers/curiosity/photos?sol=16&api_key=CoWicfDGQaa3LIzyDeOq4tk4MG0heBeACNNXVgay";
    private static final String PHOTO_URL_FIELD = "img_src";
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @SneakyThrows
    public static void main(String[] args) {
        List<URL> photosUrls = getPhotosUrls();
        getLargestPhoto(photosUrls).ifPresent(photo -> {
            System.out.println("img_src: " + photo.url());
            System.out.println("length: " + photo.length());
        });
    }

    @SneakyThrows
    private static List<URL> getPhotosUrls() {
        URL photosUrl = new URL(HTTPS_PROTOCOL, HOST, PATH);

        HttpResponse response = HttpSocket.getResponse(HttpMethod.GET, photosUrl);

        String photosJson = response.body()
                                    .stream()
                                    .filter(line -> !isChunkLength(line))
                                    .collect(Collectors.joining());

        return objectMapper.readTree(photosJson)
                           .findValuesAsText(PHOTO_URL_FIELD)
                           .stream()
                           .map(UrlUtils::toUrl)
                           .toList();
    }

    private static Optional<Photo> getLargestPhoto(List<URL> photosUrls) {
        return photosUrls.parallelStream()
                         .map(url -> new Photo(url, getLength(url)))
                         .max(Comparator.comparing(Photo::length));
    }

    private static boolean isChunkLength(String line) {
        try {
            int hexBase = 16;
            Long.parseLong(line, hexBase);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @SneakyThrows
    private static long getLength(URL url) {
        return HttpSocket.getResponse(HttpMethod.HEAD, url)
                         .headers()
                         .stream()
                         .filter(line -> line.contains("Content-Length: "))
                         .findFirst()
                         .map(line -> line.substring(line.indexOf(" ")).trim())
                         .map(Long::parseLong)
                         .orElse(0L);
    }
}
