package ch.vd.unireg.interfaces.organisation.mock;

import java.math.BigDecimal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeCapital;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationBuilder;

public class DefaultMockServiceOrganisation extends MockServiceOrganisation {

	public DefaultMockServiceOrganisation() {
		super();
	}

	@Override
	protected void init() {

		this.addOrganisation(
				new MockOrganisationBuilder(101202100L)
						.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")
						.addNomAdditionnel(RegDate.get(2015, 4, 29), null, "SST S.A., en liquidation")
						.addNomAdditionnel(RegDate.get(2015, 4, 29), null, "SST")

						.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE543257199")
						.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")
						.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101202100")

						.addSite(RegDate.get(2015, 4, 29), null, 101072613L)
						.addSite(RegDate.get(2015, 5, 30), null, 12345678L)

						.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE)
						.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

						.addDonneesSite(
								new MockSiteOrganisationBuilder(101072613L)
										.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")

										.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100057199")
										.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CH55000735325")
										.addIdentifiant("CHE", RegDate.get(2015, 4, 29), null, "100057199")
										.addIdentifiant("CH.HR", RegDate.get(2015, 4, 29), null, "CH55000735325")
										.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101072613")

										.addSiege(RegDate.get(2015, 4, 29), null, 5407)

										.addTypeDeSite(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), TypeDeSite.ETABLISSEMENT_PRINCIPAL)
										.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

										.withRC(
												new DonneesRCBuilder()
														.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")

														.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

														.addAdresseLegale(RegDate.get(2015, 4, 29), null, new AdresseBuilder()
																                  .withAddressLine1("c/o André Hefti")
																                  .withStreet("Place Large")
																                  .withTown("Leysin")
																                  .withSwissZipCode(1854L)
																                  .withPays(8100)
																                  .build()
														)
														.addAdresseLegale(RegDate.get(2015, 5, 30), null, new AdresseBuilder()
																                  .withAddressLine1("Robert Plant")
																                  .withStreet("Rue principale")
																                  .withTown("Leysin")
																                  .withSwissZipCode(1854L)
																                  .withPays(8100)
																                  .build()
														)

														.addStatusInscription(RegDate.get(2015, 4, 29), null, StatusInscriptionRC.EN_LIQUIDATION)

														.addCapital(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), new CapitalBuilder()
																            .withCapitalAmount(new BigDecimal(50000))
																            .withCashedInAmount(new BigDecimal(25000))
																            .withCurrency("CHF")
																            .withTypeOfCapital(TypeDeCapital.CAPITAL_ACTIONS)
																            .build()
														)
														.build()
										)
										.withIde(
												new DonneesRegistreIDEBuilder()
														.addTypeOrganisation(RegDate.get(2015, 4, 29), null, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE)
																// Est-ce un status valable pour une entreprise en liquidation?
														.addStatus(RegDate.get(2015, 4, 29), null, StatusRegistreIDE.RADIE)
														.addRaisonDeLiquidation(RegDate.get(2015, 4, 29), null, RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
														.addAdresseEffective(RegDate.get(2015, 4, 29), null, new AdresseBuilder()
																                     .withAddressLine1("c/o André Hefti")
																                     .withStreet("Place Large")
																                     .withTown("Leysin")
																                     .withSwissZipCode(1854L)
																                     .withPays(8100)
																                     .build()
														)
														.build()

										)
										.build())
						.addDonneesSite(
								new MockSiteOrganisationBuilder(12345678L)
										.addNom(RegDate.get(2015, 5, 30), null, "Springbok Ski ToutTours S.A., en liquidation")

										.addIdentifiant("CH.IDE", RegDate.get(2015, 5, 30), null, "CHE12345678")
										.addIdentifiant("CH.RC", RegDate.get(2015, 5, 30), null, "CH5512345678")
										.addIdentifiant("CHE", RegDate.get(2015, 5, 30), null, "12345600")
										.addIdentifiant("CH.HR", RegDate.get(2015, 5, 30), null, "CH5512345678")
										.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 5, 30), null, "12345678")

										.addSiege(RegDate.get(2015, 5, 30), null, 5407)

										.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

										.withRC(
												new DonneesRCBuilder()
														.addNom(RegDate.get(2015, 5, 30), null, "Springbok Ski ToutTours S.A., en liquidation")

														.addStatus(RegDate.get(2015, 5, 30), null, StatusRC.INSCRIT)

														.addAdresseLegale(RegDate.get(2015, 5, 30), null, new AdresseBuilder()
																                  .withAddressLine1("Albert Truc")
																                  .withStreet("Place courte")
																                  .withTown("Lolo")
																                  .withSwissZipCode(9999L)
																                  .withPays(8100)
																                  .build()
														)

														.addStatusInscription(RegDate.get(2015, 5, 30), null, StatusInscriptionRC.EN_LIQUIDATION)

														.addCapital(RegDate.get(2015, 5, 30), null, new CapitalBuilder()
																            .withCapitalAmount(new BigDecimal(50000))
																            .withCashedInAmount(new BigDecimal(50000))
																            .withCurrency("CHF")
																            .withTypeOfCapital(TypeDeCapital.CAPITAL_ACTIONS)
																            .build()
														)
														.build()
										)
										.withIde(
												new DonneesRegistreIDEBuilder()
														.addTypeOrganisation(RegDate.get(2015, 5, 30), null, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE)
																// Est-ce un status valable pour une entreprise en liquidation?
														.addStatus(RegDate.get(2015, 5, 30), null, StatusRegistreIDE.RADIE)
														.addRaisonDeLiquidation(RegDate.get(2015, 5, 30), null, RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
														.addAdresseEffective(RegDate.get(2015, 5, 30), null, new AdresseBuilder()
																                     .withAddressLine1("Albert Truc")
																                     .withStreet("Place courte")
																                     .withTown("Lolo")
																                     .withSwissZipCode(9999L)
																                     .withPays(8100)
																                     .build()
														)
														.build()

										)
										.build())
						.build()
		);
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}
}
