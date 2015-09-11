package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class SiteOrganisation implements Serializable {

	private static final long serialVersionUID = -7085217960001155684L;

	/**
	 * Le numéro technique du site pour Unireg
	 */
	private final long no;
	@NotNull
	private List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;

	private Map<String,List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<TypeDeSite>> typeDeSite;
	/**
	 * municipalityId du SwissMunicipality
	 */
	private List<DateRanged<Integer>> siege;
	private List<DateRanged<FonctionOrganisation>> fonction;

	public SiteOrganisation(long no, @NotNull List<DateRanged<String>> nom, DonneesRC rc, DonneesRegistreIDE ide,
	                        Map<String, List<DateRanged<String>>> identifiants, List<DateRanged<String>> nomsAdditionnels,
	                        List<DateRanged<TypeDeSite>> typeDeSite, List<DateRanged<Integer>> siege,
	                        List<DateRanged<FonctionOrganisation>> fonction) {
		this.no = no;
		this.nom = nom;
		this.rc = rc;
		this.ide = ide;
		this.identifiants = identifiants;
		this.nomsAdditionnels = nomsAdditionnels;
		this.typeDeSite = typeDeSite;
		this.siege = siege;
		this.fonction = fonction;
	}

	/**
	 *
	 * @return Le numéro technique du site pour Unireg
	 */
	public long getNo() {
		return no;
	}

	public List<DateRanged<FonctionOrganisation>> getFonction() {
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

	public DonneesRC getRc() {
		return rc;
	}

	public List<DateRanged<Integer>> getSiege() {
		return siege;
	}

	public List<DateRanged<TypeDeSite>> getTypeDeSite() {
		return typeDeSite;
	}

	protected void setFonction(List<DateRanged<FonctionOrganisation>> fonction) {
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

	protected void setRc(DonneesRC rc) {
		this.rc = rc;
	}

	protected void setSiege(List<DateRanged<Integer>> siege) {
		this.siege = siege;
	}

	protected void setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		this.typeDeSite = typeDeSite;
	}
}
