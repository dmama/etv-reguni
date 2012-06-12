package ch.vd.uniregctb.identification.individus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;

public class IdentificationParNoAVS13 implements StrategieIdentification {

	private RcPersClient rcPersClient;

	public IdentificationParNoAVS13(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	@Override
	public String getNom() {
		return "Numéro AVS13";
	}

	@Override
	public List<Long> identifieIndividuRcPers(Individu individu) {

		final String nouveauNoAVS = individu.getNouveauNoAVS();
		if (StringUtils.isBlank(nouveauNoAVS)) {
			return Collections.emptyList();
		}

		final ListOfPersons l = rcPersClient.getPersonsBySocialsNumbers(Arrays.asList(nouveauNoAVS), null, false);
		if (l.getNumberOfResults().intValue() == 0) {
			return Collections.emptyList();
		}

		final List<Long> list = new ArrayList<Long>();
		for (ListOfPersons.ListOfResults.Result result : l.getListOfResults().getResult()) {
			if (result.getPerson() != null) {
				list.add(IndividuRCPers.getNoIndividu(result.getPerson()));
			}
		}

		return list;
	}
}
