package ch.vd.uniregctb.identification.individus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.evd0001.v3.FoundPerson;
import ch.vd.evd0001.v3.ListOfFoundPersons;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;

public class IdentificationParPrenomEtNom implements StrategieIdentification {

	private RcPersClient rcPersClient;

	public IdentificationParPrenomEtNom(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	@Override
	public String getNom() {
		return "Prénom et nom";
	}

	@Override
	public List<Long> identifieIndividuRcPers(Individu individu) {
		final String prenom = individu.getPrenom();
		final String nom = IdentificationHelper.removePonctuation(IdentificationHelper.removeDoublonSuffixe(individu.getNom()));
		return findPersons(prenom, nom);
	}

	private List<Long> findPersons(String prenom, String nom) {
		final ListOfFoundPersons l = rcPersClient.findPersons(null, prenom, nom, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
		if (l.getNumberOfResults().intValue() == 0) {
			return Collections.emptyList();
		}
		final List<Long> list = new ArrayList<Long>();
		for (FoundPerson person : l.getListOfResults().getFoundPerson()) {
			list.add(IndividuRCPers.getNoIndividu(person.getPerson()));
		}
		return list;
	}
}
