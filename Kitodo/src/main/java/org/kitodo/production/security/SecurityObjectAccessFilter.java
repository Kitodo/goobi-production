/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kitodo.data.exceptions.DataException;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.security.SecurityAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.web.filter.GenericFilterBean;

/**
 * This filter handles the accessibility of urls which contains the parameter
 * "id". The access is denied if the user does not have the corresponding
 * authority for the current id.
 */
public class SecurityObjectAccessFilter extends GenericFilterBean {
    private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();
    private SecurityAccessService securityAccessService = ServiceManager.getSecurityAccessService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String id = httpServletRequest.getParameter("id");

        if (Objects.nonNull(id)) {
            try {
                Integer.parseInt(id);
            } catch (NumberFormatException e) {
                if (httpServletRequest.getRequestURI().contains("pages/workflowEdit")
                        && securityAccessService.hasAuthorityToViewWorkflow()) {
                    chain.doFilter(request, response);
                } else {
                    denyAccess(httpServletRequest, httpServletResponse);
                }
                return;
            }

            Map<String, Boolean> requested = new HashMap<>();
            try {
                requested.put("processEdit", securityAccessService.hasAuthorityToViewProcess(Integer.parseInt(id)));
            } catch (DataException e) {
                logger.error("Error on authority check" + e.getMessage());
            }
            requested.put("projectEdit", securityAccessService.hasAuthorityToViewProject(Integer.parseInt(id)));
            requested.put("templateEdit", securityAccessService.hasAuthorityToViewTemplate());
            requested.put("workflowEdit", securityAccessService.hasAuthorityToViewWorkflow());
            requested.put("docketEdit", securityAccessService.hasAuthorityToViewDocket());
            requested.put("rulesetEdit", securityAccessService.hasAuthorityToViewRuleset());
            requested.put("roleEdit", securityAccessService.hasAuthorityToViewRole());
            requested.put("clientEdit", securityAccessService.hasAuthorityToViewClient());

            for (Map.Entry<String, Boolean> entry : requested.entrySet()) {
                if (isAccessDenied(httpServletRequest, httpServletResponse, entry.getKey(), entry.getValue())) {
                    return;
                }
            }

            if (httpServletRequest.getRequestURI().contains("pages/userEdit")
                    && !securityAccessService.hasAuthorityToViewUser()
                    && !securityAccessService.hasAuthorityToConfigUser(Integer.parseInt(id))) {
                denyAccess(httpServletRequest, httpServletResponse);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAccessDenied(HttpServletRequest request, HttpServletResponse response, String page,
            boolean hasAuthority) throws IOException, ServletException {
        if (request.getRequestURI().contains("pages/" + page) && !hasAuthority) {
            denyAccess(request, response);
            return true;
        }
        return false;
    }

    private void denyAccess(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));
    }
}
