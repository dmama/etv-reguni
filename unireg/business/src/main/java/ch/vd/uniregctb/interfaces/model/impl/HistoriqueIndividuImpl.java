package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;

public class HistoriqueIndividuImpl implements HistoriqueIndividu, Serializable {

	private static final long serialVersionUID = 3515685965456615515L;
	
	private final RegDate dateDebut;
	private final String autresPrenoms;
	private final String noAVS;
	private final String nom;
	private final String nomNaissance;
	private final String prenom;

	public static HistoriqueIndividuImpl get(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		if (target == null) {
			return null;
		}
		return new HistoriqueIndividuImpl(target);
	}

	private HistoriqueIndividuImpl(ch.vd.registre.civil.model.HistoriqueIndividu target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.autresPrenoms = target.getAutresPrenoms();
		this.noAVS = target.getNoAVS();
		this.nom = target.getNom();
		this.nomNaissance = target.getNomNaissance();
		this.prenom = target.getPrenom();
	}

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
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
