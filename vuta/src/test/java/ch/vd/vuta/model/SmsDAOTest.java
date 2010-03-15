package ch.vd.vuta.model;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import ch.vd.vuta.AbstractSmsgwTestCase;

public class SmsDAOTest extends AbstractSmsgwTestCase {

	private SmsDAO smsDAO;

	public SmsDAOTest() {
	}

	public void onSetUp() {
		super.onSetUp();
		
		smsDAO = (SmsDAO)applicationContext.getBean("smsDAO");
	}

	public void testSave() {
		
		String langue = "de";
		Integer noCtb = 123465321;
		String noNatel = "+41764323356";
		String operateur = "swisscom";
		String requestUid = "smsUid-6576342";
		String texte = "Bla";
		Date receptDate = new Date();
		String texteComplet = "Le sms complet en XML";
		
		SmsModel sms = new SmsModel();
		sms.setLangue(langue);
		sms.setNumeroCTB(noCtb);
		sms.setNumeroNatel(noNatel);
		sms.setOperateur(operateur);
		sms.setRequestUid(requestUid);
		sms.setTexte(texte);
		sms.setDateReception(receptDate);
		sms.setSmsComplet(texteComplet);
		sms.setStatus(new Integer(3)); // Pas persisté
		sms.setStatusString("BlaBla");
		
		smsDAO.save(sms);
		
		setComplete();
		endTransaction();
		startNewTransaction();
		
		List<SmsModel> list = smsDAO.getAll();
		assertEquals(1, list.size());
		sms = list.get(0);
		assertEquals(langue, sms.getLangue());
		assertEquals(noCtb, sms.getNumeroCTB());
		assertEquals(noNatel, sms.getNumeroNatel());
		assertEquals(operateur, sms.getOperateur());
		assertEquals(requestUid, sms.getRequestUid());
		assertEquals(texte, sms.getTexte());
		assertEquals(receptDate, sms.getDateReception());
		assertEquals(texteComplet, sms.getSmsComplet());
		assertNull(sms.getStatus());
		assertEquals("BlaBla", sms.getStatusString());
	}

	/**
	 * Teste que l'ordre d'iteration est basé sur la date de reception
	 */
	public void testIterateOrder() {

		insertSms(2007, 6, 22, 12, 34, 55);
		insertSms(2007, 5, 22, 12, 34, 57);
		insertSms(2007, 4, 23, 12, 34, 51);
		insertSms(2007, 6, 22, 12, 34, 48);
		insertSms(2006, 5, 22, 12, 34, 52);
		insertSms(2007, 2, 23, 12, 34, 51);

		Iterator<SmsModel> iter = smsDAO.iterator();
		Date lastDate = null;
		while (iter.hasNext()) {
			SmsModel sms = iter.next();
			
			LOGGER.info("SMS date: "+sms.getDateReception());
			assertTrue(lastDate == null || lastDate.after(sms.getDateReception()));
			
			lastDate = sms.getDateReception();
		}
	}

	private void insertSms(int y, int m, int d, int h, int min, int s) {
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.MONTH, m);
		cal.set(Calendar.DAY_OF_MONTH, d);
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, s);
		Date date = cal.getTime();
		
		SmsModel sms = new SmsModel();
		sms.setDateReception(date);

		sms.setOperateur("swisscom");
		sms.setTexte("10282938");
		sms.setSmsComplet("Bla");
		sms.setNumeroNatel("+417912345678");
		sms.setLangue("fr");
		sms.setRequestUid("requestUid"+(int)(Math.random()*1000));
		
		smsDAO.save(sms);
		
		setComplete();
		endTransaction();
		startNewTransaction();
	}
	
}
