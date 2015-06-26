package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

public class OrganisationBuilder implements DataBuilder<Organisation> {
	private final long cantonalId;

	private Map<String, List<DateRanged<String>>> identifiants;

	@NotNull
	private final List<DateRanged<String>> nom;
	private List<DateRanged<String>> nomsAdditionels;
	private List<DateRanged<FormeLegale>> formeLegale;

	private List<DateRanged<Long>> sites;
	private List<SiteOrganisation> donneesSites;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	@Override
	public Organisation build() {
		return new Organisation(cantonalId, identifiants, nom, nomsAdditionels, formeLegale, sites,
		                        donneesSites, transfereA, transferDe, remplacePar, enRemplacementDe
		);
	}

	public OrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		this.cantonalId = cantonalId;
		this.nom = nom;
	}

	public void setDonneesSites(List<SiteOrganisation> donneesSites) {
		this.donneesSites = donneesSites;
	}

	public void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
	}

	public void setFormeLegale(List<DateRanged<FormeLegale>> formeLegale) {
		this.formeLegale = formeLegale;
	}

	public void setIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
	}

	public void setNomsAdditionels(List<DateRanged<String>> nomsAdditionels) {
		this.nomsAdditionels = nomsAdditionels;
	}

	public void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
	}

	public void setSites(List<DateRanged<Long>> sites) {
		this.sites = sites;
	}

	public void setTransferDe(List<DateRanged<Long>> transferDe) {
		this.transferDe = transferDe;
	}

	public void setTransfereA(List<DateRanged<Long>> transfereA) {
		this.transfereA = transfereA;
	}
}
