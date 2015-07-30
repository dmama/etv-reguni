package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;

/**
 * @author Raphaël Marmier, 2015-07-30
 */
public class MockSiteOrganisationBuilder extends SiteOrganisationBuilder {
	public MockSiteOrganisationBuilder(long cantonalId) {
		super(cantonalId);
	}

	public MockSiteOrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		super(cantonalId, nom);
	}

	@Override
	public MockSiteOrganisation build() {
		return new MockSiteOrganisation(super.getCantonalId(),
		                                super.getNom(), super.getRc(), super.getIde(), super.getIdentifiants(), super.getNomsAdditionnels(), super.getTypeDeSite(),
		                                super.getSiege(), super.getFonction(), super.getRemplacePar(), super.getEnRemplacementDe());
	}

}
