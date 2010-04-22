package ch.vd.vuta.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ch.vd.vuta.SmsgwProperties;

public class MainServlet extends HttpServlet {

	private static final long serialVersionUID = -4878624018923499617L;

	public static Logger LOGGER = Logger.getLogger(MainServlet.class);
	
	public static final String RECEIVE_URL = "/receive.do";
	public static final String CHECK_APP_URL = "/checkApp.do";
	
	private static List<String> allAuthorizedIps = new ArrayList<String>();
	private static List<String> dsiAuthorizedIps = new ArrayList<String>();
	private static final String LOCAL_IP = "127.0.0.1";
	private static final String MNC_IP = "212.23.250.42";
	private static final String JEC_IP = "10.240.6.90";
	private static final String PKR_IP = "10.240.x.x";
	private static final String DDO_IP = "10.240.x.x";


	public void init() {
		
		if (SmsgwProperties.getLog4jConfigFile() != null) {
			LOGGER.info("Config de log4j: utilisation de '"+SmsgwProperties.getLog4jConfigFile()+"'");
			DOMConfigurator.configureAndWatch(SmsgwProperties.getLog4jConfigFile());
		}
		else {
			LOGGER.info("Config de log4j: utilisation de '"+SmsgwProperties.getDefaultLog4jConfigFile()+"'");
			DOMConfigurator.configure(SmsgwProperties.getDefaultLog4jConfigFile());
		}
		
		allAuthorizedIps.add(LOCAL_IP);
		allAuthorizedIps.add(MNC_IP);
		allAuthorizedIps.add(JEC_IP);
		allAuthorizedIps.add(JEC_IP);
		allAuthorizedIps.add(DDO_IP);
		
		dsiAuthorizedIps.add(LOCAL_IP);
		dsiAuthorizedIps.add(PKR_IP);
		dsiAuthorizedIps.add(PKR_IP);
		dsiAuthorizedIps.add(DDO_IP);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		LOGGER.debug("Appel de doGet");
		treatUrl(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		LOGGER.debug("Appel de doPost");
		treatUrl(req, resp);
	}

	private boolean checkSecurity(HttpServletRequest req) throws ServletException {
		
		boolean ok = false;
		
		String ip = req.getLocalAddr();
		String url = req.getServletPath();
		
		if (url.equals(CHECK_APP_URL)) {
			ok = true; // Tout le monde a accès
		}
		else if (url.equals(RECEIVE_URL)) {
			if (allAuthorizedIps.contains(ip)) {
				ok = true; // All right
			}
		}
		else {
			if (dsiAuthorizedIps.contains(ip)) {
				ok = true; // All right
			}
		}
		
		// TEMP
		// Coupe le controle d'acces pour le moment
		ok = true;
		LOGGER.info("WARNING: Authorization bypassed!!!");
		// TEMP
		
		if (ok) {
			LOGGER.info("Authorization OK for ip="+ip+" url='"+url+"'");
		}
		else {
			LOGGER.info("Authorization failed: ip="+ip+" url='"+url+"'");
		}
		return ok;
	}
	
	private void treatUrl(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		LOGGER.info("Appel de la page: "+req.getServletPath());

		try {

			if (checkSecurity(req)) {
				
				if (req.getServletPath().equals("/testForm.do")) {
					
					treatPage(new FormTestPage(getApplicationContext()), req, resp);
				}
				else if (req.getServletPath().equals("/test.do")) {
					
					treatPage(new PostTestPage(getApplicationContext()), req, resp);
				}
				else if (req.getServletPath().equals("/delete.do")) {
					
					treatPage(new DeleteSmsPage(getApplicationContext()), req, resp);
				}
				else if (req.getServletPath().equals(RECEIVE_URL)) {
					
					treatPage(new Ifd2008Page(getApplicationContext()), req, resp);
				}
				else if (req.getServletPath().equals(CHECK_APP_URL)) {
					
					treatPage(new CheckPage(getApplicationContext()), req, resp);
				}
				else {
					// Default => ListPage
					treatPage(new ListSmsPage(getApplicationContext()), req, resp);
				}
			}
			else {
				showForbiddenPage(resp.getWriter());
			}

		}
		catch (IOException e) {
			LOGGER.error(e);
		}
		catch (ServletException e) {
			LOGGER.error(e);
		}
		catch (Exception e) {
			LOGGER.error(e);
		}
		LOGGER.info("Fin d'appel: "+req.getServletPath());
	}


	private void treatPage(Page page, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		boolean error = false;
		page.beforeProcess();
		try {
			page.processPage(req, resp);
		}
		catch (Exception e) {
			e.printStackTrace();
			error = true;
		}
		page.afterProcess(error);
	}

	private ApplicationContext getApplicationContext() {
		return WebApplicationContextUtils.getWebApplicationContext(getServletContext());
	}
	
	private void showForbiddenPage(PrintWriter out) {
	
		out.print("<H1>Verboten!</h1>");
	}
}
