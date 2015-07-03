package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;

/**
 * Construit un objet mock pour une organisation
 */
public class MockOrganisationBuilder extends OrganisationBuilder {
	public MockOrganisationBuilder(long cantonalId) {
		super(cantonalId);
	}

	public MockOrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		super(cantonalId, nom);
	}

	@Override
	public Organisation build() {
		return new MockOrganisation(
				getCantonalId(),
				getIdentifiants(),
				getNom(),
				getNomsAdditionnels(),
				getFormeLegale(),
				getSites(),
				getDonneesSites(),
				getTransfereA(),
				getTransferDe(),
				getRemplacePar(),
				getEnRemplacementDe()
		);
	}
}
