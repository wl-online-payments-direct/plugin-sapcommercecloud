
package com.worldline.gopay.occ.security;

import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.core.model.security.PrincipalGroupModel;

import java.util.Collection;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component("worldlineAgentSessionResolver")
public class WorldlineAgentSessionResolver {

    static final String AGENT_ROLE = "ROLE_ASAGENTGROUP";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String RESOLVED_AGENT_ATTRIBUTE =
            WorldlineAgentSessionResolver.class.getName() + ".agentUid";
    private static final String NO_AGENT = "";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(WorldlineAgentSessionResolver.class);

    @Resource(name = "userService")
    private UserService userService;

    /*
     * Must be the JwtDecoder configured for the Commerce resource server.
     * Confirm the bean name/type in your Commerce configuration.
     */
    @Resource
    private JwtDecoder jwtDecoder;

    public boolean isAgentSession() {
        final String agentUid = getAgentUid();
        return agentUid != null
                && !agentUid.equals(userService.getCurrentUser().getUid());
    }

    /**
     * @return the UID of the customer support agent whose token was sent
     * with the request, or null if the token is missing, invalid,
     * or does not belong to an agent.
     */
    public String getAgentUid() {
        final HttpServletRequest request = getCurrentRequest();

        if (request == null) {
            return null;
        }

        final Object resolved =
                request.getAttribute(RESOLVED_AGENT_ATTRIBUTE);

        if (resolved != null) {
            return NO_AGENT.equals(resolved) ? null : (String) resolved;
        }

        final String agentUid = resolveAgentUid(request);

        request.setAttribute(
                RESOLVED_AGENT_ATTRIBUTE,
                agentUid == null ? NO_AGENT : agentUid);

        return agentUid;
    }

    protected String resolveAgentUid(final HttpServletRequest request) {
        final String token = extractToken(request);

        if (StringUtils.isBlank(token)) {
            return null;
        }

        try {
            // Validates the JWT using the configured decoder.
            final Jwt jwt = jwtDecoder.decode(token);

            /*
             * Use the JWT subject to locate the Commerce user.
             * Verify that the subject in your tokens is the Commerce UID.
             */
            final String subject = jwt.getSubject();

            if (StringUtils.isBlank(subject)) {
                return null;
            }

            final UserModel agent = userService.getUserForUID(subject);

            final Collection< ? extends PrincipalGroupModel> groups =
                    agent.getGroups();

            for (final PrincipalGroupModel group : groups) {
                if (AGENT_ROLE.equals(group.getUid())) {
                    return agent.getUid();
                }
            }

        } catch (final RuntimeException e) {
            // Invalid, expired or unknown token, or user lookup failure.
            LOGGER.debug(
                    "Access token rejected while resolving the agent: {}",
                    e.getMessage());
        }

        return null;
    }

    protected String extractToken(final HttpServletRequest request) {
        final String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null
                && header.regionMatches(
                true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }

        return null;
    }

    protected HttpServletRequest getCurrentRequest() {
        final RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();

        return attributes instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attributes).getRequest()
                : null;
    }
}
