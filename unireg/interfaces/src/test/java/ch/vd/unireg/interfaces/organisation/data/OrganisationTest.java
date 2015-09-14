package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;

import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Lausanne;
import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Leysin;
import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Zurich;
import static ch.vd.unireg.interfaces.infra.mock.MockPays.Suisse;

/**
 * @author Raphaël Marmier, 2015-09-14
 */
public class OrganisationTest {

	private Organisation organisation;

	@Before
	public void setUp() throws Exception {
		organisation = new OrganisationBuilder(101202100L)
				.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
				.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
				.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")
				.addNomAdditionnel(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "SST S.A.R.L.")
				.addNomAdditionnel(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "SST S.A.")
				.addNomAdditionnel(RegDate.get(2015, 10, 1), null, "SST S.A., en liquidation")

				.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE543257199")
				.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")
				.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101202100")

				.addSite(RegDate.get(2015, 4, 29), null, 101072613L)
				.addSite(RegDate.get(2015, 4, 29), null, 101072656L)
				.addSite(RegDate.get(2015, 5, 30), null, 12345678L)

				.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE)
				.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

				.addDonneesSite(
						new SiteOrganisationBuilder(101072613L)
								.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100057199")

								.addSiege(RegDate.get(2015, 4, 29), null, Leysin.getCommuneLocalite().getNoOFS())

								.addTypeDeSite(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), TypeDeSite.ETABLISSEMENT_PRINCIPAL)
								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

												.addAdresseLegale(RegDate.get(2015, 4, 29), null, new AdresseBuilder()
														                  .withAddressLine1("Robert Plant")
														                  .withStreet("Rue principale")
														                  .withTown(Leysin.getNom())
														                  .withSwissZipCode(Leysin.getNPA())
														                  .withPays(Suisse.getNoOFS())
														                  .build()
												)

												.addStatusInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusInscriptionRC.ACTIF)
												.addStatusInscription(RegDate.get(2015, 10, 1), null, StatusInscriptionRC.EN_LIQUIDATION)


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
												.addStatus(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusRegistreIDE.DEFINITIF)
												.addStatus(RegDate.get(2015, 10, 1), null, StatusRegistreIDE.RADIE)
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), new AdresseBuilder()
														                     .withAddressLine1("Robert Plant")
														                     .withStreet("Rue principale")
														                     .withTown(Leysin.getNom())
														                     .withSwissZipCode(Leysin.getNPA())
														                     .withPays(Suisse.getNoOFS())
														                     .build()
												)
												.addAdresseEffective(RegDate.get(2015, 10, 1), null, new AdresseBuilder()
														                     .withAddressLine1("c/o André Hefti")
														                     .withStreet("Place Large")
														                     .withTown(Leysin.getNom())
														                     .withSwissZipCode(Leysin.getNPA())
														                     .withPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.build())
				.addDonneesSite(
						new SiteOrganisationBuilder(101072656L)
								.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100052312")

								.addSiege(RegDate.get(2015, 4, 29), null, Lausanne.getCommuneLocalite().getNoOFS())

								.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

												.addAdresseLegale(RegDate.get(2015, 4, 29), null, new AdresseBuilder()
														                  .withStreet("Avenue de la gare")
														                  .withTown(Lausanne.getNom())
														                  .withSwissZipCode(Lausanne.getNPA())
														                  .withPays(Suisse.getNoOFS())
														                  .build()
												)

												.addStatusInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusInscriptionRC.ACTIF)
												.addStatusInscription(RegDate.get(2015, 10, 1), null, StatusInscriptionRC.EN_LIQUIDATION)

												.build()
								)
								.withIde(
										new DonneesRegistreIDEBuilder()
												.addTypeOrganisation(RegDate.get(2015, 4, 29), null, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE)
												.addStatus(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusRegistreIDE.DEFINITIF)
												.addStatus(RegDate.get(2015, 10, 1), null, StatusRegistreIDE.RADIE)
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(RegDate.get(2015, 10, 1), null, new AdresseBuilder()
														                     .withStreet("Avenue de la gare")
														                     .withTown(Lausanne.getNom())
														                     .withSwissZipCode(Lausanne.getNPA())
														                     .withPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.build())
				.addDonneesSite(
						new SiteOrganisationBuilder(12345678L)
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addIdentifiant("CH.IDE", RegDate.get(2015, 5, 30), null, "CHE12345678")
								.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 5, 30), null, "12345678")

								.addSiege(RegDate.get(2015, 5, 30), null, Zurich.getCommuneLocalite().getNoOFS())

								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 5, 30), null, StatusRC.INSCRIT)

												.addAdresseLegale(RegDate.get(2015, 5, 30), null, new AdresseBuilder()
														                  .withAddressLine1("Albert Truc")
														                  .withStreet("Place courte")
														                  .withTown(Zurich.getNom())
														                  .withSwissZipCode(Zurich.getNPA())
														                  .withPays(Suisse.getNoOFS())
														                  .build()
												)

												.addStatusInscription(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), StatusInscriptionRC.ACTIF)
												.addStatusInscription(RegDate.get(2015, 10, 1), null, StatusInscriptionRC.EN_LIQUIDATION)

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
												.addStatus(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), StatusRegistreIDE.DEFINITIF)
												.addStatus(RegDate.get(2015, 10, 1), null, StatusRegistreIDE.RADIE)
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(RegDate.get(2015, 5, 30), null, new AdresseBuilder()
														                     .withAddressLine1("Albert Truc")
														                     .withStreet("Place courte")
														                     .withTown(Zurich.getNom())
														                     .withSwissZipCode(Zurich.getNPA())
														                     .withPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.build())
				.build();

	}

	@Test
	public void testGetSiegesPrincipaux() throws Exception {
		Assert.assertEquals(Leysin.getCommuneLocalite().getNoOFS(), organisation.getSiegesPrincipaux().get(0).getPayload().intValue());
		Assert.assertEquals(Zurich.getCommuneLocalite().getNoOFS(), organisation.getSiegesPrincipaux().get(1).getPayload().intValue());
	}

	@Test
	public void testGetSiegePrincipal() throws Exception {
		Assert.assertEquals(Leysin.getCommuneLocalite().getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 5, 1)).intValue());
		Assert.assertEquals(Zurich.getCommuneLocalite().getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 6, 10)).intValue());
	}

	@Test
	public void testGetFormeLegale() throws Exception {
		Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE, organisation.getFormeLegale(RegDate.get(2015, 5, 1)));
		Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, organisation.getFormeLegale(RegDate.get(2015, 6, 10)));
	}

	@Test
	public void testGetCapitaux() throws Exception {
		Assert.assertEquals(25000, organisation.getCapitaux().get(0).getPayload().getCashedInAmount().intValue());
		Assert.assertEquals(50000, organisation.getCapitaux().get(1).getPayload().getCashedInAmount().intValue());
	}

	@Test
	public void testGetNoIDE() throws Exception {
		Assert.assertEquals("CHE543257199", organisation.getNoIDE().get(0).getPayload());
	}
}