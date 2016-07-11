package ch.vd.unireg.interfaces.upi.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public abstract class MockServiceUpi implements ServiceUpiRaw {

	private final Map<String, String> map = new HashMap<>();

	protected abstract void init();

	protected final void addReplacement(String oldAvs, String newAvs) {
		if (StringUtils.isBlank(oldAvs) || StringUtils.isBlank(newAvs)) {
			throw new IllegalArgumentException("Non-blank values required");
		}
		if (oldAvs.equals(newAvs)) {
			// useless mapping
			throw new IllegalArgumentException("Useless mapping : " + oldAvs);
		}
		map.put(oldAvs, newAvs);
	}

	protected final void addUnknown(String avs) {
		if (StringUtils.isBlank(avs)) {
			throw new IllegalArgumentException("Non-blank value required");
		}
		map.put(avs, StringUtils.EMPTY);
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws ServiceUpiException {
		final String other = map.get(noAvs13);
		if (other != null) {
			if (StringUtils.isBlank(other)) {
				// numéro complètement annulé, ou inconnu
				return null;
			}
			else {
				// numéro remplacé pas un autre
				return new UpiPersonInfo(other);
			}
		}
		else {
			// pas mieux -> on renvoie le même numéro AVS
			return new UpiPersonInfo(noAvs13);
		}
	}
}
