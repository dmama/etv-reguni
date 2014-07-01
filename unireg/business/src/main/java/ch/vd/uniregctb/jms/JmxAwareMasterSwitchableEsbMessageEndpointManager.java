package ch.vd.uniregctb.jms;

import org.apache.log4j.Logger;

public class JmxAwareMasterSwitchableEsbMessageEndpointManager extends JmxAwareEsbMessageEndpointManager {

	private static final Logger LOGGER = Logger.getLogger(JmxAwareMasterSwitchableEsbMessageEndpointManager.class);

	private boolean masterSwitch;

	public void setMasterSwitch(boolean masterSwitch) {
		this.masterSwitch = masterSwitch;
	}

	@Override
	public void start() {
		if (masterSwitch) {
			super.start();
		}
		else {
			LOGGER.warn(String.format("Ecoute sur la queue '%s' non-démarrée pour cause de switch principal coupé.", getDestinationName()));
		}
	}
}
