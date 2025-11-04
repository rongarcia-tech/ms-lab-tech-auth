package cl.duoc.ms_lab.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    private SecurityUtils() {}

    public static JwtUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof JwtUser ju) return ju;

        logger.warn("El principal del usuario autenticado no es un JwtUser, sino un {}. Se devolverá null.", auth.getPrincipal().getClass().getName());
        return null;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return roles.contains("ROLE_" + role);
    }
}
