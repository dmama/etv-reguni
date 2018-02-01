package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseEffectiveBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseLegaleBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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

				.addSite(101072613L, RegDate.get(2015, 4, 29), null, 101072613L)
				.addSite(101072656L, RegDate.get(2015, 4, 29), null, 101072656L)
				.addSite(12345678L, RegDate.get(2015, 5, 30), null, 12345678L)

				.addDonneesSite(
						new SiteOrganisationBuilder(101072613L)
								.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")
								.addNomAdditionnel(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "SST S.A.R.L.")
								.addNomAdditionnel(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "SST S.A.")
								.addNomAdditionnel(RegDate.get(2015, 10, 1), null, "SST S.A., en liquidation")

								.addIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE543257199")
								.addIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 5, 30), null, "CHE982034234")

								.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE)
								.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

								.addSiege(new Domicile(RegDate.get(2015, 4, 29), null, MockCommune.Leysin))

								.addTypeDeSite(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), TypeDeSite.ETABLISSEMENT_PRINCIPAL)
								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.withRC(
										new DonneesRCBuilder()
												.addAdresseLegale(new AdresseLegaleBuilder()
														                  .withDateDebut(RegDate.get(2015, 4, 29))
														                  .withTitre("Robert Plant")
														                  .withRue("Rue principale")
														                  .withLocalite(Leysin.getNom())
														                  .withNumeroPostal(Leysin.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
														                  .build()
												)

												.addInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), new InscriptionRC(StatusInscriptionRC.ACTIF, null, RegDate.get(2015, 4, 29), null, RegDate.get(2015, 4, 29), null))
												.addInscription(RegDate.get(2015, 10, 1), null, new InscriptionRC(StatusInscriptionRC.EN_LIQUIDATION, null, RegDate.get(2015, 4, 29), null, RegDate.get(2015, 4, 29), null))

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
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonDeRadiationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(new AdresseEffectiveBuilder()
														                     .withDateDebut(RegDate.get(2015, 4, 29))
														                     .withDateFin(RegDate.get(2015, 9, 30))
														                     .withTitre("Robert Plant")
														                     .withRue("Rue principale")
														                     .withLocalite(Leysin.getNom())
														                     .withNumeroPostal(Leysin.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
														                     .build()
												)
												.addAdresseEffective(new AdresseEffectiveBuilder()
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
								.withRee(
										new DonneesREERCEnt(null)
								)
								.build())
				.addDonneesSite(
						new SiteOrganisationBuilder(101072656L)
								.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE100052312")

								.addSiege(new Domicile(RegDate.get(2015, 4, 29), null, MockCommune.Lausanne))

								.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_SECONDAIRE)

								.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE)
								.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

								.withRC(
										new DonneesRCBuilder()
												.addAdresseLegale(new AdresseLegaleBuilder()
														                  .withDateDebut(RegDate.get(2015, 4, 29))
														                  .withRue("Avenue de la gare")
														                  .withLocalite(Lausanne.getNom())
														                  .withNumeroPostal(Lausanne.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
														                  .build()
												)

												.addInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), new InscriptionRC(StatusInscriptionRC.ACTIF, null, RegDate.get(2015, 4, 29), null, RegDate.get(2015, 4, 29), null))
												.addInscription(RegDate.get(2015, 10, 1), null, new InscriptionRC(StatusInscriptionRC.EN_LIQUIDATION, null, RegDate.get(2015, 4, 29), null, RegDate.get(2015, 4, 29), null))

												.build()
								)
								.withIde(
										new DonneesRegistreIDEBuilder()
												.addTypeOrganisation(RegDate.get(2015, 4, 29), null, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE)
												.addStatus(RegDate.get(2015, 4, 29), RegDate.get(2015, 9, 30), StatusRegistreIDE.DEFINITIF)
												.addStatus(RegDate.get(2015, 10, 1), null, StatusRegistreIDE.RADIE)
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonDeRadiationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(new AdresseEffectiveBuilder()
														                     .withDateDebut(RegDate.get(2015, 10, 1))
														                     .withRue("Avenue de la gare")
														                     .withLocalite(Lausanne.getNom())
														                     .withNumeroPostal(Lausanne.getNPA().toString())
														                     .withNoOfsPays(Suisse.getNoOFS())
														                     .build()
												)
												.build()

								)
								.withRee(
										new DonneesREERCEnt(null)
								)
								.build())
				.addDonneesSite(
						new SiteOrganisationBuilder(12345678L)
								.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
								.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")

								.addIdentifiant(OrganisationConstants.CLE_IDE, RegDate.get(2015, 5, 30), null, "CHE543257199")

								.addSiege(new Domicile(RegDate.get(2015, 5, 30), null, MockCommune.Zurich))

								.addTypeDeSite(RegDate.get(2015, 5, 30), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

								.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE)
								.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

								.withRC(
										new DonneesRCBuilder()
												.addAdresseLegale(new AdresseLegaleBuilder()
														                  .withDateDebut(RegDate.get(2015, 5, 30))
														                  .withTitre("Albert Truc")
														                  .withRue("Place courte")
														                  .withLocalite(Zurich.getNom())
														                  .withNumeroPostal(Zurich.getNPA().toString())
														                  .withNoOfsPays(Suisse.getNoOFS())
														                  .build()
												)

												.addInscription(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), new InscriptionRC(StatusInscriptionRC.ACTIF, null, RegDate.get(2015, 5, 30), null, RegDate.get(2015, 5, 30), null))
												.addInscription(RegDate.get(2015, 10, 1), null, new InscriptionRC(StatusInscriptionRC.EN_LIQUIDATION, null, RegDate.get(2015, 5, 30), null, RegDate.get(2015, 5, 30), null))

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
												.addRaisonDeLiquidation(RegDate.get(2015, 10, 1), null, RaisonDeRadiationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
												.addAdresseEffective(new AdresseEffectiveBuilder()
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
								.withRee(
										new DonneesREERCEnt(null)
								)
								.build())
				.build();

	}

	@Test
	public void testGetSiegesPrincipaux() throws Exception {
		Assert.assertEquals((Integer) MockCommune.Leysin.getNoOFS(), organisation.getSiegesPrincipaux().get(0).getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, organisation.getSiegesPrincipaux().get(0).getTypeAutoriteFiscale());
		Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), organisation.getSiegesPrincipaux().get(1).getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, organisation.getSiegesPrincipaux().get(1).getTypeAutoriteFiscale());
	}

	@Test
	public void testGetSiegePrincipal() throws Exception {
		Assert.assertEquals((Integer) MockCommune.Leysin.getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 5, 1)).getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, organisation.getSiegePrincipal(RegDate.get(2015, 5, 1)).getTypeAutoriteFiscale());
		Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), organisation.getSiegePrincipal(RegDate.get(2015, 6, 10)).getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, organisation.getSiegePrincipal(RegDate.get(2015, 6, 10)).getTypeAutoriteFiscale());
	}

	@Test
	public void testGetFormeLegale() throws Exception {
		Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, organisation.getFormeLegale(RegDate.get(2015, 5, 1)));
		Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, organisation.getFormeLegale(RegDate.get(2015, 6, 10)));
	}

	@Test
	public void testGetCapitaux() throws Exception {
		Assert.assertEquals(25000, organisation.getCapitaux().get(0).getCapitalLibere().intValue());
		Assert.assertEquals(50000, organisation.getCapitaux().get(1).getCapitalLibere().intValue());
	}

	@Test
	public void testGetNoIDE() throws Exception {
		Assert.assertEquals("CHE543257199", organisation.getNumeroIDE().get(0).getPayload());
	}

	@Test
	public void testEntreeJournal() throws Exception {
		Assert.assertEquals("CHE543257199", organisation.getNumeroIDE().get(0).getPayload());
	}

}