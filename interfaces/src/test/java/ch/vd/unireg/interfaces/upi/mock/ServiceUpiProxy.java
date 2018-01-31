package ch.vd.unireg.interfaces.upi.mock;

import org.junit.Assert;

import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public class ServiceUpiProxy implements ServiceUpiRaw {

	private ServiceUpiRaw target;

	public void setUp(MockServiceUpi target) {
		this.target = target;
		target.init();
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws ServiceUpiException {
		Assert.assertNotNull(target);
		return target.getPersonInfo(noAvs13);
	}
}
