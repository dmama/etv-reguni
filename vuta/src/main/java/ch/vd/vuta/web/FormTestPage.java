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

public class FormTestPage extends HtmlPage {

	protected static Logger LOGGER = Logger.getLogger(FormTestPage.class);

	public FormTestPage(ApplicationContext context) {
		
		super(context);

		setPageFileName("/testPage.html");
	}
	
}
