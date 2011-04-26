package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ch.vd.registre.web.filter.IAMUtil;

// FIXME (msi) déplacer cette classe dans Shared-web
// Copié-collé de la classe ATI + adapté à Spring 3
public class IAMAuthenticationProcessingFilter extends UsernamePasswordAuthenticationFilter {

    // Logger:
    protected Log logger = LogFactory.getLog(getClass());

    /** Username header key. Default is 'iam-userid'. */
    private String usernameHeaderKey = "iam-userid";

    /** Application header key. Default is 'iam-application'. */
    private String applicationHeaderKey = "iam-application";

    /** Roles header key. Default is 'iam-roles'. */
    private String rolesHeaderKey = "iam-roles";

    /**
     * Internal "roles header key".
     * Extra key so that end user may change "roles header key" value.
     * Value is "_internal-iam-roles-key".
     */
    final static String _INTERNAL_ROLES_HEADER_KEY = "_internal-iam-roles-key";

	public static final String ACEGI_SECURITY_FORM_USERNAME_KEY = "j_username";
	public static final String ACEGI_SECURITY_FORM_PASSWORD_KEY = "j_password";
	public static final String ACEGI_SECURITY_LAST_USERNAME_KEY = "ACEGI_SECURITY_LAST_USERNAME";
	public static final String ACEGI_SECURITY_CONTEXT_KEY = "ACEGI_SECURITY_CONTEXT";

    private boolean redirectResponse = false;

	private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

//  ---------------------------------------------------------------------------
//  Constructors
//  ---------------------------------------------------------------------------

    public IAMAuthenticationProcessingFilter()
    {
        super();

        // Default values (they are unused in this class):
        setFilterProcessesUrl("/");
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/error.jsp"));
    }

// ---------------------------------------------------------------------------
// Getters and setters
// ---------------------------------------------------------------------------

    public void setUsernameHeaderKey(final String key)
    {
        this.usernameHeaderKey = key;
    }

    public void setApplicationHeaderKey(String applicationHeaderKey)
    {
        this.applicationHeaderKey = applicationHeaderKey;
    }

    public void setRolesHeaderKey(String rolesHeaderKey)
    {
        this.rolesHeaderKey = rolesHeaderKey;
    }

    public boolean isRedirectResponse()
    {
        return redirectResponse;
    }

    /**
     * If true then <code>response.sendRedirect( response.encodeRedirectURL(targetUrl) ) </code>
     * is performed in 'successfulAuthentication()'. Default value is false.
     */
    public void setRedirectResponse(boolean redirectResponse)
    {
        this.redirectResponse = redirectResponse;
    }

// ---------------------------------------------------------------------------

    /**
     *
     * @see org.acegisecurity.ui.AbstractProcessingFilter#attemptAuthentication(javax.servlet.http.HttpServletRequest)
     */
    public Authentication attemptAuthentication(HttpServletRequest request)
        throws AuthenticationException
    {
        String username = null;
        String password = null;

        // Check headers for authentication info:
        if ((usernameHeaderKey != null) && (usernameHeaderKey.length() > 0))
        {
            username = request.getHeader(usernameHeaderKey);
        }

        // If the authentication info wasn't available, then get it from
        // the form parameters:
        if ((username == null) || (username.length() == 0))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("attemptAuthentication(): Authentication headers not found. Trying to use form values");
            }

