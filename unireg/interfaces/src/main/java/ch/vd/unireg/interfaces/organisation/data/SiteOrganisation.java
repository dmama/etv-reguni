package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class SiteOrganisation {
	/**
	 * Le numéro technique du site pour Unireg
	 */
	private final long no;
	@NotNull
	private List<DateRanged<String>> nom;

	@NotNull
	public DonneesRC rc;
	@NotNull
	public DonneesRegistreIDE ide;

	private Map<String,List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<TypeDeSite>> typeDeSite;
	/**
	 * municipalityId du SwissMunicipality
	 */
	private List<DateRanged<Integer>> siege;
	private List<DateRanged<Fonction>> fonction;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public SiteOrganisation(long no, @NotNull List<DateRanged<String>> nom, @NotNull DonneesRC rc, @NotNull DonneesRegistreIDE ide,
	                        Map<String, List<DateRanged<String>>> identifiants, List<DateRanged<String>> nomsAdditionnels,
	                        List<DateRanged<TypeDeSite>> typeDeSite, List<DateRanged<Integer>> siege,
	                        List<DateRanged<Fonction>> fonction, List<DateRanged<Long>> remplacePar,
	                        List<DateRanged<Long>> enRemplacementDe) {
		this.no = no;
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

	/**
	 *
	 * @return Le numéro technique du site pour Unireg
	 */
	public long getNo() {
		return no;
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public List<DateRanged<Fonction>> getFonction() {
		return fonction;
	}

	@NotNull
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

	@NotNull
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

	protected void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
	}

	protected void setFonction(List<DateRanged<Fonction>> fonction) {
		this.fonction = fonction;
	}

	protected void setIde(@NotNull DonneesRegistreIDE ide) {
		this.ide = ide;
	}

	protected void setIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
	}

	protected void setNom(@NotNull List<DateRanged<String>> nom) {
		this.nom = nom;
	}

	protected void setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
	}

	protected void setRc(@NotNull DonneesRC rc) {
		this.rc = rc;
	}

	protected void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
	}

	protected void setSiege(List<DateRanged<Integer>> siege) {
		this.siege = siege;
	}

	protected void setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		this.typeDeSite = typeDeSite;
	}
}
