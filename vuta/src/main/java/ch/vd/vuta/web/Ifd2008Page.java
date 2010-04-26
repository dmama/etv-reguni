package ch.vd.vuta.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ch.vd.vuta.model.SmsDAO;
import ch.vd.vuta.processing.ProcessorResponse;
import ch.vd.vuta.processing.SmsProcessor;

public class Ifd2008Page extends Page {

	protected static Logger LOGGER = Logger.getLogger(Ifd2008Page.class);

	private PlatformTransactionManager transactionManager;
	private SmsDAO smsDAO; 
	
	public Ifd2008Page(ApplicationContext context) {
		
		super(context);

		transactionManager = (PlatformTransactionManager)getApplicationContext().getBean("transactionManager");
		smsDAO = (SmsDAO)getApplicationContext().getBean("smsDAO");
	}
	
	@Override
	public void processPage(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// Decodage et logging du XML
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		String smsAsXml = "";
		String line;
		LOGGER.info("SMS recu");
		LOGGER.info("---");
		while ((line = reader.readLine()) != null) {
			smsAsXml += line;
			LOGGER.info(line);
		}
		LOGGER.info("---");

		SmsProcessor processor = new SmsProcessor(getApplicationContext());
		PrintWriter out = resp.getWriter();

		// Start de la transaction
		TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

		// Traite le SML, renvoie le chaine que l'expediteur recevra
		ProcessorResponse responseProc = processor.treatSms(smsAsXml);
		LOGGER.info("Réponse: '"+responseProc.getTexteForSender()+"'");

		// Si on est là, c'est que tout a bien été
		LOGGER.info("Commit transaction");
		transactionManager.commit(status);

		// Renvoie une reponse d'erreur dans tous les cas d'erreur
		String respAsXml = SmsProcessor.getTextAsXmlResponse(responseProc.getTexteForSender());
		LOGGER.info("Réponse envoyée: '"+respAsXml+"'");
		out.write(respAsXml);
	}

}
