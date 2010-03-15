package ch.vd.vuta.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

public class CheckPage extends Page {

	private static Logger LOGGER = Logger.getLogger(CheckPage.class);

	public CheckPage(ApplicationContext context) {
		super(context);
	}
	
	@Override
	public void processPage(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		showHeader(out, "Pas de check pour l'exploitant");
		showMenu(out);
		
		out.print("<h1>L'application SMS Gateway fonctionne correctement</h1>");
		out.print("<hr>");
		
		showFooter(out);
	}

}
