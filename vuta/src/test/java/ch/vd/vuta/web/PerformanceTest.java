package ch.vd.vuta.web;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import ch.vd.vuta.AbstractSmsgwTestCase;
import ch.vd.vuta.processing.SmsProcessor;

public class PerformanceTest extends AbstractSmsgwTestCase {
	
	public void testFake() {
		
	}

	public void a_testPerformance() throws Exception {
		
		runSms(false);
		runSms(true);

		long begin = System.nanoTime();
		
		int nbThreads = 100;
		List<PerformanceTestThread> threads = new ArrayList<PerformanceTestThread>();
		for (int i=0;i<nbThreads;i++) {
			PerformanceTestThread thread = new PerformanceTestThread(this, true);
			thread.start();
			threads.add(thread);
			
			thread = new PerformanceTestThread(this, false);
			thread.start();
			threads.add(thread);
		}

		for (int i=0;i<threads.size();i++) {
			PerformanceTestThread thread = threads.get(i);
			thread.join();
		}

		long end = System.nanoTime();
		long millis = (end-begin)/1000000;
		LOGGER.info(nbThreads+" SMS traités en "+millis+"[ms] ("+(millis*1.0/nbThreads)+"[ms] / SMS");
	}
		
	protected void runSms(boolean numeroFaux) throws Exception {
		
		long begin = System.nanoTime();
		
		URL url = new URL("http://www.smsifd.etat-de-vaud.ch/fiscalite/smsifd"+MainServlet.RECEIVE_URL);
		URLConnection conn = url.openConnection();
		
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestProperty ("Content-Type", "text/xml");

		String natel = "+41798888732";
		String operateur = "swisscom";
		String langue = "fr";
		String requestUid = "sms382788";
		Integer ctb = 10000005;
		
		String texte = "IFD "+ctb;
		if (numeroFaux) {
			texte = "IFD bla bla ";
		}
		String message = SmsProcessor.getSmsAsXml(natel, texte, operateur, langue, requestUid);
		
		DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
	    dos.writeBytes(message);
	    dos.flush();
	    dos.close();
	    
		DataInputStream dis = new DataInputStream(conn.getInputStream());
		String resp = "";
		String line;
		while ((line = dis.readLine()) != null) {
			
			resp += line;
		}
	    
		if (numeroFaux) {
			assertTrue(resp.contains("invalide"));
		}
		else {
			assertTrue(resp.contains("mensualisation"));
			assertTrue(resp.contains(ctb.toString()));
		}
		
		long end = System.nanoTime();
		long millis = (end-begin)/1000000;
		LOGGER.info("Test terminé en "+millis+" [ms]");
	}

}
