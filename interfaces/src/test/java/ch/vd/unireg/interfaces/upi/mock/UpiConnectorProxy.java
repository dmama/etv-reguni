package ch.vd.unireg.interfaces.upi.mock;

import org.junit.Assert;

import ch.vd.unireg.interfaces.upi.UpiConnector;
import ch.vd.unireg.interfaces.upi.UpiConnectorException;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public class UpiConnectorProxy implements UpiConnector {

	private UpiConnector target;

	public void setUp(MockUpiConnector target) {
		this.target = target;
		target.init();
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws UpiConnectorException {
		Assert.assertNotNull(target);
		return target.getPersonInfo(noAvs13);
	}
}
