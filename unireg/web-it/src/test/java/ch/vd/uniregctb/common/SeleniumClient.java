package ch.vd.uniregctb.common;

import com.thoughtworks.selenium.DefaultSelenium;


public class SeleniumClient extends DefaultSelenium {

    public SeleniumClient(String serverHost, int serverPort, String browserStartCommand, String browserURL) {
    	
    	super(serverHost, serverPort, browserStartCommand, browserURL);
    }

    public void clickAndWait(String url) {
    	click(url);
    	waitForPageToLoad("30000");
    }
    
}
