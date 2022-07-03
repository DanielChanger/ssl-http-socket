package com.changer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.changer.Constants.HTTPS_PROTOCOL;
import static com.changer.Constants.HTTP_PROTOCOL;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpSocket {
    private static final int HTTPS_PORT = 443;
    private static final int HTTP_PORT = 80;

    private static final String GET_REQUEST_TEMPLATE = """
            %s %s HTTP/1.1
            Host: %s

            """;

    @SneakyThrows
    public static HttpResponse getResponse(HttpMethod method, URL url) {
        HttpResponse response;
        Optional<URL> location = Optional.of(url);
        do {
            try (Socket socket = createHttpSocket(location.get())) {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                writer.format(GET_REQUEST_TEMPLATE, method, url.getFile(), url.getHost());

                response = getResponse(method, socket);
                location = parseLocation(response.headers());
            }
        } while (location.isPresent());

        return response;
    }

    @SneakyThrows
    private static HttpResponse getResponse(HttpMethod method, Socket socket) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        List<String> headers = reader.lines()
                                     .takeWhile(line -> !line.isEmpty())
                                     .toList();

        List<String> body;
        if (method == HttpMethod.HEAD) {
            body = Collections.emptyList();
        } else {
            body = reader.lines()
                         .takeWhile(line -> !line.isEmpty() && !Constants.CHUNKS_END_LINE.equals(line))
                         .toList();
        }

        return new HttpResponse(headers, body);
    }

    private static Optional<URL> parseLocation(List<String> responseLines) {
        return responseLines.stream()
                            .filter(line -> line.contains("Location: "))
                            .map(line -> line.substring(line.indexOf(" ")).trim())
                            .map(UrlUtils::toUrl)
                            .findFirst();
    }

    @SneakyThrows
    private static Socket createHttpSocket(URL url) {
        if (url.getProtocol().equals(HTTP_PROTOCOL)) {
            return SocketFactory.getDefault().createSocket(url.getHost(), HTTP_PORT);
        } else if (url.getProtocol().equals(HTTPS_PROTOCOL)) {
            return SSLSocketFactory.getDefault().createSocket(url.getHost(), HTTPS_PORT);
        }
        throw new IllegalArgumentException("Unsupported url");
    }
}
