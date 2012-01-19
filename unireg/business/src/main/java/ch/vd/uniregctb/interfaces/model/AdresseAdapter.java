package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Adapter qui permet de spécifier de nouvelles dates de début et de fin sur une adresse civile.
 */
public class AdresseAdapter implements Adresse {

	private final Adresse target;
	private final RegDate debut;
	private final RegDate fin;

	public AdresseAdapter(Adresse target, RegDate debut, RegDate fin) {
		this.target = target;
		this.debut = debut;
		this.fin = fin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
	}

	@Override
	public CasePostale getCasePostale() {
		return target.getCasePostale();
	}

	@Override
	public RegDate getDateDebut() {
		return debut;
	}

	@Override
	public RegDate getDateFin() {
		return fin;
	}

	@Override
	public String getLocalite() {
		return target.getLocalite();
	}

	@Override
	public String getNumero() {
		return target.getNumero();
	}

	@Override
	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	@Override
	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	@Override
	public Integer getNoOfsPays() {
		return target.getNoOfsPays();
	}

	@Override
	public String getRue() {
		return target.getRue();
	}

	@Override
	public Integer getNumeroRue() {
		return target.getNumeroRue();
	}

	@Override
	public String getNumeroAppartement() {
		return target.getNumeroAppartement();
	}

	@Override
	public String getTitre() {
		return target.getTitre();
	}

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return target.getTypeAdresse();
	}

	@Override
	public Commune getCommuneAdresse() {
		return target.getCommuneAdresse();
	}

	@Override
	public Integer getEgid() {
		return target.getEgid();
	}

	@Override
	public Integer getEwid() {
		return target.getEwid();
	}

	@Override
	public Localisation getLocalisationPrecedente() {
		return target.getLocalisationPrecedente();
	}

	@Override
	public Localisation getLocalisationSuivante() {
		return target.getLocalisationSuivante();
	}
}