            // Read username / password from form:
            username = request.getParameter(ACEGI_SECURITY_FORM_USERNAME_KEY);
            password = request.getParameter(ACEGI_SECURITY_FORM_PASSWORD_KEY);
        }

        // Do not perform authentication for null username:
        if ((username == null) || "".equals(username))
        {
            logger.warn("attemptAuthentication(): username is null");
            throw new UsernameNotFoundException("username is null");
        }

        if (password == null)
        {
            password = ""; // Set to blank to avoid a NPE.
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("attemptAuthentication(): username=" + username);
        }

        // Create authentication token:
        UsernamePasswordAuthenticationToken authRequest =
            new UsernamePasswordAuthenticationToken(username, password);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        // Place the last username attempted into HttpSession for views
        request.getSession().setAttribute(ACEGI_SECURITY_LAST_USERNAME_KEY,
            username);

        // Delegate authentication and find roles:
        return this.getAuthenticationManager().authenticate(authRequest);
    }


    /**
     * Redefined. A Hashtable with the following entry is put as Details:
     * <ul><li> key = _INTERNAL_ROLES_HEADER_KEY
     * </li><li> value = String[], which contains the roles retrieved from
     * the upfront system (i.e. IAM) for the current application.
     * </li></ul>
     * The roles header key would contain all of the roles of the user.
     * Split these roles and only take those that are relevant to this application.
     *
     * @see ch.vd.ati.security.IAMUtil#createsRoles
     */
    protected void setDetails(HttpServletRequest request,
        UsernamePasswordAuthenticationToken authRequest)
    {
        // Retrieve application and roles from header keys:
        String application = request.getHeader(applicationHeaderKey);
        String allRoles =  request.getHeader(rolesHeaderKey);

        // Extract business roles:
        String[] roles = IAMUtil.createRoles(application, allRoles);

        // Put business roles in Hashtable and put it in Details:
        Hashtable h = new Hashtable();
        h.put(_INTERNAL_ROLES_HEADER_KEY, roles);
        authRequest.setDetails(h);
    }


    /**
     * Redefined. True if no SecurityContext has been found.
     * @see org.acegisecurity.ui.AbstractProcessingFilter#requiresAuthentication(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected boolean requiresAuthentication(final HttpServletRequest request, final HttpServletResponse response)
    {
        boolean bAuthenticated = false;
        SecurityContext context = getSecurityContext(request);

        if (context != null)
        {
            Authentication auth = context.getAuthentication();

            if ((auth != null) && auth instanceof UsernamePasswordAuthenticationToken)
            {
                UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
                bAuthenticated = token.isAuthenticated();

                if (bAuthenticated)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("requiresAuthentication(): Already authenticated: principal=" + token.getName());
                    }
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("requiresAuthentication(): result=" + (!bAuthenticated));
        }
        return (!bAuthenticated);
    }

	/**
	 * Returns SecurityContext if any from 'request'.
	 * The security context is stored in 'HttpSessionContextIntegrationFilter.ACEGI_SECURITY_CONTEXT_KEY'
	 * session attribute.
	 * Requires 'request' is not null.
	 */
	static public SecurityContext getSecurityContext(final HttpServletRequest request)
	{
	    SecurityContext result = (SecurityContext) request.getSession().getAttribute(ACEGI_SECURITY_CONTEXT_KEY);
	    return result;
	}


    /**
     * Redefined. No 'response.sendRedirect()' is performed.
     * @see org.acegisecurity.ui.AbstractProcessingFilter#successfulAuthentication()
     */
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException, ServletException
    {
        SecurityContextHolder.getContext().setAuthentication(authResult);

        if (logger.isDebugEnabled())
        {
            logger.debug("successfulAuthentication(): Updated SecurityContextHolder with Authentication='" + authResult + "'");
        }

        getRememberMeServices().loginSuccess(request, response, authResult);

        // Fire event
        if (this.eventPublisher != null)
        {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }

        // 2006-07-19 Savrak: No redirect Response:
        if (! isRedirectResponse())
        {
            return;
        }

        // At this point, redirection must be performed to the relevant targetUrl.
	    successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    /**
     * Redefined. Commented out 'return' after 'successfulAuthentication()',
     * which implies no 'response.sendRedirect()' is performed.
     * @see org.acegisecurity.ui.AbstractProcessingFilter#doFilter()
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (!(request instanceof HttpServletRequest))
        {
            throw new ServletException("doFilter(): Can only process HttpServletRequest");
        }

        if (!(response instanceof HttpServletResponse))
        {
            throw new ServletException("doFilter(): Can only process HttpServletResponse");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (requiresAuthentication(httpRequest, httpResponse))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("doFilter(): Processing authentication");
            }

            Authentication authResult;

            try
            {
                authResult = attemptAuthentication(httpRequest);
            }
            catch (AuthenticationException failed)
            {
                // Authentication failed
                unsuccessfulAuthentication(httpRequest, httpResponse, failed);

                return;
            }

            // Authentication success
            successfulAuthentication(httpRequest, httpResponse, authResult);

            // 2006-07-19 Savrak:
            // See also successfulAuthentication()
            if (isRedirectResponse())
            {
                return;
            }
        }

        chain.doFilter(request, response);
    }

	/**
	 * Sets the strategy used to handle a successful authentication.
	 * By default a {@link SavedRequestAwareAuthenticationSuccessHandler} is used.
	 */
	public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler successHandler) {
		super.setAuthenticationSuccessHandler(successHandler);
	    this.successHandler = successHandler;
	}

}
