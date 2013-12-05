package ch.vd.unireg.interfaces.upi;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.load.MethodCallDescriptor;

public class ServiceUpiEndPoint implements ServiceUpiRaw, DetailedLoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(ServiceUpiEndPoint.class);

	private ServiceUpiRaw target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(ServiceUpiRaw target) {
		this.target = target;
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws ServiceUpiException {
		loadMeter.start(new MethodCallDescriptor("getPersonInfo", "noAvs13", noAvs13));
		try {
			return target.getPersonInfo(noAvs13);
		}
		catch (RuntimeException e) {
			final String msg = getMessage(e);
			LOGGER.error("Exception dans getPersonInfo(noAvs13=" + noAvs13 + ") : " + msg, e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceUpiException(msg);
		}
		finally {
			loadMeter.end();
		}
	}

	private static String getMessage(RuntimeException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.getClass().getSimpleName();
		}
		return message;
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}
}
