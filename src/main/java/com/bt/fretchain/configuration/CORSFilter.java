package com.bt.fretchain.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class CORSFilter extends GenericFilterBean implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "*");// http://localhost
        response.setHeader("Access-Control-Allow-Methods", "*");//POST, PUT, GET, OPTIONS, DELETE
        response.setHeader("Access-Control-Allow-Headers", "*");//x-requested-with
        response.setHeader("Access-Control-Allow-Credentials", "false");//true
        response.setHeader("Access-Control-Max-Age", "3600");
        chain.doFilter(req, res);
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return false;
    }

}