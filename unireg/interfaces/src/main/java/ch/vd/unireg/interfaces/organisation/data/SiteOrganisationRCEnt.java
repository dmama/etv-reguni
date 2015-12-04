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

	public final DonneesRC rc;
	public final DonneesRegistreIDE ide;

	private final Map<String, List<DateRanged<String>>> nomsAdditionnels;
	private final List<DateRanged<TypeDeSite>> typeDeSite;

	private final List<Siege> siege;
	private final Map<String, List<DateRanged<FonctionOrganisation>>> fonction;

	private final List<DateRanged<Long>> remplacePar;
	private final Map<Long, List<DateRanged<Long>>> enRemplacementDe;

	public SiteOrganisationRCEnt(long numeroSite,
	                             Map<String, List<DateRanged<String>>> autresIdentifiants,
	                             List<DateRanged<String>> nom,
	                             DonneesRC rc,
	                             DonneesRegistreIDE ide,
	                             Map<String, List<DateRanged<String>>> nomsAdditionnels,
	                             List<DateRanged<TypeDeSite>> typeDeSite, List<Siege> siege,
	                             Map<String, List<DateRanged<FonctionOrganisation>>> fonction,
	                             List<DateRanged<Long>> remplacePar,
	                             Map<Long, List<DateRanged<Long>>> enRemplacementDe) {
		this.numeroSite = numeroSite;
		this.numeroIDE = OrganisationHelper.extractIdentifiant(autresIdentifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.rc = rc;
		this.ide = ide;
		this.nomsAdditionnels = nomsAdditionnels;
		this.typeDeSite = typeDeSite;
		this.siege = siege;
		this.fonction = fonction;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
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

	public Map<String, List<DateRanged<String>>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	public List<String> getNomsAdditionnels(RegDate date) {
		return OrganisationHelper.valuesForDate(nomsAdditionnels, date);
	}

	public DonneesRC getDonneesRC() {
		return rc;
	}

	public List<Siege> getSieges() {
		return siege;
	}

	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return typeDeSite;
	}

	@Override
	public Siege getSiege(RegDate date) {
		return OrganisationHelper.dateRangeForDate(getSieges(), date);
	}

	@Override
	public List<Adresse> getAdresses() {
		return OrganisationHelper.getAdressesPourSite(this);
	}

	@Override
	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	@Override
	public Long getRemplacePar(RegDate date) {
		return OrganisationHelper.valueForDate(remplacePar, date);
	}

	@Override
	public Map<Long, List<DateRanged<Long>>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	@Override
	public List<Long> getEnRemplacementDe(RegDate date) {
		return OrganisationHelper.valuesForDate(enRemplacementDe, date);
	}
}
