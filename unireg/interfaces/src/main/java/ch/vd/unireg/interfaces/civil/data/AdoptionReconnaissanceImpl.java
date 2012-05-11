package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class AdoptionReconnaissanceImpl implements AdoptionReconnaissance, Serializable {

	private static final long serialVersionUID = -4845025882379646564L;

	private final Individu adopte;
	private final RegDate dateAccueil;
	private final RegDate dateAdoption;
	private final RegDate dateDesaveu;
	private final RegDate dateReconnaissance;

	public static AdoptionReconnaissanceImpl get(ch.vd.registre.civil.model.AdoptionReconnaissance target, RegDate upTo) {
		if (target == null) {
			return null;
		}
		return new AdoptionReconnaissanceImpl(target, upTo);
	}

	private AdoptionReconnaissanceImpl(ch.vd.registre.civil.model.AdoptionReconnaissance target, RegDate upTo) {
		this.adopte = IndividuImpl.get(target.getAdopteReconnu(), upTo);
		this.dateAccueil = RegDate.get(target.getDateAccueilAdoption());
		this.dateAdoption = RegDate.get(target.getDateAdoption());
		this.dateDesaveu = RegDate.get(target.getDateDesaveu());
		this.dateReconnaissance = RegDate.get(target.getDateReconnaissance());
	}

	@Override
	public Individu getAdopteReconnu() {
		return adopte;
	}

	@Override
	public RegDate getDateAccueilAdoption() {
		return dateAccueil;
	}

	@Override
	public RegDate getDateAdoption() {
		return dateAdoption;
	}

	@Override
	public RegDate getDateDesaveu() {
		return dateDesaveu;
	}

	@Override
	public RegDate getDateReconnaissance() {
		return dateReconnaissance;
	}
}
