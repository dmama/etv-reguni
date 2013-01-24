package ch.vd.uniregctb.identification.individus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.evd0001.v3.FoundPerson;
import ch.vd.evd0001.v3.ListOfFoundPersons;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;

public class IdentificationParPrenomNomEtDateNaissance implements StrategieIdentification {

	private RcPersClient rcPersClient;

	public IdentificationParPrenomNomEtDateNaissance(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	@Override
	public String getNom() {
		return "Prénom, nom et date de naissance";
	}

	@Override
	public List<Long> identifieIndividuRcPers(Individu individu) {
		final RegDate dateNaissance = individu.getDateNaissance();
		if (dateNaissance == null) {
			return Collections.emptyList();
		}
		final String prenom = individu.getPrenom();
		final String nom = IdentificationHelper.removePonctuation(IdentificationHelper.removeDoublonSuffixe(individu.getNom()));
		return findPersons(prenom, nom, dateNaissance);
	}

	private List<Long> findPersons(String prenom, String nom, RegDate dateNaissance) {
		final ListOfFoundPersons l = rcPersClient.findPersons(null, prenom, nom, null, null, null, null, null, null, null, null, null, null, null, null, null, dateNaissance, null);
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
