package ch.vd.vuta.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import ch.vd.vuta.model.SmsDAO;
import ch.vd.vuta.model.SmsModel;

public class ListSmsPage extends DbTransactionPage {

	protected static Logger LOGGER = Logger.getLogger(ListSmsPage.class);

	public ListSmsPage(ApplicationContext context) {

		super(context);

		LOGGER.debug("ListPage()");
	}
	
	public void processPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		LOGGER.debug("ListPage.showPage");
		
		PrintWriter out = resp.getWriter();

		showHeader(out, "Lancement d'un test");
		showMenu(out);

		out.write("<TABLE border=\"1\"><TR>\n");
		out.write("<TH>\n");
		out.write("ID");
		out.write("</TH><TH>\n");
		out.write("Natel");
		out.write("</TH><TH>\n");
		out.write("Date de reception");
		out.write("</TH><TH>\n");
		out.write("Numero de CTB");
		out.write("</TH><TH>\n");
		out.write("Langue");
		out.write("</TH><TH>\n");
		out.write("Operateur");
		out.write("</TH><TH>\n");
		out.write("Request UID");
		out.write("</TH><TH>\n");
		out.write("Texte");
		out.write("</TH><TH>\n");
		out.write("Status");
		out.write("</TH><TH>\n");
		out.write("Delete2");
		out.write("</TH></TR>\n");
		
		SmsDAO smsDao = (SmsDAO)getApplicationContext().getBean("smsDAO");
		Iterator<SmsModel> iter = smsDao.iterator();
		int nbMaxSmsParPage = 200;
		int count = 0;
		while (iter.hasNext() && count < nbMaxSmsParPage) {
			SmsModel sms = (SmsModel)iter.next();
			count++;
			
			//LOGGER.debug("Sms #"+sms.getId()+" Numero:"+sms.getNumeroNatel());
			
			out.write("<TR><TD>\n");
			out.write(sms.getId().toString());
			out.write("</TD><TD>\n");
			if (sms.getNumeroNatel() != null) {
				out.write(sms.getNumeroNatel());
			}
			out.write("</TD><TD>\n");
			if (sms.getDateReception() != null) {
				out.write(sms.getDateReception().toString());
			}
			out.write("</TD><TD>\n");
			if (sms.getNumeroCTB() != null) {
				out.write(sms.getNumeroCTB().toString());
			}
			out.write("</TD><TD>\n");
			if (sms.getLangue() != null) {
				out.write(sms.getLangue());
			}
			out.write("</TD><TD>\n");
			if (sms.getOperateur() != null) {
				out.write(sms.getOperateur());
			}
			out.write("</TD><TD>\n");
			if (sms.getRequestUid() != null) {
				out.write(sms.getRequestUid());
			}
			out.write("</TD><TD>\n");
			if (sms.getTexte() != null) {
				out.write(sms.getTexte());
			}
			out.write("</TD><TD>\n");
			if (sms.getStatusString() != null) {
				out.write(sms.getStatusString());
			}
			out.write("</TD><TD>\n");
			out.write("<a href=\"delete.do?id="+sms.getId().toString()+"\">X</a>");
			out.write("</TD></TR>\n");
		}
		out.write("</TABLE>\n");
		showFooter(out);
		
		LOGGER.debug("Fin de showPage");
	}

}
