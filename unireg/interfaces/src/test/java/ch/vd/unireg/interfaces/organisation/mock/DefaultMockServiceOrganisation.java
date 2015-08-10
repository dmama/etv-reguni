package ch.vd.unireg.interfaces.organisation.mock;

import java.math.BigDecimal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
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

						.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE543257199")
						.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CHE123456199")
						.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101202100")

						.addSite(RegDate.get(2015, 4, 29), null, 101072613L)

						.addDonneesSite(
								new MockSiteOrganisationBuilder(101072613L)
										.addNom(RegDate.get(2015, 4, 29), null, "Springbok Ski Tours S.A., en liquidation")

										.addIdentifiant("CH.IDE", RegDate.get(2015, 4, 29), null, "CHE100057199")
										.addIdentifiant("CH.RC", RegDate.get(2015, 4, 29), null, "CH55000735325")
										.addIdentifiant("CHE", RegDate.get(2015, 4, 29), null, "100057199")
										.addIdentifiant("CH.HR", RegDate.get(2015, 4, 29), null, "CH55000735325")
										.addIdentifiant("CT.VD.PARTY", RegDate.get(2015, 4, 29), null, "101072613")

										.addSiege(RegDate.get(2015, 4, 29), null, 5407)

										.addTypeDeSite(RegDate.get(2015, 4, 29), null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)

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
						.build()
		);
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}
}
