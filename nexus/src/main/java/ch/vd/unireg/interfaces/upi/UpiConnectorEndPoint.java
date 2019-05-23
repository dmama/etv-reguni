package ch.vd.unireg.interfaces.upi;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

public class UpiConnectorEndPoint implements UpiConnector, DetailedLoadMonitorable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpiConnectorEndPoint.class);

	private UpiConnector target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(UpiConnector target) {
		this.target = target;
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws UpiConnectorException {
		loadMeter.start(new MethodCallDescriptor("getPersonInfo", "noAvs13", noAvs13));
		try {
			return target.getPersonInfo(noAvs13);
		}
		catch (RuntimeException e) {
			final String msg = getMessage(e);
			LOGGER.error("Exception dans getPersonInfo(noAvs13=" + noAvs13 + ") : " + msg, e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new UpiConnectorException(msg);
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
