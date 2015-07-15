package ch.vd.unireg.interfaces.organisation.change;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.comparison.ComparisonStrategy;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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
import ch.vd.unireg.interfaces.organisation.data.builder.FonctionBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.SiteOrganisationBuilder;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class ServiceOrganisationChangeTest extends WithoutSpringTest {

	private static Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationChangeTest.class);

	private final Organisation org1 = new OrganisationBuilder(101202100L)
			.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")

			.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE543257199")
			.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")
			.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101202100")

			.addSite(RegDate.get(2015, 4, 29), null, 101072613L)

			.addDonneesSite(
					new SiteOrganisationBuilder(101072613L)
							.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")

							.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100057199")
							.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CH55000735325")
							.addIdentifiant("CHE", RegDate.get(2015, 4, 29), null, "100057199")
							.addIdentifiant("CH.HR", RegDate.get(2015, 4, 29), null, "CH55000735325")
							.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101072613")

							.addSiege(RegDate.get(2015, 4, 29), null, 5407)

							.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

							.addFonction(RegDate.get(2015, 4, 29), null,
							             new FonctionBuilder("Roger Truc", "Rennens")
									             .withAutorisation(Autorisation.SIG_COLLECTIVE_A_DEUX)
									             .withCommune(4747)
									             .withPays(8100)
									             .build()
							)
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
											.addStatusInscription(RegDate.get(2015, 4, 29), null, StatusInscriptionRC.EN_LIQUIDATION)

											.addCapital(RegDate.get(2015, 4, 29), null, new CapitalBuilder()
													            .withCapitalAmount(new BigDecimal(50000))
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

							.build()
			)
			.build();

	private final Organisation org2 = new OrganisationBuilder(101202100L)
			.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), "Springbok Ski Tours S.A., en liquidation")
			.addNom(RegDate.get(2015, 5, 26), null, "Kobgnirps Iks Rout")

			.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE543257199")
			.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")
			.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101202100")
			.addIdentifiant("IDE", RegDate.get(2015, 4, 29), null, "543257199")

			.addSite(RegDate.get(2015, 4, 29), null, 101072613L)

			.addDonneesSite(
					new SiteOrganisationBuilder(101072613L)
							.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), "Springbok Ski Tours S.A., en liquidation")
							.addNom(RegDate.get(2015, 5, 26), null, "Kobgnirps Iks Rout")

							.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100057199")
							.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CH55000735325")
							.addIdentifiant("CHE", RegDate.get(2015, 4, 29), null, "100057199")
							.addIdentifiant("CH.HR", RegDate.get(2015, 4, 29), null, "CH55000735325")
							.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101072613")

							.addSiege(RegDate.get(2015, 4, 29), null, 7045)

							.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

							.addFonction(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25),
							             new FonctionBuilder("Roger Truc", "Rennens")
									             .withAutorisation(Autorisation.SIG_COLLECTIVE_A_DEUX)
									             .withCommune(4747)
									             .withPays(8100)
									             .build()
							)

							.withRC(
									new DonneesRCBuilder()
											.addNom(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), "Springbok Ski Tours S.A., en liquidation")
											.addNom(RegDate.get(2015, 5, 26), null, "Kobgnirps Iks Rout")

											.addStatus(RegDate.get(2015, 4, 29), null, StatusRC.INSCRIT)

											.addAdresseLegale(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), new AdresseBuilder()
													                  .withAddressLine1("c/o André Hefti")
													                  .withStreet("Place Large")
													                  .withTown("Leysin")
													                  .withSwissZipCode(1854L)
													                  .withPays(8100)
													                  .build()
											)
											.addAdresseLegale(RegDate.get(2015, 5, 26), null, new AdresseBuilder()
													                  .withAddressLine1("c/o Itfeh Erdan")
													                  .withStreet("Place courte")
													                  .withTown("Nisyel")
													                  .withSwissZipCode(4581L)
													                  .withPays(8100)
													                  .build()
											)
											.addStatusInscription(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), StatusInscriptionRC.EN_LIQUIDATION)
											.addStatusInscription(RegDate.get(2015, 5, 26), null, StatusInscriptionRC.ACTIF)

											.addCapital(RegDate.get(2015, 4, 29), null, new CapitalBuilder()
													            .withCapitalAmount(new BigDecimal(50000))
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
											.addStatus(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), StatusRegistreIDE.RADIE)
											.addStatus(RegDate.get(2015, 5, 26), null, StatusRegistreIDE.DEFINITIF)
											.addRaisonDeLiquidation(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), RaisonLiquidationRegistreIDE.CESSATION_SCISSION_RETRAITE_SALARIE)
											.addAdresseEffective(RegDate.get(2015, 4, 29), RegDate.get(2015, 5, 25), new AdresseBuilder()
													                     .withAddressLine1("c/o André Hefti")
													                     .withStreet("Place Large")
													                     .withTown("Leysin")
													                     .withSwissZipCode(1854L)
													                     .withPays(8100)
													                     .build()
											)
											.addAdresseEffective(RegDate.get(2015, 5, 26), null, new AdresseBuilder()
													                     .withAddressLine1("c/o Itfeh Erdan")
													                     .withStreet("Place courte")
													                     .withTown("Nisyel")
													                     .withSwissZipCode(4581L)
													                     .withPays(8100)
													                     .build()
											)
											.build()
							)
							.build()
			)
			.build();

	@Before
	public void setup() throws Exception {
	}

	@Test
	public void testDetectChangeNewNameEndLiquidationChangeAddress() {
		// Ce scénario est totallement fictif. Une entreprise ne peut pas sortir de liquidation (?).
		final long startPrepare = System.nanoTime();

		ObjectDifferBuilder differBuilder = ObjectDifferBuilder.startBuilding();
		differBuilder.comparison().ofType(RegDate.class).toUseEqualsMethod();
		differBuilder.comparison().ofType(DateRanged.class).toUseEqualsMethod();
		differBuilder.comparison().ofType(ArrayList.class).toUse(new ComparisonStrategy() {

			final ObjectDifferBuilder internalDifferBuilder = ObjectDifferBuilder.startBuilding();
			final ObjectDiffer differ;

			{
				internalDifferBuilder.comparison().ofType(RegDate.class).toUseEqualsMethod();
				internalDifferBuilder.comparison().ofType(DateRanged.class).toUseEqualsMethod();
				differ = internalDifferBuilder.build();
			}

			@Override
			public void compare(DiffNode node, @Deprecated Class<?> type, Object working, Object base) {
				final List avant = (List) base;
				final List apres = (List) working;
				if (isEmptyList(avant) && isEmptyList(apres)) {
					node.setState(DiffNode.State.UNTOUCHED);
				}
				else if (isEmptyList(avant) && apres.size() > 0) {
					node.setState(DiffNode.State.ADDED);
				}
				else if (avant.size() > 0 && isEmptyList(apres)) {
					node.setState(DiffNode.State.REMOVED);
				}
				else if (avant.size() == apres.size()) {
					DiffNode diff = differ.compare(working, base);
					node.setState(diff.getState());
				}
				else {
					node.setState(DiffNode.State.CHANGED);
				}
			}

			private boolean isEmptyList(List list) {
				return list == null || list.size() == 0;
			}
		});

		final ObjectDiffer differ = differBuilder.build();

		final long startCompare = System.nanoTime();

		DiffNode diff = differ.compare(org2, org1);

		final long finishCompare = System.nanoTime();

		LOGGER.warn(String.format("Preparation du differ en %d millisecondes. \n" +
				                          "Comparaison des organisations en %d millisecondes",
		                          (startCompare - startPrepare) / 1000000L,
		                          (finishCompare - startCompare) / 1000000L));

		diff.visit(
				new DiffNode.Visitor() {
					public void node(DiffNode node, Visit visit) {
//				if (node.getState() == DiffNode.State.CHANGED) {
						System.out.println(node.getPath() + " => " + node.getState());
//				}
					}
				});
	}
}
