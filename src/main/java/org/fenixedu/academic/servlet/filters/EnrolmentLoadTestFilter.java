package org.fenixedu.academic.servlet.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;

@WebFilter(value = "/enrolment-load-test", asyncSupported = true)
public class EnrolmentLoadTestFilter implements Filter {

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authenticate.mock(User.findByUsername("<username>"), "no method");
            chain.doFilter(request, response);
        } finally {
            Authenticate.unmock();
        }
    }

    @Override
    public void destroy() {

    }

}
