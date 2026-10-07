package org.brainbot.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.brainbot.api.repo.UserRepository;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    public JwtFilter(JwtService jwt,UserRepository users){this.jwt=jwt;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String header=req.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")) try {
            var claims=jwt.parse(header.substring(7));
            var account=users.findById(Long.parseLong(claims.getSubject())).orElseThrow();
            if(!Long.valueOf(account.getPasswordResetVersion()).equals(claims.get("passwordResetVersion",Long.class))) throw new IllegalArgumentException("Session has been revoked");
            var auth=new UsernamePasswordAuthenticationToken(claims.getSubject(),null,List.of(new SimpleGrantedAuthority("ROLE_"+claims.get("role",String.class))));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch(Exception ignored) { SecurityContextHolder.clearContext(); }
        chain.doFilter(req,res);
    }
}
