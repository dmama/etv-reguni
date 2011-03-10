package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Adapter qui permet de spécifier de nouvelles dates de début et de fin sur une adresse civile.
 */
public class AdresseAdapter implements Adresse {

	private Adresse target;
	private RegDate debut;
	private RegDate fin;

	public AdresseAdapter(Adresse target, RegDate debut, RegDate fin) {
		this.target = target;
		this.debut = debut;
		this.fin = fin;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
	}

	public String getCasePostale() {
		return target.getCasePostale();
	}

	public RegDate getDateDebut() {
		return debut;
	}

	public RegDate getDateFin() {
		return fin;
	}

	public String getLocalite() {
		return target.getLocalite();
	}

	public String getNumero() {
		return target.getNumero();
	}

	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	public Integer getNoOfsPays() {
		return target.getNoOfsPays();
	}

	public String getRue() {
		return target.getRue();
	}

	public Integer getNumeroRue() {
		return target.getNumeroRue();
	}

	public String getNumeroAppartement() {
		return target.getNumeroAppartement();
	}

	public String getTitre() {
		return target.getTitre();
	}

	public TypeAdresseCivil getTypeAdresse() {
		return target.getTypeAdresse();
	}

	public CommuneSimple getCommuneAdresse() {
		return target.getCommuneAdresse();
	}

	@Override
	public Long getEgid() {
		return target.getEgid();
	}

	@Override
	public Long getEwid() {
		return target.getEwid();
	}
}
