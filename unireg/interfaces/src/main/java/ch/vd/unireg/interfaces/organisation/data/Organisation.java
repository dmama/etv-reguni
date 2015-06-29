package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class Organisation implements Serializable {

	private static final long serialVersionUID = 7148376177681713313L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long no;

	@NotNull
	private final Map<String, List<DateRanged<String>>> identifiants;

	@NotNull
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> nomsAdditionels;
	private final List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private final List<DateRanged<Long>> sites;
	@NotNull
	private final List<SiteOrganisation> donneesSites;

	private final List<DateRanged<Long>> transfereA;
	private final List<DateRanged<Long>> transferDe;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;

	public Organisation(long no, @NotNull Map<String, List<DateRanged<String>>> identifiants, @NotNull List<DateRanged<String>> nom,
	                    List<DateRanged<String>> nomsAdditionels, List<DateRanged<FormeLegale>> formeLegale, @NotNull List<DateRanged<Long>> sites,
	                    @NotNull List<SiteOrganisation> donneesSites, List<DateRanged<Long>> transfereA, List<DateRanged<Long>> transferDe,
	                    List<DateRanged<Long>> remplacePar, List<DateRanged<Long>> enRemplacementDe) {
		this.no = no;
		this.identifiants = identifiants;
		this.nom = nom;
		this.nomsAdditionels = nomsAdditionels;
		this.formeLegale = formeLegale;
		this.sites = sites;
		this.donneesSites = donneesSites;
		this.transfereA = transfereA;
		this.transferDe = transferDe;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	/**
	 *
	 * @return Le numéro technique de l'organisation pour Unireg
	 */
	public long getNo() {
		return no;
	}

	@NotNull
	public List<SiteOrganisation> getDonneesSites() {
		return donneesSites;
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@NotNull
	public Map<String, List<DateRanged<String>>> getIdentifiants() {
		return identifiants;
	}

	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionels() {
		return nomsAdditionels;
	}

	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	@NotNull
	public List<DateRanged<Long>> getSites() {
		return sites;
	}

	public List<DateRanged<Long>> getTransferDe() {
		return transferDe;
	}

	public List<DateRanged<Long>> getTransfereA() {
		return transfereA;
	}
}
