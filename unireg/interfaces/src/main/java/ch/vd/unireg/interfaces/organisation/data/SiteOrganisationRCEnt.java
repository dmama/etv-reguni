package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.

 */
public class SiteOrganisationRCEnt implements Serializable, SiteOrganisation {

	private static final long serialVersionUID = 4000453604399268480L;

	/**
	 * Le numéro technique du site pour Unireg
	 */
	private final long numeroSite;

	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> numeroIDE;

	private final List<DateRanged<String>> nomAdditionnel;
	private final List<DateRanged<TypeDeSite>> typeDeSite;
	private final List<DateRanged<FormeLegale>> formeLegale;

	private final List<Domicile> domicile;
	private final Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	public final DonneesRC rc;
	public final DonneesRegistreIDE ide;

	private final Map<RegDate, List<PublicationBusiness>> publications;

	private final List<DateRanged<Long>> ideRemplacePar;
	private final List<DateRanged<Long>> ideEnRemplacementDe;

	private final List<DateRanged<Long>> burTransfereA;
	private final List<DateRanged<Long>> burTransferDe;

	public SiteOrganisationRCEnt(long numeroSite,
	                             Map<String, List<DateRanged<String>>> autresIdentifiants,
	                             List<DateRanged<String>> nom,
	                             List<DateRanged<String>> nomAdditionnel,
	                             List<DateRanged<TypeDeSite>> typeDeSite,
	                             List<DateRanged<FormeLegale>> formeLegale,
	                             List<Domicile> domicile,
	                             Map<String, List<DateRanged<FonctionOrganisation>>> fonction,
	                             DonneesRC rc,
	                             DonneesRegistreIDE ide,
	                             Map<RegDate, List<PublicationBusiness>> publications,
	                             List<DateRanged<Long>> ideRemplacePar,
	                             List<DateRanged<Long>> ideEnRemplacementDe,
	                             List<DateRanged<Long>> burTransfereA,
	                             List<DateRanged<Long>> burTransferDe) {
		this.numeroSite = numeroSite;
		this.nomAdditionnel = nomAdditionnel;
		this.formeLegale = formeLegale;
		this.ideRemplacePar = ideRemplacePar;
		this.ideEnRemplacementDe = ideEnRemplacementDe;
		this.burTransfereA = burTransfereA;
		this.burTransferDe = burTransferDe;
		this.numeroIDE = OrganisationHelper.extractIdentifiant(autresIdentifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.rc = rc;
		this.publications = publications;
		this.ide = ide;
		this.typeDeSite = typeDeSite;
		this.domicile = domicile;
		this.fonction = fonction;
	}

	@Override
	public long getNumeroSite() {
		return numeroSite;
	}

	public Map<String, List<DateRanged<FonctionOrganisation>>> getFonction() {
		return fonction;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return numeroIDE;
	}

	public DonneesRegistreIDE getDonneesRegistreIDE() {
		return ide;
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	@Override
	public String getNom(RegDate date) {
		return OrganisationHelper.valueForDate(getNom(), date);
	}

	public List<DateRanged<String>> getNomAdditionnel() {
		return nomAdditionnel;
	}

	public String getNomAdditionnel(RegDate date) {
		return OrganisationHelper.valueForDate(nomAdditionnel, date);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return OrganisationHelper.valueForDate(formeLegale, date);
	}

	public DonneesRC getDonneesRC() {
		return rc;
	}

	public List<Domicile> getDomiciles() {
		return domicile;
	}

	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return typeDeSite;
	}

	@Override
	public Domicile getDomicile(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getDomiciles(), date);
	}

	@Override
	public RegDate getDateInscriptionRC(RegDate date) {
		return OrganisationHelper.valueForDate(this.getDonneesRC().getDateInscription(), date);
	}

	@Override
	public RegDate getDateInscriptionRCVd(RegDate date) {
		return OrganisationHelper.valueForDate(this.getDonneesRC().getDateInscriptionVd(), date);
	}

	@Override
	public List<Adresse> getAdresses() {
		return OrganisationHelper.getAdressesPourSite(this);
	}

	@Override
	public List<DateRanged<Long>> getIdeRemplacePar() {
		return ideRemplacePar;
	}

	@Override
	public Long getIdeRemplacePar(RegDate date) {
		return OrganisationHelper.valueForDate(ideRemplacePar, date);
	}

	@Override
	public List<DateRanged<Long>> getIdeEnRemplacementDe() {
		return ideEnRemplacementDe;
	}

	@Override
	public Long getIdeEnRemplacementDe(RegDate date) {
		return OrganisationHelper.valueForDate(ideEnRemplacementDe, date);
	}

	public List<DateRanged<Long>> getBurTransferDe() {
		return burTransferDe;
	}

	public Long getBurTransferDe(RegDate date) {
		return OrganisationHelper.valueForDate(burTransferDe, date);
	}

	public List<DateRanged<Long>> getBurTransfereA() {
		return burTransfereA;
	}

	public Long getBurTransfereA(RegDate date) {
		return OrganisationHelper.valueForDate(burTransfereA, date);
	}

	@Override
	public Map<RegDate, List<PublicationBusiness>>  getPublications() {
		return publications;
	}

	@Override
	public List<PublicationBusiness>  getPublications(RegDate date) {
		return publications.get(date);
	}

	/**
	 * Indique si un l'organisation est inscrite au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscritAuRC(RegDate date) {
		return OrganisationHelper.isInscritAuRC(this, date);
	}

	/**
	 * Indique si un l'organisation est radiée au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieDuRC(RegDate date) {
		return OrganisationHelper.isRadieDuRC(this, date);
	}

	/**
	 * Indique si un l'organisation est radiée de l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieIDE(RegDate date) {
		return OrganisationHelper.isRadieIDE(this, date);
	}
}
