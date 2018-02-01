package ch.vd.unireg.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;

public class GentilEsbMessageListenerContainerWithMasterSwitch extends GentilEsbMessageListenerContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GentilEsbMessageListenerContainerWithMasterSwitch.class);

	private boolean masterSwitch;

	public void setMasterSwitch(boolean masterSwitch) {
		this.masterSwitch = masterSwitch;
	}

	@Override
	public void start() throws JmsException {
		if (masterSwitch) {
			super.start();
		}
		else {
			LOGGER.warn(String.format("Ecoute sur la queue '%s' non-démarrée pour cause de switch principal coupé.", getDestinationName()));
		}
	}
}
