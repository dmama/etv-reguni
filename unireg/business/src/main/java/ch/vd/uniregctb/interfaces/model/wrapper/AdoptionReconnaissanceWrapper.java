package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Individu;

public class AdoptionReconnaissanceWrapper implements AdoptionReconnaissance {

	private final ch.vd.registre.civil.model.AdoptionReconnaissance target;
	private Individu adopte;
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
		this.target = target;
		dateAccueil = RegDate.get(target.getDateAccueilAdoption());
		dateAdoption = RegDate.get(target.getDateAdoption());
		dateDesaveu = RegDate.get(target.getDateDesaveu());
		dateReconnaissance = RegDate.get(target.getDateReconnaissance());
	}

	public Individu getAdopteReconnu() {
		if (adopte == null) {
			adopte = IndividuWrapper.get(target.getAdopteReconnu());
		}
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
