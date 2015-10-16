package ch.vd.uniregctb.evenement.organisation.interne.passeplat;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;
import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-16
 */
public class PassePlatStrategyTest {

	private static class MockServiceOrganisationService implements ServiceOrganisationService {
		@Override
		public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
			throw new UnsupportedOperationException();
		}

		@NotNull
		@Override
		public String createOrganisationDescription(Organisation organisation, RegDate date) {
			return "UT: Org no 1L.";
		}
	}

	private final PassePlatStrategy strategy = new PassePlatStrategy();

	private final EvenementOrganisationContext context = new EvenementOrganisationContext(new MockServiceOrganisationService(), null, null);

	private final EvenementOrganisationOptions options = new EvenementOrganisationOptions(false);

	MockOrganisation organisation = MockOrganisationFactory
			.createOrganisation(1L, 1L, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
			                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
			                    TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

	final Entreprise entreprise = new Entreprise();

	@Test(timeout = 10L)
	public void testMapping() throws Exception {
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_DISSOLUTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_RADIATION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_RADIATION_SUCCURSALE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REINSCRIPTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_AUTRE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IMPORTATION_ENTREPRISE));
		Assert.assertEquals(TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE, createEventAndMatch(TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_SUSPENSION_FAILLITE));
		Assert.assertEquals(TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE, createEventAndMatch(TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_CLOTURE_DE_LA_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REVOCATION_DE_LA_FAILLITE));
		Assert.assertEquals(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE, createEventAndMatch(TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_DEMANDE_SURSIS_CONCORDATAIRE));
		Assert.assertEquals(TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE, createEventAndMatch(TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.SURSIS_CONCORDATAIRE, createEventAndMatch(TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT, createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF, createEventAndMatch(TypeEvenementOrganisation.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE, createEventAndMatch(TypeEvenementOrganisation.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE, createEventAndMatch(TypeEvenementOrganisation.FOSC_ANNULATION_SURSIS_CONCORDATAIRE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS));
		Assert.assertEquals(TypeInformationComplementaire.HOMOLOGATION_CONCORDAT, createEventAndMatch(TypeEvenementOrganisation.FOSC_HOMOLOGATION_DU_CONCORDAT).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REVOCATION_DU_CONCORDAT));
		Assert.assertEquals(TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF, createEventAndMatch(TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT, createEventAndMatch(TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF).getTypeInfo());
		Assert.assertEquals(TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE, createEventAndMatch(TypeEvenementOrganisation.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LE_CONCORDAT));
		Assert.assertEquals(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE, createEventAndMatch(TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE).getTypeInfo());
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMANDEMENT_DE_PAYER));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SEQUESTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SAISIE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LA_PROUSUITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_MUTATION_DANS_REGISTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_RADIATION_DANS_REGISTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_REACTIVATION_DANS_REGISTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_ANNULATION_DANS_REGISTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_ANNULATION_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_ANNULATION_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_CORRECTION_DONNEES));
	}

	@Test(timeout = 10L)
	public void testEntrepriseNull() throws Exception {
		Assert.assertNull(
				strategy.matchAndCreate(
						createEvent(1000000L, 1L, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut"),
						organisation,
						null,
						context,
						options)
		);
	}

	private PassePlat createEventAndMatch(TypeEvenementOrganisation typeEvt) throws EvenementOrganisationException {
		return (PassePlat) strategy.matchAndCreate(createEvent(1000000L, 1L, typeEvt, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut"), organisation, entreprise, context, options);
	}

	@NotNull
	private static EvenementOrganisation createEvent(Long evtId, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat,
	                                                   EmetteurEvenementOrganisation emetteur, String refDataEmetteur) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setId(evtId);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		event.setIdentiteEmetteur(emetteur);
		event.setRefDataEmetteur(refDataEmetteur);
		return event;
	}

}