package ch.vd.unireg.evenement.entreprise.interne.information;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.service.mock.MockServiceEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.evenement.entreprise.interne.demenagement.DemenagementSiegeStrategyTest.getEvenementEntreprise;
import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;
import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-16
 */
public class FailliteConcordatStrategyTest extends WithoutSpringTest {

	private final MockServiceEntreprise serviceEntreprise = new MockServiceEntreprise() {

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
			return null;
		}

		@NotNull
		@Override
		public String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date) {
			return "UT: Org no 1L.";
		}
	};

	private final EvenementEntrepriseContext context = new EvenementEntrepriseContext(serviceEntreprise, null, null, null);

	private final EvenementEntrepriseOptions options = new EvenementEntrepriseOptions();

	private final FailliteConcordatStrategy strategy = new FailliteConcordatStrategy(context, options);

	MockEntrepriseCivile entrepriseCivile = MockEntrepriseFactory
			.createEntreprise(1L, 1L, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
			                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
			                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

	final Entreprise entreprise = new Entreprise();

	@Test
	public void testMapping() throws Exception {
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_NOUVELLE_SUCCURSALE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_DISSOLUTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_RADIATION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_RADIATION_SUCCURSALE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_REINSCRIPTION_ENTREPRISE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_AUTRE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IMPORTATION_ENTREPRISE));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS, TypeEvenementEntreprise.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.SUSPENSION_FAILLITE, TypeEvenementEntreprise.FOSC_SUSPENSION_FAILLITE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE, TypeEvenementEntreprise.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE));
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.CLOTURE_FAILLITE, TypeEvenementEntreprise.FOSC_CLOTURE_DE_LA_FAILLITE);
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.REVOCATION_FAILLITE, TypeEvenementEntreprise.FOSC_REVOCATION_DE_LA_FAILLITE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE, TypeEvenementEntreprise.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_FAILLITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_DEMANDE_SURSIS_CONCORDATAIRE));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE, TypeEvenementEntreprise.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.SURSIS_CONCORDATAIRE, TypeEvenementEntreprise.FOSC_SURSIS_CONCORDATAIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT, TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF, TypeEvenementEntreprise.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE, TypeEvenementEntreprise.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE, TypeEvenementEntreprise.FOSC_ANNULATION_SURSIS_CONCORDATAIRE);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.HOMOLOGATION_CONCORDAT, TypeEvenementEntreprise.FOSC_HOMOLOGATION_DU_CONCORDAT);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_REVOCATION_DU_CONCORDAT));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF, TypeEvenementEntreprise.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT, TypeEvenementEntreprise.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF);
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE, TypeEvenementEntreprise.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_LE_CONCORDAT));
		assertEvenementFiscalSansImpact(TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE, TypeEvenementEntreprise.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_COMMANDEMENT_DE_PAYER));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_PROCES_VERBAL_SEQUESTRE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_PROCES_VERBAL_SAISIE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_LA_POURSUITE));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL));
		assertEvenementFiscalAvecImpact(TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS, TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER);
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IDE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IDE_RADIATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IDE_REACTIVATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.IDE_ANNULATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.RCPERS_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.RCPERS_ANNULATION_DECES));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.RCPERS_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.RCPERS_ANNULATION_DEPART));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.RCPERS_CORRECTION_DONNEES));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_MUTATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_SUPPRESSION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_RADIATION));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_TRANSFERT_ETABLISSEMENT));
		Assert.assertNull(createEventAndMatch(TypeEvenementEntreprise.REE_REACTIVATION));
	}

	private void assertEvenementFiscalAvecImpact(TypeInformationComplementaire envoye, TypeEvenementEntreprise recu) throws EvenementEntrepriseException {
		InformationComplementaire evtinterne = createEventAndMatch(recu);
		Assert.assertEquals(InformationComplementaireAVerifier.class, evtinterne.getClass());
		Assert.assertEquals(envoye, evtinterne.getTypeInfo());
	}

	private void assertEvenementFiscalSansImpact(TypeInformationComplementaire envoye, TypeEvenementEntreprise recu) throws EvenementEntrepriseException {
		InformationComplementaire evtinterne = createEventAndMatch(recu);
		Assert.assertEquals(InformationComplementaire.class, evtinterne.getClass());
		Assert.assertEquals(envoye, evtinterne.getTypeInfo());
	}

	@Test
	public void testEntrepriseNull() throws Exception {
		Assert.assertNull(
				strategy.matchAndCreate(
						createEvent(1000000L, 1L, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER),
						entrepriseCivile,
						null)
		);
	}

	private InformationComplementaire createEventAndMatch(TypeEvenementEntreprise typeEvt) throws EvenementEntrepriseException {
		return (InformationComplementaire) strategy.matchAndCreate(createEvent(1000000L, 1L, typeEvt, RegDate.get(2015, 6, 24), A_TRAITER), entrepriseCivile, entreprise);
	}

	@NotNull
	private static EvenementEntreprise createEvent(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		return getEvenementEntreprise(noEvenement, noEntrepriseCivile, type, date, etat);
	}

}