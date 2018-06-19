package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseEffectiveBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.AdresseLegaleBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.EtablissementBuilder;

import static ch.vd.unireg.interfaces.infra.mock.MockLocalite.Leysin;
import static ch.vd.unireg.interfaces.infra.mock.MockPays.Suisse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-03-23, <raphael.marmier@vd.ch>
 */
public class EtablissementCivilTest extends WithoutSpringTest {

	@Test
	public void testIsSuccursale() throws Exception {

		EtablissementCivil builder = new EtablissementBuilder(101072613L)
				.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
				.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
				.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")
				.addNomAdditionnel(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "SST S.A.R.L.")
				.addNomAdditionnel(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "SST S.A.")
				.addNomAdditionnel(RegDate.get(2015, 10, 1), null, "SST S.A., en liquidation")

				.addIdentifiant(EntrepriseConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE543257199")
				.addIdentifiant(EntrepriseConstants.CLE_IDE, RegDate.get(2015, 5, 30), null, "CHE982034234")

				.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE)
				.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

				.addSiege(new Domicile(RegDate.get(2015, 4, 29), null, MockCommune.Leysin))

				.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE)

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

								.addInscription(date(2015, 4, 29), date(2015, 9, 30), new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2015, 4, 29), null, date(2015, 4, 29), null))
								.addInscription(date(2015, 10, 1), null, new InscriptionRC(StatusInscriptionRC.EN_LIQUIDATION, null, date(2015, 4, 29), null, date(2015, 4, 29), null))

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
								.addTypeEntreprise(RegDate.get(2015, 4, 29), null, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE)
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
				.withRee(new DonneesREERCEnt(null))
				.build();

		assertTrue(builder.isSuccursale(date(2016, 3, 23)));
	}

	@Test
	public void testIsSuccursaleNot() throws Exception {

		EtablissementCivil builder = new EtablissementBuilder(101072613L)
				.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "Springbok Ski Tours S.A.R.L.")
				.addNom(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "Springbok Ski Tours S.A.")
				.addNom(RegDate.get(2015, 10, 1), null, "Springbok Ski Tours S.A., en liquidation")
				.addNomAdditionnel(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), "SST S.A.R.L.")
				.addNomAdditionnel(RegDate.get(2015, 5, 30), RegDate.get(2015, 9, 30), "SST S.A.")
				.addNomAdditionnel(RegDate.get(2015, 10, 1), null, "SST S.A., en liquidation")

				.addIdentifiant(EntrepriseConstants.CLE_IDE, RegDate.get(2015, 4, 29), null, "CHE543257199")
				.addIdentifiant(EntrepriseConstants.CLE_IDE, RegDate.get(2015, 5, 30), null, "CHE982034234")

				.addFormeLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 29), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE)
				.addFormeLegale(RegDate.get(2015, 5, 30), null, FormeLegale.N_0106_SOCIETE_ANONYME)

				.addSiege(new Domicile(RegDate.get(2015, 4, 29), null, MockCommune.Leysin))

				.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE)

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

								.addInscription(date(2015, 4, 29), date(2015, 7, 31), new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2015, 5, 30), null, date(2015, 5, 30), null))
								.addInscription(date(2015, 8, 1), date(2015, 9, 30), new InscriptionRC(StatusInscriptionRC.EN_LIQUIDATION, null, date(2015, 5, 30), null, date(2015, 5, 30), null))
								.addInscription(date(2015, 10, 1), null, new InscriptionRC(StatusInscriptionRC.RADIE, null, date(2015, 5, 30), date(2015, 9, 28), date(2015, 5, 30), date(2015, 9, 28)))

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
								.addTypeEntreprise(RegDate.get(2015, 4, 29), null, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE)
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
				.withRee(new DonneesREERCEnt(null))
				.build();

		assertFalse(builder.isSuccursale(date(2016, 3, 23)));
	}
}