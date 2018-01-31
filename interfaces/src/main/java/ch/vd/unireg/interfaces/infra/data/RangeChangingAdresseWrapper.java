package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class RangeChangingAdresseWrapper implements Adresse, Serializable {

	private static final long serialVersionUID = 6328338353446156803L;

	private final Adresse target;
	private final DateRange range;

	public RangeChangingAdresseWrapper(Adresse target, RegDate dateDebut, RegDate dateFin) {
		this.target = target;
		this.range = new DateRangeHelper.Range(dateDebut, dateFin);
	}

	@Override
	public RegDate getDateDebut() {
		return range.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return range.getDateFin();
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return range.isValidAt(date);
	}

	@Override
	public CasePostale getCasePostale() {
		return target.getCasePostale();
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
	public Integer getNumeroOrdrePostal() {
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
	public Integer getEgid() {
		return target.getEgid();
	}

	@Override
	public Integer getEwid() {
		return target.getEwid();
	}

	@Override
	@Nullable
	public Localisation getLocalisationPrecedente() {
		return target.getLocalisationPrecedente();
	}

	@Override
	@Nullable
	public Localisation getLocalisationSuivante() {
		return target.getLocalisationSuivante();
	}

	@Override
	@Nullable
	public Integer getNoOfsCommuneAdresse() {
		return target.getNoOfsCommuneAdresse();
	}
}
