package ch.vd.vuta.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import ch.vd.vuta.processing.SmsProcessor;

public class PostTestPage extends Page {

	protected static Logger LOGGER = Logger.getLogger(PostTestPage.class);

	public PostTestPage(ApplicationContext ctx) {
		
		super(ctx);
	}
	
	@Override
	public void processPage(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try {
			// Decode parameters
			String natel = req.getParameter("natel");
			LOGGER.debug("Natel:"+natel);
			String texte = req.getParameter("texte");
			texte.trim();
			LOGGER.debug("Texte:"+texte);
			String operateur = req.getParameter("operateur");
			LOGGER.debug("Operateur:"+operateur);
			String langue = req.getParameter("langue");
			LOGGER.debug("Langue:"+langue);
			String requestUid = req.getParameter("requestUid");
			LOGGER.debug("requestUid:'"+requestUid+"'");
			LOGGER.debug("Appel de getSmsAsXml");
			
			String smsAsXml = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
			LOGGER.debug("SMS: "+smsAsXml);
			
			String servletUrl = req.getRequestURL().toString();
			int index = servletUrl.lastIndexOf("/");
			if (index > 0) {
				servletUrl = servletUrl.substring(0, index);
			}
			
			URL url = new URL(servletUrl+"/receive.do");
			URLConnection conn = (URLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			// Send the data
			OutputStream httpOut = conn.getOutputStream();
			httpOut.write(smsAsXml.getBytes());
			httpOut.close(); // Push to server
			LOGGER.debug("Sms sent to servlet");
			
			// Get the data back
			InputStream in = conn.getInputStream();
			BufferedReader stream = new BufferedReader(new InputStreamReader(in));
			String respAsXml = "";
			String line;
			while ((line = stream.readLine()) != null) {
				respAsXml += line;
			}
			LOGGER.debug("Response received from servlet: "+respAsXml);
	
			// Send the answer
			PrintWriter out = resp.getWriter();
			showHeader(out, "Response from servlet");
			showMenu(out);
			
			out.write("<h1>Response</h1>");
			//out.write("<pre>");
			respAsXml = respAsXml.replaceAll("&", "&amp;");
			respAsXml = respAsXml.replaceAll(">", "&gt;");
			respAsXml = respAsXml.replaceAll("<", "&lt;");
			respAsXml = respAsXml.replaceAll("\n", "<br>");
			out.write(respAsXml);
			//out.write("</pre>");
	
			showFooter(out);
		}
		catch (Exception e) {
			// Send the answer
			PrintWriter out = resp.getWriter();
			showHeader(out, "Response from servlet");
			showMenu(out);

			out.write("<h1>Erreur!</h1>");
			e.printStackTrace(out);
			
			showFooter(out);
		}
	}

}
