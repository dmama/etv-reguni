package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
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
	public MockOrganisation build() {
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

	public static MockOrganisation createDummySA(long cantonalId, String nom, RegDate dateDebut) {
		FormeLegale formeLegale = FormeLegale.N_0106_SOCIETE_ANONYME;
		return createSimpleEntrepriseRC(cantonalId, cantonalId + 999373737, nom, dateDebut, formeLegale, MockCommune.Lausanne.getOfsCommuneMere());
	}

	public static MockOrganisation createSimpleEntrepriseRC(long cantonalId, long cantonalIdSite, String nom, RegDate dateDebut, FormeLegale formeLegale, Integer noOfsSiegePrincipal) {
		return createOrganisation(cantonalId,
		                          cantonalIdSite,
		                          nom,
		                          dateDebut,
		                          formeLegale,
		                          noOfsSiegePrincipal,
		                          StatusRC.INSCRIT,
		                          StatusInscriptionRC.ACTIF,
		                          StatusRegistreIDE.DEFINITIF,
		                          TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE
		);
	}

	public static MockOrganisation createOrganisation(long cantonalId,
	                                                  long cantonalIdSite,
	                                                  String nom,
	                                                  RegDate dateDebut,
	                                                  @Nullable FormeLegale formeLegale,
	                                                  @Nullable Integer noOfsSiegePrincipal,
	                                                  @Nullable StatusRC statusRC,
	                                                  @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                  @Nullable StatusRegistreIDE statusIde,
	                                                  @Nullable TypeOrganisationRegistreIDE typeIde) {

		MockOrganisationBuilder mockOrg = createOrganisationBuilder(cantonalId, nom, dateDebut, formeLegale);

		MockSiteOrganisationBuilder mockSite = MockSiteOrganisationBuilder.createSiteBuilder(cantonalIdSite,
		                                                                                     dateDebut,
		                                                                                     nom,
		                                                                                     true,
		                                                                                     noOfsSiegePrincipal,
		                                                                                     statusRC,
		                                                                                     statusInscriptionRC,
		                                                                                     statusIde,
		                                                                                     typeIde);

		mockOrg.addSite(dateDebut, null, cantonalIdSite);
		mockOrg.addDonneesSite(mockSite.build());

		return  mockOrg.build();
	}

	public static MockOrganisation createOrganisationAvecSiteSecondaire(long cantonalId,
	                                                                    long siteCantonalIdPrincipal,
	                                                                    long siteCantonalIdSecondaire,
	                                                                    String nom,
	                                                                    RegDate dateDebut,
	                                                                    @Nullable FormeLegale formeLegale,
	                                                                    @Nullable Integer noOfsSiegePrincipal,
	                                                                    @Nullable Integer noOfsSiegeSecondaire,
	                                                                    @Nullable StatusRC statusRCPrincipal,
	                                                                    @Nullable StatusRC statusRCSecondaire,
	                                                                    @Nullable StatusInscriptionRC statusInscriptionRCPrincipal,
	                                                                    @Nullable StatusInscriptionRC statusInscriptionRCSecondaire,
	                                                                    @Nullable StatusRegistreIDE statusIdePrincipal,
	                                                                    @Nullable StatusRegistreIDE statusIdeSecondaire,
	                                                                    @Nullable TypeOrganisationRegistreIDE typeIdePrincipal,
	                                                                    @Nullable TypeOrganisationRegistreIDE typeIdeSecondaire) {

		MockOrganisationBuilder mockOrg = createOrganisationBuilder(cantonalId, nom, dateDebut, formeLegale);

		MockSiteOrganisationBuilder mockSitePrincipal = MockSiteOrganisationBuilder.createSiteBuilder(siteCantonalIdPrincipal,
		                                                                                              dateDebut,
		                                                                                              nom,
		                                                                                              true,
		                                                                                              noOfsSiegePrincipal,
		                                                                                              statusRCPrincipal,
		                                                                                              statusInscriptionRCPrincipal,
		                                                                                              statusIdePrincipal,
		                                                                                              typeIdePrincipal);

		mockOrg.addSite(dateDebut, null, siteCantonalIdPrincipal);
		mockOrg.addDonneesSite(mockSitePrincipal.build());

		MockSiteOrganisationBuilder mockSiteSecondaire = MockSiteOrganisationBuilder.createSiteBuilder(siteCantonalIdSecondaire,
		                                                                                               dateDebut,
		                                                                                               nom,
		                                                                                               false,
		                                                                                               noOfsSiegeSecondaire,
		                                                                                               statusRCSecondaire,
		                                                                                               statusInscriptionRCSecondaire,
		                                                                                               statusIdeSecondaire,
		                                                                                               typeIdeSecondaire);

		mockOrg.addSite(dateDebut, null, siteCantonalIdSecondaire);
		mockOrg.addDonneesSite(mockSiteSecondaire.build());

		return  mockOrg.build();
	}

	public static MockOrganisationBuilder createOrganisationBuilder(long cantonalId, String nom, RegDate dateDebut, @Nullable FormeLegale formeLegale) {
		final MockOrganisationBuilder mock = (MockOrganisationBuilder) new MockOrganisationBuilder(cantonalId)
				.addNom(dateDebut, null, nom)
				.addIdentifiant("CT.VD.PARTY", dateDebut, null, String.valueOf(cantonalId));
		if (formeLegale != null) {
			mock.addFormeLegale(dateDebut, null, formeLegale);
		}
		return mock;
	}
}
