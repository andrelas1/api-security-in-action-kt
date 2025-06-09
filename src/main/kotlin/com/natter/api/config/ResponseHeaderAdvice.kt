package com.natter.api.config

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import com.natter.api.controller.SpaceController

@ControllerAdvice(assignableTypes = [SpaceController::class])
class ResponseHeaderAdvice : ResponseBodyAdvice<Any> {

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        val headers = response.headers
        
        headers.add("X-XSS-Protection", "0")
        headers.add("X-Content-Type-Options", "nosniff")
        headers.add("X-Frame-Options", "DENY")
        headers.add("Cache-Control", "no-store")
        headers.add("Expires", "0")
        headers.add("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox 'n/a'")
        
        return body
    }
}