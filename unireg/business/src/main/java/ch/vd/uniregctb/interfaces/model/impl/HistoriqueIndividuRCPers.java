package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.evd0001.v2.Identity;
import ch.vd.evd0001.v2.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuRCPers implements HistoriqueIndividu, Serializable {

	private static final long serialVersionUID = -5416345024674196983L;

	private final RegDate dateDebut;
	private final String autresPrenoms;
	private final String noAVS;
	private final String nom;
	private final String nomNaissance;
	private final String prenom;

	public HistoriqueIndividuRCPers(Person person) {
		final Identity identity = person.getIdentity();
		this.dateDebut = null; // TODO (rcpers) à renseigner lorsque l'historique sera disponible
		this.autresPrenoms = identity.getPersonIdentification().getFirstNames();
		this.noAVS = EchHelper.avs13FromEch(person.getUpiPerson().getVn());
		this.nom = identity.getPersonIdentification().getOfficialName();
		this.nomNaissance = identity.getOriginalName();
		this.prenom = identity.getCallName();
	}

	public static HistoriqueIndividu get(Person person) {
		if (person == null) {
		return null;
		}
		return new HistoriqueIndividuRCPers(person);
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	@Override
	public String getNoAVS() {
		return noAVS;
	}

	@Override
	public String getNom() {
		return nom;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	@Override
	public String getPrenom() {
		return prenom;
	}
}
