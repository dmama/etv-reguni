package ch.vd.unireg.interfaces.upi.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.upi.UpiConnector;
import ch.vd.unireg.interfaces.upi.UpiConnectorException;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;

public abstract class MockUpiConnector implements UpiConnector {

	private final Map<String, String> avsReplacementMap = new HashMap<>();
	private final Map<String, UpiPersonInfo> data = new HashMap<>();

	protected abstract void init();

	protected final void addReplacement(String oldAvs, String newAvs) {
		if (StringUtils.isBlank(oldAvs) || StringUtils.isBlank(newAvs)) {
			throw new IllegalArgumentException("Non-blank values required");
		}
		if (oldAvs.equals(newAvs)) {
			// useless mapping
			throw new IllegalArgumentException("Useless mapping : " + oldAvs);
		}
		avsReplacementMap.put(oldAvs, newAvs);
	}

	protected final void addData(@NotNull UpiPersonInfo info) {
		if (StringUtils.isBlank(info.getNoAvs13())) {
			throw new IllegalArgumentException("NAVS13 obligatoire !");
		}
		data.put(info.getNoAvs13(), info);
	}

	@Override
	public UpiPersonInfo getPersonInfo(String noAvs13) throws UpiConnectorException {
		final String other = avsReplacementMap.get(noAvs13);
		if (other != null) {
			if (StringUtils.isBlank(other)) {
				// numéro complètement annulé, ou inconnu
				return null;
			}
			else {
				// numéro remplacé pas un autre
				return data.get(other);
			}
		}
		else {
			// pas mieux -> on renvoie le même numéro AVS
			return data.get(noAvs13);
		}
	}
}
