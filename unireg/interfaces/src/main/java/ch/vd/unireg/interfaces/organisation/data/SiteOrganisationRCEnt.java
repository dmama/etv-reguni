package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;

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

	public SiteOrganisationRCEnt(long numeroSite,
	                             Map<String, List<DateRanged<String>>> autresIdentifiants,
	                             List<DateRanged<String>> nom,
	                             DonneesRC rc,
	                             DonneesRegistreIDE ide,
	                             Map<String, List<DateRanged<String>>> nomsAdditionnels,
	                             List<DateRanged<TypeDeSite>> typeDeSite, List<Siege> siege,
	                             Map<String, List<DateRanged<FonctionOrganisation>>> fonction) {
		this.numeroSite = numeroSite;
		this.numeroIDE = OrganisationHelper.extractIdentifiant(autresIdentifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.rc = rc;
		this.ide = ide;
		this.nomsAdditionnels = nomsAdditionnels;
		this.typeDeSite = typeDeSite;
		this.siege = siege;
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
}
