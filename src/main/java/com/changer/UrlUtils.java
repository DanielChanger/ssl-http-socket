package com.changer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.URL;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlUtils {
    @SneakyThrows
    public static URL toUrl(String uri) {
        return URI.create(uri).toURL();
    }
}
