package ch.vd.uniregctb.common;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.openqa.selenium.server.SeleniumServer;

import com.thoughtworks.selenium.SeleneseTestCase;

/**
 * Test Case Selenium
 * 
 * 
 */
//extends SeleneseTestCase {
public abstract class AbstractSeleniumTest extends SeleneseTestCase {

	private Logger LOGGER = Logger.getLogger(AbstractSeleniumTest.class);
	
	/**
	 * Une instance du server selenium instanciée si aucun server selenium n'est
	 * démarré. Utilisé pour lancer les tests unitaires depuis eclipse.
	 */
	protected SeleniumServer server;

	/**
	 * Une instance du client selenium.
	 */
	protected SeleniumClient selenium;
	
	/**
	 * Les properties externes du projet pour les tests Web-IT
	 */
	private Properties properties;

	public AbstractSeleniumTest() {
		DOMConfigurator.configure("src/test/resources/ut/log4j.xml");
	}

	/**
	 * @return l'URL utilisée comme point d'entrée pour le test ( e.g.
	 *         http://hostname.domain.com:port).
	 */
	protected String getBaseUrl() {
		return getProperty("selenium.baseurl");
	}
	
	/**
	 * @throws Exception
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp() throws Exception {
		
		// Init des props
		properties = new Properties();
		InputStream inStream = getClass().getResourceAsStream("/ut/unireg-webit.properties");
		properties.load(inStream);
		
		String autoStartServer = getProperty("selenium.autostart-server");

		// Init de selenium server
		String url = getBaseUrl();
		String host = getProperty("selenium.server.host");
		int port = Integer.parseInt(getProperty("selenium.server.port"));
		String browser = getProperty("selenium.browser");
		LOGGER.debug("BaseUrl: "+url);
		LOGGER.debug("AutoStartServer: "+autoStartServer);
		LOGGER.debug("ServerHost: "+host);
		LOGGER.debug("ServerPort: "+port);
		LOGGER.debug("Browser: "+browser);
		LOGGER.debug("Context: "+this.getClass().getSimpleName() + "." + getName());
		selenium = new SeleniumClient(host, port, browser, url);
		try {
			selenium.start();
	        selenium.setContext(this.getClass().getSimpleName() + "." + getName());
	        LOGGER.info("Demarrage du client OK");
		} catch (Exception e) {
			LOGGER.error("Impossible to start the Selenium client:");
			LOGGER.debug(e,e);
			if (autoStartServer.equals("true")) {
				LOGGER.warn("Impossible to start the Selenium client => try to start the selenium server");
				// Si le client selenium n'a pas se connecter au server, on lance un
				// serveur local et on essaie de s'y connecter.
				server = new SeleniumServer(port);
				server.start();
				LOGGER.warn("Server start OK!");
				
				try {
					// And then start the client
					LOGGER.warn("... and then try to start the selenium client");
					selenium.start();
			        selenium.setContext(this.getClass().getSimpleName() + "." + getName());
				}
				catch (Exception ee) {
					LOGGER.error(ee,ee);
				}
			}
			else {
				LOGGER.warn("... impossible to start the Selenium client");
			}
		}
		
		//super.setUp();
	}
	
	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	public void tearDown() throws Exception {
		selenium.stop();
		if (server != null) {
			server.stop();
		}
		
		//super.tearDown();
	}

	protected void clickAndWait(String link) {
		selenium.click(link);
		selenium.waitForPageToLoad("30000");
	}
	
    public void verifyTrue(boolean b) {
    	
    	assertTrue(b);
    }

    public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
}
