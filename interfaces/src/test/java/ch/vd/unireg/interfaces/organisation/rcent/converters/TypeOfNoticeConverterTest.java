package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static org.junit.Assert.assertEquals;

public class TypeOfNoticeConverterTest {

	private final TypeOfNoticeConverter converter = new TypeOfNoticeConverter();

	@Test
	public void testAllValues() throws Exception {
		/* Test de chaque valeur codée en dur car le type jaxb contient des types supplémentaires "de réserve" qu'on ignore dans Unireg. Voir: SIREF-10512 */
		assertEquals(70, TypeOfNotice.values().length);
		assertEquals(61, TypeEvenementOrganisation.values().length);
		final TypeOfNoticeConverter converter = new TypeOfNoticeConverter();
		assertEquals(TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_NOUVELLE_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE, converter.convert(TypeOfNotice.FOSC_NOUVELLE_SUCCURSALE));
		assertEquals(TypeEvenementOrganisation.FOSC_DISSOLUTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_DISSOLUTION_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_RADIATION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_RADIATION_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_RADIATION_SUCCURSALE, converter.convert(TypeOfNotice.FOSC_RADIATION_SUCCURSALE));
		assertEquals(TypeEvenementOrganisation.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_REINSCRIPTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_REINSCRIPTION_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, converter.convert(TypeOfNotice.FOSC_AUTRE_MUTATION));
		assertEquals(TypeEvenementOrganisation.IMPORTATION_ENTREPRISE, converter.convert(TypeOfNotice.IMPORTATION_ENTREPRISE));
		assertEquals(TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, converter.convert(TypeOfNotice.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, converter.convert(TypeOfNotice.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS));
		assertEquals(TypeEvenementOrganisation.FOSC_SUSPENSION_FAILLITE, converter.convert(TypeOfNotice.FOSC_SUSPENSION_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_CLOTURE_DE_LA_FAILLITE, converter.convert(TypeOfNotice.FOSC_CLOTURE_DE_LA_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_REVOCATION_DE_LA_FAILLITE, converter.convert(TypeOfNotice.FOSC_REVOCATION_DE_LA_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_FAILLITE));
		assertEquals(TypeEvenementOrganisation.FOSC_DEMANDE_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_DEMANDE_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE, converter.convert(TypeOfNotice.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE));
		assertEquals(TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT));
		assertEquals(TypeEvenementOrganisation.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF, converter.convert(TypeOfNotice.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF));
		assertEquals(TypeEvenementOrganisation.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementOrganisation.FOSC_ANNULATION_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_ANNULATION_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementOrganisation.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS, converter.convert(TypeOfNotice.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS));
		assertEquals(TypeEvenementOrganisation.FOSC_HOMOLOGATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_HOMOLOGATION_DU_CONCORDAT));
		assertEquals(TypeEvenementOrganisation.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT));
		assertEquals(TypeEvenementOrganisation.FOSC_REVOCATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_REVOCATION_DU_CONCORDAT));
		assertEquals(TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF, converter.convert(TypeOfNotice.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF));
		assertEquals(TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF, converter.convert(TypeOfNotice.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF));
		assertEquals(TypeEvenementOrganisation.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE, converter.convert(TypeOfNotice.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE));
		assertEquals(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LE_CONCORDAT, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_LE_CONCORDAT));
		assertEquals(TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE, converter.convert(TypeOfNotice.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE));
		assertEquals(TypeEvenementOrganisation.FOSC_COMMANDEMENT_DE_PAYER, converter.convert(TypeOfNotice.FOSC_COMMANDEMENT_DE_PAYER));
		assertEquals(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SEQUESTRE, converter.convert(TypeOfNotice.FOSC_PROCES_VERBAL_SEQUESTRE));
		assertEquals(TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SAISIE, converter.convert(TypeOfNotice.FOSC_PROCES_VERBAL_SAISIE));
		assertEquals(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LA_POURSUITE, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_LA_POURSUITE));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL));
		assertEquals(TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER));
		assertEquals(TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, converter.convert(TypeOfNotice.IDE_NOUVELLE_INSCRIPTION));
		assertEquals(TypeEvenementOrganisation.IDE_MUTATION, converter.convert(TypeOfNotice.IDE_MUTATION));
		assertEquals(TypeEvenementOrganisation.IDE_RADIATION, converter.convert(TypeOfNotice.IDE_RADIATION));
		assertEquals(TypeEvenementOrganisation.IDE_REACTIVATION, converter.convert(TypeOfNotice.IDE_REACTIVATION));
		assertEquals(TypeEvenementOrganisation.IDE_ANNULATION, converter.convert(TypeOfNotice.IDE_ANNULATION));
		assertEquals(TypeEvenementOrganisation.RCPERS_DECES, converter.convert(TypeOfNotice.RCPERS_DECES));
		assertEquals(TypeEvenementOrganisation.RCPERS_ANNULATION_DECES, converter.convert(TypeOfNotice.RCPERS_ANNULATION_DECES));
		assertEquals(TypeEvenementOrganisation.RCPERS_DEPART, converter.convert(TypeOfNotice.RCPERS_DEPART));
		assertEquals(TypeEvenementOrganisation.RCPERS_ANNULATION_DEPART, converter.convert(TypeOfNotice.RCPERS_ANNULATION_DEPART));
		assertEquals(TypeEvenementOrganisation.RCPERS_CORRECTION_DONNEES, converter.convert(TypeOfNotice.RCPERS_CORRECTION_DONNEES));
		assertEquals(TypeEvenementOrganisation.REE_NOUVELLE_INSCRIPTION, converter.convert(TypeOfNotice.REE_NOUVELLE_INSCRIPTION));
		assertEquals(TypeEvenementOrganisation.REE_MUTATION, converter.convert(TypeOfNotice.REE_MUTATION));
		assertEquals(TypeEvenementOrganisation.REE_SUPPRESSION, converter.convert(TypeOfNotice.REE_SUPPRESSION));
		assertEquals(TypeEvenementOrganisation.REE_RADIATION, converter.convert(TypeOfNotice.REE_RADIATION));
		assertEquals(TypeEvenementOrganisation.REE_TRANSFERT_ETABLISSEMENT, converter.convert(TypeOfNotice.REE_TRANSFERT_ETABLISSEMENT));
		assertEquals(TypeEvenementOrganisation.REE_REACTIVATION, converter.convert(TypeOfNotice.REE_REACTIVATION));
		assertEquals(TypeEvenementOrganisation.REE_NOUVEL_ETABLISSEMENT, converter.convert(TypeOfNotice.REE_NOUVEL_ETABLISSEMENT));
	}
}