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

public final class AdresseCourrierMinimale implements Adresse, Serializable {

	private static final long serialVersionUID = 4476888448775996725L;

	private final DateRange range;
	private final Integer noOfsCommune;
	private final Integer noOfsPays;

	public AdresseCourrierMinimale(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable Integer noOfsCommune, @Nullable Integer noOfsPays) {
		this.range = new DateRangeHelper.Range(dateDebut, dateFin);
		this.noOfsCommune = noOfsCommune;
		this.noOfsPays = noOfsPays;
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
	public TypeAdresseCivil getTypeAdresse() {
		return TypeAdresseCivil.COURRIER;
	}

	@Override
	public Integer getNoOfsPays() {
		return noOfsPays;
	}

	@Override
	public String getLocalite() {
		return null;
	}

	@Override
	public CasePostale getCasePostale() {
		return null;
	}

	@Override
	public String getNumero() {
		return null;
	}

	@Override
	public Integer getNumeroOrdrePostal() {
		return null;
	}

	@Override
	public String getNumeroPostal() {
		return null;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return null;
	}

	@Override
	public String getRue() {
		return null;
	}

	@Override
	public Integer getNumeroRue() {
		return null;
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public String getTitre() {
		return null;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public Integer getEwid() {
		return null;
	}

	@Override
	public Localisation getLocalisationPrecedente() {
		return null;
	}

	@Override
	public Localisation getLocalisationSuivante() {
		return null;
	}

	@Override
	public Integer getNoOfsCommuneAdresse() {
		return noOfsCommune;
	}
}
