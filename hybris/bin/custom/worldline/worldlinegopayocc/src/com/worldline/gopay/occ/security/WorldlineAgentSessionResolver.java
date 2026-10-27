package com.worldline.gopay.occ.security;

import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Tells whether the current OCC request is made by a customer support agent on behalf of a customer or a guest.
 * <p>
 * The decision is taken from the OAuth access token of the request, which is validated against the token store,
 * and not from the security context: for a guest cart SAP's {@code GuestRoleFilter} replaces the security context
 * with a guest authentication, so the agent role is no longer visible there. A value sent by the client is never trusted.
 * <p>
 * A request is an agent session when the token belongs to a user with {@value #AGENT_ROLE} and that user is not
 * the current user (the emulated customer, or the anonymous user of a guest checkout).
 */
@Component("worldlineAgentSessionResolver")
public class WorldlineAgentSessionResolver {

    static final String AGENT_ROLE = "ROLE_ASAGENTGROUP";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String RESOLVED_AGENT_ATTRIBUTE = WorldlineAgentSessionResolver.class.getName() + ".agentUid";
    private static final String NO_AGENT = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineAgentSessionResolver.class);

    @Resource(name = "userService")
    private UserService userService;

    @Resource(name = "oauthTokenServices")
    private ResourceServerTokenServices tokenServices;

    public boolean isAgentSession() {
        final String agentUid = getAgentUid();
        return agentUid != null && !agentUid.equals(userService.getCurrentUser().getUid());
    }

    /**
     * @return the uid of the customer support agent whose token was sent with the request, or {@code null}
     * when the token is missing, invalid, or does not belong to an agent
     */
    public String getAgentUid() {
        final HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        final Object resolved = request.getAttribute(RESOLVED_AGENT_ATTRIBUTE);
        if (resolved != null) {
            return NO_AGENT.equals(resolved) ? null : (String) resolved;
        }
        final String agentUid = resolveAgentUid(request);
        request.setAttribute(RESOLVED_AGENT_ATTRIBUTE, agentUid == null ? NO_AGENT : agentUid);
        return agentUid;
    }

    protected String resolveAgentUid(final HttpServletRequest request) {
        final String token = extractToken(request);
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            final OAuth2Authentication authentication = tokenServices.loadAuthentication(token);
            for (final GrantedAuthority authority : authentication.getAuthorities()) {
                if (AGENT_ROLE.equals(authority.getAuthority())) {
                    return authentication.getName();
                }
            }
        } catch (final RuntimeException e) {
            // invalid, expired or unknown token: not an agent session
            LOGGER.debug("Access token rejected while resolving the agent: {}", e.getMessage());
        }
        return null;
    }

    protected String extractToken(final HttpServletRequest request) {
        final Object token = request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);
        if (token instanceof String) {
            return (String) token;
        }
        final String header = request.getHeader(AUTHORIZATION_HEADER);
        return header != null && header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()).trim() : null;
    }

    protected HttpServletRequest getCurrentRequest() {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest() : null;
    }
}
