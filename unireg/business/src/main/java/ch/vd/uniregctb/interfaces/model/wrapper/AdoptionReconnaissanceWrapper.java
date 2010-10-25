package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Individu;

public class AdoptionReconnaissanceWrapper implements AdoptionReconnaissance, Serializable {

	private static final long serialVersionUID = -4845025882379646564L;

	private final Individu adopte;
	private final RegDate dateAccueil;
	private final RegDate dateAdoption;
	private final RegDate dateDesaveu;
	private final RegDate dateReconnaissance;

	public static AdoptionReconnaissanceWrapper get(ch.vd.registre.civil.model.AdoptionReconnaissance target) {
		if (target == null) {
			return null;
		}
		return new AdoptionReconnaissanceWrapper(target);
	}

	private AdoptionReconnaissanceWrapper(ch.vd.registre.civil.model.AdoptionReconnaissance target) {
		this.adopte = IndividuWrapper.get(target.getAdopteReconnu());
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
