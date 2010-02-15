package ch.vd.vuta.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

public abstract class Page {
	
	private static Logger LOGGER = Logger.getLogger(Page.class);
	
	private ApplicationContext applicationContext;

	public abstract void processPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
	
	
	// Constructor
	public Page(ApplicationContext context) {
		applicationContext = context;
	}
	
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	protected void showHeader(PrintWriter out, String title) {
		
		out.write("<HTML>");
		out.write("<HEAD>");
		out.write("<TITLE>Sms gateway - "+title+"</TITLE>");
		out.write("</HEAD>");
		out.write("</BODY>");
	}
	
	protected void showFooter(PrintWriter out) {
		
		out.write("</BODY>");
		out.write("</HTML>");
	}
	
	protected void showMenu(PrintWriter out) {
		
		out.write("<table>\n");
		out.write("<tr>\n");
		out.write("<td><a href=\"list.do\">Liste des SMS</a></td>\n");
		out.write("<td><a href=\"testForm.do\">Lance un test</a></td>\n");
		out.write("</tr>\n");
		out.write("</table>\n");
	}

	public void beforeProcess() {
		
	}
	public void afterProcess(boolean errorHappened) {
		
	}

}
