package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Individu;

public class AdoptionReconnaissanceImpl implements AdoptionReconnaissance, Serializable {

	private static final long serialVersionUID = -4845025882379646564L;

	private final Individu adopte;
	private final RegDate dateAccueil;
	private final RegDate dateAdoption;
	private final RegDate dateDesaveu;
	private final RegDate dateReconnaissance;

	public static AdoptionReconnaissanceImpl get(ch.vd.registre.civil.model.AdoptionReconnaissance target) {
		if (target == null) {
			return null;
		}
		return new AdoptionReconnaissanceImpl(target);
	}

	private AdoptionReconnaissanceImpl(ch.vd.registre.civil.model.AdoptionReconnaissance target) {
		this.adopte = IndividuImpl.get(target.getAdopteReconnu());
		this.dateAccueil = RegDate.get(target.getDateAccueilAdoption());
		this.dateAdoption = RegDate.get(target.getDateAdoption());
		this.dateDesaveu = RegDate.get(target.getDateDesaveu());
		this.dateReconnaissance = RegDate.get(target.getDateReconnaissance());
	}

	public Individu getAdopteReconnu() {
		return adopte;
	}

	public RegDate getDateAccueilAdoption() {
		return dateAccueil;
	}

	public RegDate getDateAdoption() {
		return dateAdoption;
	}

	public RegDate getDateDesaveu() {
		return dateDesaveu;
	}

	public RegDate getDateReconnaissance() {
		return dateReconnaissance;
	}
}
