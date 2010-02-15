package ch.vd.vuta.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import ch.vd.vuta.model.SmsDAO;
import ch.vd.vuta.model.SmsModel;

public class DeleteSmsPage extends DbTransactionPage {
	
	private static Logger LOGGER = Logger.getLogger(DeleteSmsPage.class);
	
	private SmsDAO smsDAO;
	
	public DeleteSmsPage(ApplicationContext context) {
		
		super(context);
		
		smsDAO = (SmsDAO)getApplicationContext().getBean("smsDAO");
	}
	
	@Override
	public void processPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String idStr = req.getParameter("id");
		
		String error = null;
		try {
			Long id = Long.parseLong(idStr);
			SmsModel sms = smsDAO.get(id);
			smsDAO.getHibernateTemplate().delete(sms);
			LOGGER.info("Effacement du SMS "+id+" OK");
		}
		catch (Exception e) {
			e.printStackTrace();
			error = "Problème lors de l'effacement du SMS numéro "+idStr+" : "+e.getMessage();
			LOGGER.error(error);
		}
		PrintWriter out = resp.getWriter();
		showHeader(out, "Delete sms");
		showMenu(out);
		
		out.write("<h2>SMS numéro "+idStr+" effacé avec succès</h2>");
		
		showFooter(out);
	}

}
