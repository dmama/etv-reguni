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
	private Map<String, List<DateRanged<String>>> identifiants;

	@NotNull
	private List<DateRanged<String>> nom;
	private List<DateRanged<String>> nomsAdditionels;
	private List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private List<DateRanged<Long>> sites;
	@NotNull
	private Map<Long, SiteOrganisation> donneesSites;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public Organisation(long no, @NotNull Map<String, List<DateRanged<String>>> identifiants, @NotNull List<DateRanged<String>> nom,
	                    List<DateRanged<String>> nomsAdditionels, List<DateRanged<FormeLegale>> formeLegale, @NotNull List<DateRanged<Long>> sites,
	                    @NotNull Map<Long, SiteOrganisation> donneesSites, List<DateRanged<Long>> transfereA, List<DateRanged<Long>> transferDe,
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
	public Map<Long, SiteOrganisation> getDonneesSites() {
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

	/*
		Setters réservés au Mock
	 */

	protected void setDonneesSites(@NotNull Map<Long, SiteOrganisation> donneesSites) {
		this.donneesSites = donneesSites;
	}

	protected void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
	}

	protected void setFormeLegale(List<DateRanged<FormeLegale>> formeLegale) {
		this.formeLegale = formeLegale;
	}

	protected void setIdentifiants(@NotNull Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
	}

	protected void setNom(@NotNull List<DateRanged<String>> nom) {
		this.nom = nom;
	}

	protected void setNomsAdditionels(List<DateRanged<String>> nomsAdditionels) {
		this.nomsAdditionels = nomsAdditionels;
	}

	protected void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
	}

	protected void setSites(@NotNull List<DateRanged<Long>> sites) {
		this.sites = sites;
	}

	protected void setTransferDe(List<DateRanged<Long>> transferDe) {
		this.transferDe = transferDe;
	}

	protected void setTransfereA(List<DateRanged<Long>> transfereA) {
		this.transfereA = transfereA;
	}
}
