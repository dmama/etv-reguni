package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Lausanne;
import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Leysin;
import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Zurich;
import static ch.vd.unireg.interfaces.infra.mock.MockPays.Suisse;

/**
 * @author Raphaël Marmier, 2015-09-14
 */
public class OrganisationRCEntTest {

	private OrganisationRCEnt organisation;

	@Before
	public void setUp() throws Exception {
		organisation = new OrganisationBuilder(101202100L)
				.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
				.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
				.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")
				.addNomAdditionnel(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "SST S.A.R.L.")
				.addNomAdditionnel(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "SST S.A.")
				.addNomAdditionnel(RegDate.get(2015, 10, 1), null, "SST S.A., en liquidation")

				.addAutreIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE543257199")
				.addAutreIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")

				.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE)
				.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

				.addDonneesSite(
						new SiteOrganisationBuilder(101072613L)
								.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addAutreIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE100057199")

								.addSiege(new Siege(RegDate.get(2015, 4, 29), null, MockCommune.Leysin))

								.addTypeDeSite(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), TypeDeSite.ETABLISSEMENT_PRINCIPAL)
								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

												.addAdresseLegale(new AdresseBuilder()
														                  .withDateDebut(RegDate.get(2015, 4, 29))
														                  .withTitre("Robert Plant")
														                  .withRue("Rue principale")
														                  .withLocalite(Leysin.getNom())
														                  .withNumeroPostal(Leysin.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
														                  .build()
												)

												.addStatusInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusInscriptionRC.ACTIF)
												.addStatusInscription(RegDate.get(2015, 10, 1), null, StatusInscriptionRC.EN_LIQUIDATION)


												.addCapital(new CapitalBuilder()
														            .withDateDebut(RegDate.get(2015, 4, 29))
														            .withDateFin(RegDate.get(2015, 5, 29))
														            .withCapitalAmount(new BigDecimal(25000))
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
												.addAdresseEffective(new AdresseBuilder()
														                     .withDateDebut(RegDate.get(2015, 4, 29))
														                     .withDateFin(RegDate.get(2015, 9, 30))
														                     .withTitre("Robert Plant")
														                     .withRue("Rue principale")
														                     .withLocalite(Leysin.getNom())
														                     .withNumeroPostal(Leysin.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
														                     .build()
												)
												.addAdresseEffective(new AdresseBuilder()
														                     .withDateDebut(RegDate.get(2015, 10, 1))
														                     .withTitre("c/o André Hefti")
														                     .withRue("Place Large")
														                     .withLocalite(Leysin.getNom())
														                     .withNumeroPostal(Leysin.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
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

								.addAutreIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE100052312")

								.addSiege(new Siege(RegDate.get(2015, 4, 29), null, MockCommune.Lausanne))

								.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

												.addAdresseLegale(new AdresseBuilder()
														                  .withDateDebut(RegDate.get(2015, 4, 29))
														                  .withRue("Avenue de la gare")
														                  .withLocalite(Lausanne.getNom())
														                  .withNumeroPostal(Lausanne.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
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
												.addAdresseEffective(new AdresseBuilder()
														                     .withDateDebut(RegDate.get(2015, 10, 1))
														                     .withRue("Avenue de la gare")
														                     .withLocalite(Lausanne.getNom())
														                     .withNumeroPostal(Lausanne.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.build())
				.addDonneesSite(
						new SiteOrganisationBuilder(12345678L)
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addAutreIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 5, 30), null, "CHE12345678")

								.addSiege(new Siege(RegDate.get(2015, 5, 30), null, MockCommune.Zurich))

								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

								.withRC(
										new DonneesRCBuilder()
												.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
												.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

												.addStatus(RegDate.get(2015, 5, 30), null, StatusRC.INSCRIT)

												.addAdresseLegale(new AdresseBuilder()
														                  .withDateDebut(RegDate.get(2015, 5, 30))
														                  .withTitre("Albert Truc")
														                  .withRue("Place courte")
														                  .withLocalite(Zurich.getNom())
														                  .withNumeroPostal(Zurich.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
														                  .build()
												)

												.addStatusInscription(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), StatusInscriptionRC.ACTIF)
												.addStatusInscription(RegDate.get(2015, 10, 1), null, StatusInscriptionRC.EN_LIQUIDATION)

												.addCapital(new CapitalBuilder()
														            .withDateDebut(RegDate.get(2015, 5, 30))
														            .withCapitalAmount(new BigDecimal(50000))
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
												.addAdresseEffective(new AdresseBuilder()
														                     .withDateDebut(RegDate.get(2015, 5, 30))
														                     .withTitre("Albert Truc")
														                     .withRue("Place courte")
														                     .withLocalite(Zurich.getNom())
														                     .withNumeroPostal(Zurich.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.build())
				.build();

	}

	@Test
	public void testGetSiegesPrincipaux() throws Exception {
		Assert.assertEquals(MockCommune.Leysin.getNoOFS(), organisation.getSiegesPrincipaux().get(0).getNoOfs());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, organisation.getSiegesPrincipaux().get(0).getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Zurich.getNoOFS(), organisation.getSiegesPrincipaux().get(1).getNoOfs());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, organisation.getSiegesPrincipaux().get(1).getTypeAutoriteFiscale());
	}

	@Test
	public void testGetSiegePrincipal() throws Exception {
		Assert.assertEquals(MockCommune.Leysin.getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 5, 1)).getNoOfs());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, organisation.getSiegePrincipal(RegDate.get(2015, 5, 1)).getTypeAutoriteFiscale());
		Assert.assertEquals(MockCommune.Zurich.getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 6, 10)).getNoOfs());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, organisation.getSiegePrincipal(RegDate.get(2015, 6, 10)).getTypeAutoriteFiscale());
	}

	@Test
	public void testGetFormeLegale() throws Exception {
		Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE, organisation.getFormeLegale(RegDate.get(2015, 5, 1)));
		Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, organisation.getFormeLegale(RegDate.get(2015, 6, 10)));
	}

	@Test
	public void testGetCapitaux() throws Exception {
		Assert.assertEquals(25000, organisation.getCapitaux().get(0).getCapitalAmount().intValue());
		Assert.assertEquals(50000, organisation.getCapitaux().get(1).getCapitalAmount().intValue());
	}

	@Test
	public void testGetNoIDE() throws Exception {
		Assert.assertEquals("CHE543257199", organisation.getNumeroIDE().get(0).getPayload());
	}
}