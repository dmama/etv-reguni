package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   EntrepriseHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.

 */
public class EtablissementCivilRCEnt implements Serializable, EtablissementCivil {

	private static final long serialVersionUID = 7428788715221796719L;

	/**
	 * Le numéro technique de l'établissement civil pour Unireg
	 */
	private final long numeroEtablissement;

	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> numeroIDE;
	private final List<DateRanged<String>> numeroRC;

	private final List<DateRanged<String>> nomAdditionnel;
	private final List<DateRanged<TypeEtablissementCivil>> typeEtablissement;
	private final List<DateRanged<FormeLegale>> formeLegale;

	private final List<Domicile> domicile;
	private final Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	public final DonneesRC rc;
	public final DonneesRegistreIDE ide;
	public final DonneesREE ree;

	private final List<PublicationBusiness> publications;

	private final List<DateRanged<Long>> ideRemplacePar;
	private final List<DateRanged<Long>> ideEnRemplacementDe;

	private final List<DateRanged<Long>> burTransfereA;
	private final List<DateRanged<Long>> burTransferDe;

	public EtablissementCivilRCEnt(long numeroEtablissement,
	                               Map<String, List<DateRanged<String>>> autresIdentifiants,
	                               List<DateRanged<String>> nom,
	                               List<DateRanged<String>> nomAdditionnel,
	                               List<DateRanged<TypeEtablissementCivil>> typeEtablissement,
	                               List<DateRanged<FormeLegale>> formeLegale,
	                               List<Domicile> domicile,
	                               Map<String, List<DateRanged<FonctionOrganisation>>> fonction,
	                               @NotNull DonneesRC rc,
	                               @NotNull DonneesRegistreIDE ide,
	                               @NotNull DonneesREE ree,
	                               List<PublicationBusiness> publications,
	                               List<DateRanged<Long>> ideRemplacePar,
	                               List<DateRanged<Long>> ideEnRemplacementDe,
	                               List<DateRanged<Long>> burTransfereA,
	                               List<DateRanged<Long>> burTransferDe) {
		this.numeroEtablissement = numeroEtablissement;
		this.nomAdditionnel = nomAdditionnel;
		this.formeLegale = formeLegale;
		this.ideRemplacePar = ideRemplacePar;
		this.ideEnRemplacementDe = ideEnRemplacementDe;
		this.burTransfereA = burTransfereA;
		this.burTransferDe = burTransferDe;
		this.numeroIDE = EntrepriseHelper.extractIdentifiant(autresIdentifiants, EntrepriseConstants.CLE_IDE);
		this.numeroRC = EntrepriseHelper.extractIdentifiant(autresIdentifiants, EntrepriseConstants.CLE_RC);
		this.nom = nom;
		this.rc = rc;
		this.ree = ree;
		this.publications = publications;
		this.ide = ide;
		this.typeEtablissement = typeEtablissement;
		this.domicile = domicile;
		this.fonction = fonction;
		if (this.rc == null) {
			throw new IllegalArgumentException();
		}
		if (this.ide == null) {
			throw new IllegalArgumentException();
		}
		if (this.ree == null) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public long getNumeroEtablissement() {
		return numeroEtablissement;
	}

	@Override
	public Map<String, List<DateRanged<FonctionOrganisation>>> getFonction() {
		return fonction;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return numeroIDE;
	}

	@Override
	public String getNumeroIDE(RegDate date) {
		return EntrepriseHelper.valueForDate(numeroIDE, date);
	}

	@Override
	public List<DateRanged<String>> getNumeroRC() {
		return numeroRC;
	}

	@Override
	public DonneesRegistreIDE getDonneesRegistreIDE() {
		return ide;
	}

	@Override
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	@Override
	public String getNom(RegDate date) {
		return EntrepriseHelper.valueForDate(getNom(), date);
	}

	@Override
	public List<DateRanged<String>> getNomAdditionnel() {
		return nomAdditionnel;
	}

	@Override
	public String getNomAdditionnel(RegDate date) {
		return EntrepriseHelper.valueForDate(nomAdditionnel, date);
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@Override
	public FormeLegale getFormeLegale(RegDate date) {
		return EntrepriseHelper.valueForDate(formeLegale, date);
	}

	@Override
	public DonneesRC getDonneesRC() {
		return rc;
	}

	@Override
	public List<Domicile> getDomiciles() {
		return domicile;
	}

	@Override
	public List<Domicile> getDomicilesEnActivite() {
		return EntrepriseHelper.getDomicilesReels(this, domicile);
	}

	@Override
	public List<DateRanged<TypeEtablissementCivil>> getTypesEtablissement() {
		return typeEtablissement;
	}

	@Override
	public TypeEtablissementCivil getTypeEtablissement(RegDate date) {
		return EntrepriseHelper.valueForDate(getTypesEtablissement(), date);
	}

	@Override
	public Domicile getDomicile(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(getDomiciles(), date);
	}

	@Override
	public boolean isSuccursale(RegDate date) {
		return EntrepriseHelper.isSuccursale(this, date);
	}

	@Override
	public RegDate getDateInscriptionRC(RegDate date) {
		final InscriptionRC inscription = EntrepriseHelper.valueForDate(this.getDonneesRC().getInscription(), date);
		return inscription != null ? inscription.getDateInscriptionCH() : null;
	}

	@Override
	public RegDate getDateInscriptionRCVd(RegDate date) {
		final InscriptionRC inscription = EntrepriseHelper.valueForDate(this.getDonneesRC().getInscription(), date);
		return inscription != null ? inscription.getDateInscriptionVD() : null;
	}

	@Override
	public RegDate getDateRadiationRC(RegDate date) {
		final InscriptionRC inscription = EntrepriseHelper.valueForDate(this.getDonneesRC().getInscription(), date);
		return inscription != null ? inscription.getDateRadiationCH() : null;
	}

	@Override
	public RegDate getDateRadiationRCVd(RegDate date) {
		final InscriptionRC inscription = EntrepriseHelper.valueForDate(this.getDonneesRC().getInscription(), date);
		return inscription != null ? inscription.getDateRadiationVD() : null;
	}

	@Override
	public List<Adresse> getAdresses() {
		return EntrepriseHelper.getAdressesPourEtablissement(this);
	}

	@Override
	public List<DateRanged<Long>> getIdeRemplacePar() {
		return ideRemplacePar;
	}

	@Override
	public Long getIdeRemplacePar(RegDate date) {
		return EntrepriseHelper.valueForDate(ideRemplacePar, date);
	}

	@Override
	public List<DateRanged<Long>> getIdeEnRemplacementDe() {
		return ideEnRemplacementDe;
	}

	@Override
	public Long getIdeEnRemplacementDe(RegDate date) {
		return EntrepriseHelper.valueForDate(ideEnRemplacementDe, date);
	}

	public List<DateRanged<Long>> getBurTransferDe() {
		return burTransferDe;
	}

	public Long getBurTransferDe(RegDate date) {
		return EntrepriseHelper.valueForDate(burTransferDe, date);
	}

	public List<DateRanged<Long>> getBurTransfereA() {
		return burTransfereA;
	}

	public Long getBurTransfereA(RegDate date) {
		return EntrepriseHelper.valueForDate(burTransfereA, date);
	}

	@Override
	public List<PublicationBusiness>  getPublications() {
		return publications;
	}

	@Override
	public List<PublicationBusiness>  getPublications(RegDate date) {
		return EntrepriseHelper.getPublications(this.getPublications(), date);
	}

	@Override
	public RegDate connuAuCivilDepuis() {
		return EntrepriseHelper.connuAuCivilDepuis(this);
	}

	/**
	 * Indique si un l'entreprise est inscrite au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isInscritAuRC(RegDate date) {
		return EntrepriseHelper.isInscritAuRC(this, date);
	}

	@Override
	public boolean isConnuInscritAuRC(RegDate date) {
		return EntrepriseHelper.isConnuInscritAuRC(this, date);
	}

	@Override
	public boolean isActif(RegDate date) {
		return EntrepriseActiviteHelper.isActif(this, date);
	}

	/**
	 * Indique si un l'entreprise est radiée au RC à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieDuRC(RegDate date) {
		return EntrepriseHelper.isRadieDuRC(this, date);
	}

	/**
	 * Indique si un l'entreprise est radiée de l'IDE à la date indiquée. Si la date est nulle, la date du jour est utilisée.
	 */
	@Override
	public boolean isRadieIDE(RegDate date) {
		return EntrepriseHelper.isRadieIDE(this, date);
	}

	@Override
	public DonneesREE getDonneesREE() {
		return ree;
	}
}
