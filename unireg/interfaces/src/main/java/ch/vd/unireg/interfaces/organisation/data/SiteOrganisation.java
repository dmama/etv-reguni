package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class SiteOrganisation {
	private final long cantonalId;
	@NotNull
	private final List<DateRanged<String>> nom;

	public final DonneesRC rc;
	public final DonneesRegistreIDE ide;

	private final Map<String,List<DateRanged<String>>> identifiants;
	private final List<DateRanged<String>> nomsAdditionnels;
	private final List<DateRanged<TypeDeSite>> typeDeSite;
	/**
	 * municipalityId du SwissMunicipality
	 */
	private final List<DateRanged<Integer>> siege;
	private final List<DateRanged<Fonction>> fonction;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;

	public SiteOrganisation(long cantonalId, @NotNull List<DateRanged<String>> nom, DonneesRC rc, DonneesRegistreIDE ide,
	                        Map<String, List<DateRanged<String>>> identifiants, List<DateRanged<String>> nomsAdditionnels,
	                        List<DateRanged<TypeDeSite>> typeDeSite, List<DateRanged<Integer>> siege,
	                        List<DateRanged<Fonction>> fonction, List<DateRanged<Long>> remplacePar,
	                        List<DateRanged<Long>> enRemplacementDe) {
		this.cantonalId = cantonalId;
		this.nom = nom;
		this.rc = rc;
		this.ide = ide;
		this.identifiants = identifiants;
		this.nomsAdditionnels = nomsAdditionnels;
		this.typeDeSite = typeDeSite;
		this.siege = siege;
		this.fonction = fonction;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public List<DateRanged<Fonction>> getFonction() {
		return fonction;
	}

	public DonneesRegistreIDE getIde() {
		return ide;
	}

	public Map<String, List<DateRanged<String>>> getIdentifiants() {
		return identifiants;
	}

	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	public DonneesRC getRc() {
		return rc;
	}

	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	public List<DateRanged<Integer>> getSiege() {
		return siege;
	}

	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return typeDeSite;
	}
}
