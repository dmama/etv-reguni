package ch.vd.unireg.evenement.organisation.interne.information;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.interfaces.service.mock.MockServiceOrganisationService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;
import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-16
 */
public class FailliteConcordatStrategyTest extends WithoutSpringTest {

	private final MockServiceOrganisationService serviceOrganisation = new MockServiceOrganisationService() {

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
			return null;
		}

		@NotNull
		@Override
		public String createOrganisationDescription(Organisation organisation, RegDate date) {
			return "UT: Org no 1L.";
		}
	};

	private final EvenementOrganisationContext context = new EvenementOrganisationContext(serviceOrganisation, null, null, null);

	private final EvenementOrganisationOptions options = new EvenementOrganisationOptions();

	private final FailliteConcordatStrategy strategy = new FailliteConcordatStrategy(context, options);

	MockOrganisation organisation = MockOrganisationFactory
			.createOrganisation(1L, 1L, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
			                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),StatusInscriptionRC.ACTIF, date(2010, 6, 24),
			                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

	final Entreprise entreprise = new Entreprise();

	@Test
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
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS, TypeEvenementOrganisation.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.SUSPENSION_FAILLITE, TypeEvenementOrganisation.FOSC_SUSPENSION_FAILLITE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE, TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE));
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.CLOTURE_FAILLITE, TypeEvenementOrganisation.FOSC_CLOTURE_DE_LA_FAILLITE);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.REVOCATION_FAILLITE, TypeEvenementOrganisation.FOSC_REVOCATION_DE_LA_FAILLITE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE, TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_DEMANDE_SURSIS_CONCORDATAIRE));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE, TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.SURSIS_CONCORDATAIRE, TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT, TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF, TypeEvenementOrganisation.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE, TypeEvenementOrganisation.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE, TypeEvenementOrganisation.FOSC_ANNULATION_SURSIS_CONCORDATAIRE);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.HOMOLOGATION_CONCORDAT, TypeEvenementOrganisation.FOSC_HOMOLOGATION_DU_CONCORDAT);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_REVOCATION_DU_CONCORDAT));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF, TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT, TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE, TypeEvenementOrganisation.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LE_CONCORDAT));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE, TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMANDEMENT_DE_PAYER));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SEQUESTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SAISIE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LA_POURSUITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL));
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS, TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER);
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_RADIATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_REACTIVATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.IDE_ANNULATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_ANNULATION_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_ANNULATION_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.RCPERS_CORRECTION_DONNEES));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_NOUVELLE_INSCRIPTION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_SUPPRESSION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_RADIATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_TRANSFERT_ETABLISSEMENT));
		Assert.assertNull(createEventAndMatch(TypeEvenementOrganisation.REE_REACTIVATION));
	}

	private void assertEvenementFiscalAvecImpact(TypeInformationComplementaire envoye, TypeEvenementOrganisation recu) throws EvenementOrganisationException {
		InformationComplementaire evtinterne = createEventAndMatch(recu);
		Assert.assertEquals(InformationComplementaireAVerifier.class, evtinterne.getClass());
		Assert.assertEquals(envoye, evtinterne.getTypeInfo());
	}

	private void assertEvenementFiscalSansImpact(TypeInformationComplementaire envoye, TypeEvenementOrganisation recu) throws EvenementOrganisationException {
		InformationComplementaire evtinterne = createEventAndMatch(recu);
		Assert.assertEquals(InformationComplementaire.class, evtinterne.getClass());
		Assert.assertEquals(envoye, evtinterne.getTypeInfo());
	}

	@Test
	public void testEntrepriseNull() throws Exception {
		Assert.assertNull(
				strategy.matchAndCreate(
						createEvent(1000000L, 1L, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER),
						organisation,
						null)
		);
	}

	private InformationComplementaire createEventAndMatch(TypeEvenementOrganisation typeEvt) throws EvenementOrganisationException {
		return (InformationComplementaire) strategy.matchAndCreate(createEvent(1000000L, 1L, typeEvt, RegDate.get(2015, 6, 24), A_TRAITER), organisation, entreprise);
	}

	@NotNull
	private static EvenementOrganisation createEvent(Long noEvenement, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setId(noEvenement);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}

}