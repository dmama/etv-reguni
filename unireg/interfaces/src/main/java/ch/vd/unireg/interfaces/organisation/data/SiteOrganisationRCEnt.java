package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

	private final List<DateRanged<String>> nomsAdditionnels;
	private final List<DateRanged<TypeDeSite>> typeDeSite;

	private final List<Siege> siege;
	private final List<DateRanged<FonctionOrganisation>> fonction;

	public SiteOrganisationRCEnt(List<DateRanged<String>> nom, DonneesRC rc, DonneesRegistreIDE ide,
	                             Map<String, List<DateRanged<String>>> identifiants, List<DateRanged<String>> nomsAdditionnels,
	                             List<DateRanged<TypeDeSite>> typeDeSite, List<Siege> siege,
	                             List<DateRanged<FonctionOrganisation>> fonction) {
		this.numeroSite = OrganisationHelper.extractIdCantonal(identifiants);
		this.numeroIDE = OrganisationHelper.extractIdentifiant(identifiants, OrganisationConstants.CLE_IDE);
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

	public List<DateRanged<FonctionOrganisation>> getFonction() {
		return fonction;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return numeroIDE;
	}

	public DonneesRegistreIDE getDonneesRegistreIDE() {
		return ide;
	}

	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
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

}
