package com.example.aiproject.lovable_clone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.Array2DHashSet;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final AuthUtil authUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    log.info("incoming request: {}",request.getRequestURI());
    final String requestHeaderToken = request.getHeader("Authorization");
    if(requestHeaderToken==null || !requestHeaderToken.startsWith("Bearer ")){
        filterChain.doFilter(request,response);
        return;
    }
    String jwtToken=requestHeaderToken.split("Bearer ")[1];
    JwtUserPrincipal userPrincipal = authUtil.verifyAccessToken(jwtToken);
    // if have validated token and context does not have info about the user than
    if(userPrincipal!=null  && SecurityContextHolder.getContext().getAuthentication()==null){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userPrincipal,null, new ArrayList<>()
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
    filterChain.doFilter(request,response);
    }
}
