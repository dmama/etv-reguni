package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;

import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_SECONDAIRE;

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
		                                super.getSiege(), super.getFonction());
	}

	public static MockSiteOrganisationBuilder createSiteBuilder(long cantonalId,
	                                                            RegDate dateDebut,
	                                                            String nom,
	                                                            @Nullable Boolean principal,
	                                                            @Nullable Integer noOfsSiege,
	                                                            @Nullable StatusRC statusRC,
	                                                            @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                            @Nullable StatusRegistreIDE statusIde,
	                                                            @Nullable TypeOrganisationRegistreIDE typeIde
	) {
		MockSiteOrganisationBuilder mock = (MockSiteOrganisationBuilder) new MockSiteOrganisationBuilder(cantonalId)
				.addNom(dateDebut, null, nom)
				.addIdentifiant("CT.VD.PARTY", dateDebut, null, String.valueOf(cantonalId));

		if (principal != null) {
			mock.addTypeDeSite(dateDebut, null, principal ? ETABLISSEMENT_PRINCIPAL : ETABLISSEMENT_SECONDAIRE);
		}

		if (noOfsSiege != null) {
			mock.addSiege(dateDebut, null, noOfsSiege);
		}

		if (statusRC != null) {
			DonneesRCBuilder rcBuilder = new DonneesRCBuilder()
					.addNom(dateDebut, null, nom)
					.addStatus(dateDebut, null, statusRC);
			if (statusInscriptionRC != null) {
				rcBuilder.addStatusInscription(dateDebut, null, statusInscriptionRC);
			}
			mock.withRC(rcBuilder.build()
			);
		}

		if (statusIde != null) {
			DonneesRegistreIDEBuilder idebuilder = new DonneesRegistreIDEBuilder()
					.addStatus(dateDebut, null, statusIde);
			if (typeIde != null) {
				idebuilder.addTypeOrganisation(dateDebut, null, typeIde);
			}
			mock.withIde(idebuilder.build());
		}
		return mock;
	}



}
