package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseServiceImpl;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseCivilView implements DateRange {

	private TypeAdresseCivil usageCivil;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String complements;
	private String rue;
	private String casePostale;
	private String localite;
	private Integer paysOFS;
	private Integer egid;
	private Integer ewid;
	private LocalisationView localisationPrecedente;
	private LocalisationView localisationSuivante;

	public AdresseCivilView(Adresse adresse, TypeAdresseCivil type) {
		this.usageCivil = type;
		this.dateDebut = adresse.getDateDebut();
		this.dateFin = adresse.getDateFin();
		this.complements = AdresseCivileAdapter.extractComplement(adresse);
		this.rue = extractRue(adresse);

		final CasePostale casePostale = adresse.getCasePostale();
		this.casePostale = casePostale != null ? casePostale.toString() : null;

		this.localite = extractLocalite(adresse);
		this.paysOFS = adresse.getNoOfsPays();
		this.egid = adresse.getEgid();
		this.ewid = adresse.getEwid();
		this.localisationPrecedente = extractLocalisation(adresse.getLocalisationPrecedente());
		this.localisationSuivante = extractLocalisation(adresse.getLocalisationSuivante());
	}

	private static LocalisationView extractLocalisation(Localisation localisation) {
		return localisation == null ? null : new LocalisationView(localisation);
	}

	private static String extractLocalite(Adresse adresse) {
		final NpaEtLocalite npaEtLocalite = AdresseServiceImpl.buildNpaEtLocalite(adresse);
		return npaEtLocalite == null ? null : npaEtLocalite.toString();
	}

	private static String extractRue(Adresse adresse) {
		final RueEtNumero rueEtNumero = AdresseServiceImpl.buildRueEtNumero(adresse);
		return rueEtNumero == null ? null : rueEtNumero.getRueEtNumero();
	}

	public TypeAdresseCivil getUsageCivil() {
		return usageCivil;
	}

	public void setUsageCivil(TypeAdresseCivil usageCivil) {
		this.usageCivil = usageCivil;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getComplements() {
		return complements;
	}

	public void setComplements(String complements) {
		this.complements = complements;
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public String getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(String casePostale) {
		this.casePostale = casePostale;
	}

	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public Integer getPaysOFS() {
		return paysOFS;
	}

	public void setPaysOFS(Integer paysOFS) {
		this.paysOFS = paysOFS;
	}

	public LocalisationView getLocalisationPrecedente() {
		return localisationPrecedente;
	}

	public LocalisationView getLocalisationSuivante() {
		return localisationSuivante;
	}

	public Integer getEgid() {
		return egid;
	}

	public Integer getEwid() {
		return ewid;
	}
}
