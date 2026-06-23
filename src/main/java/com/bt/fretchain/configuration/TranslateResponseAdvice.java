package com.bt.fretchain.configuration;

import com.bt.fretchain.bean.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class TranslateResponseAdvice implements ResponseBodyAdvice<Object> {

    private final MessageSource messageSource;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body instanceof ResponseData data) {

            String lang = request.getHeaders().getFirst("Accept-Language");

            Locale locale = "fr".equalsIgnoreCase(lang)
                    ? Locale.FRENCH
                    : Locale.ENGLISH;

            String translated = messageSource.getMessage(
                    data.getMessage(),
                    null,
                    data.getMessage(),
                    locale
            );

            data.setMessage(translated);
        }

        return body;
    }
}