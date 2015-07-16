package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

/**
 * Représente un object mock pour une organisation. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 */
public class MockOrganisation extends Organisation {
	public MockOrganisation(long no, @NotNull Map<String, List<DateRanged<String>>> identifiants,
	                        @NotNull List<DateRanged<String>> nom,
	                        List<DateRanged<String>> nomsAdditionels,
	                        List<DateRanged<FormeLegale>> formeLegale,
	                        @NotNull List<DateRanged<Long>> sites,
	                        @NotNull List<SiteOrganisation> donneesSites,
	                        List<DateRanged<Long>> transfereA,
	                        List<DateRanged<Long>> transferDe,
	                        List<DateRanged<Long>> remplacePar,
	                        List<DateRanged<Long>> enRemplacementDe) {
		super(no, identifiants, nom, nomsAdditionels, formeLegale, sites, donneesSites, transfereA, transferDe, remplacePar, enRemplacementDe);
	}

	protected void setDonneesSites(@NotNull List<SiteOrganisation> donneesSites) {
		super.setDonneesSites(donneesSites);
	}

	protected void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		super.setEnRemplacementDe(enRemplacementDe);
	}

	protected void setFormeLegale(List<DateRanged<FormeLegale>> formeLegale) {
		super.setFormeLegale(formeLegale);
	}

	protected void setIdentifiants(@NotNull Map<String, List<DateRanged<String>>> identifiants) {
		super.setIdentifiants(identifiants);
	}

	protected void setNom(@NotNull List<DateRanged<String>> nom) {
		super.setNom(nom);
	}

	protected void setNomsAdditionels(List<DateRanged<String>> nomsAdditionels) {
		super.setNomsAdditionels(nomsAdditionels);
	}

	protected void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		super.setRemplacePar(remplacePar);
	}

	protected void setSites(@NotNull List<DateRanged<Long>> sites) {
		super.setSites(sites);
	}

	protected void setTransferDe(List<DateRanged<Long>> transferDe) {
		super.setTransferDe(transferDe);
	}

	protected void setTransfereA(List<DateRanged<Long>> transfereA) {
		super.setTransfereA(transfereA = transfereA);
	}
}
