package ch.vd.vuta.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

public abstract class HtmlPage extends Page {

	protected static Logger LOGGER = Logger.getLogger(HtmlPage.class);

	private String resourceFileName;
	
	public HtmlPage(ApplicationContext ctx) {
		
		super(ctx);
	}
	
	public void setPageFileName(String filename) {
		this.resourceFileName = filename;
	}
	
	public void processPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		LOGGER.debug("HtmlPage.showPage");

		PrintWriter out = resp.getWriter();
		
		InputStream stream = getClass().getResourceAsStream(resourceFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line;
		while ((line = reader.readLine()) != null) {
			out.write(line);
		}
	}

}
