package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;

/**
 * Helper de création pour des cas spécifiques de MockOrganisation
 */
public abstract class MockOrganisationFactory {

	public static MockOrganisation createDummySA(long cantonalId, String nom, RegDate dateDebut) {
		FormeLegale formeLegale = FormeLegale.N_0106_SOCIETE_ANONYME;
		return createSimpleEntrepriseRC(cantonalId, cantonalId + 999373737, nom, dateDebut, formeLegale, MockCommune.Lausanne.getOfsCommuneMere());
	}

	public static MockOrganisation createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, FormeLegale formeLegale, Integer noOfsSiegePrincipal) {
		return createOrganisation(cantonalId,
		                          cantonalIdSitePrincipal,
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
	                                                  long cantonalIdSitePrincipal,
	                                                  String nom,
	                                                  RegDate dateDebut,
	                                                  @Nullable FormeLegale formeLegale,
	                                                  @Nullable Integer noOfsSiegePrincipal,
	                                                  @Nullable StatusRC statusRC,
	                                                  @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                  @Nullable StatusRegistreIDE statusIde,
	                                                  @Nullable TypeOrganisationRegistreIDE typeIde) {

		final MockOrganisation mockOrg = new MockOrganisation(cantonalId, dateDebut, nom, formeLegale);

		MockSiteOrganisationFactory.addSite(cantonalIdSitePrincipal,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    true,
		                                    noOfsSiegePrincipal,
		                                    statusRC,
		                                    statusInscriptionRC,
		                                    statusIde,
		                                    typeIde);

		return mockOrg;
	}

	public static MockOrganisation createOrganisationAvecSiteSecondaire(long cantonalId,
	                                                                    long cantonalIdSitePrincipal,
	                                                                    long cantonalIdSiteSecondaire,
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

		final MockOrganisation mockOrg = new MockOrganisation(cantonalId, dateDebut, nom, formeLegale);

		MockSiteOrganisationFactory.addSite(cantonalIdSitePrincipal,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    true,
		                                    noOfsSiegePrincipal,
		                                    statusRCPrincipal,
		                                    statusInscriptionRCPrincipal,
		                                    statusIdePrincipal,
		                                    typeIdePrincipal);

		MockSiteOrganisationFactory.addSite(cantonalIdSiteSecondaire,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    false,
		                                    noOfsSiegeSecondaire,
		                                    statusRCSecondaire,
		                                    statusInscriptionRCSecondaire,
		                                    statusIdeSecondaire,
		                                    typeIdeSecondaire);

		return mockOrg;
	}
}
