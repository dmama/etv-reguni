package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static org.junit.Assert.assertEquals;

public class TypeOfNoticeConverterTest {

	private final TypeOfNoticeConverter converter = new TypeOfNoticeConverter();

	@Test
	public void testAllValues() throws Exception {
		/* Test de chaque valeur codée en dur car le type jaxb contient des types supplémentaires "de réserve" qu'on ignore dans Unireg. Voir: SIREF-10512 */
		assertEquals(70, TypeOfNotice.values().length);
		assertEquals(61, TypeEvenementEntreprise.values().length);
		final TypeOfNoticeConverter converter = new TypeOfNoticeConverter();
		assertEquals(TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_NOUVELLE_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_NOUVELLE_SUCCURSALE, converter.convert(TypeOfNotice.FOSC_NOUVELLE_SUCCURSALE));
		assertEquals(TypeEvenementEntreprise.FOSC_DISSOLUTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_DISSOLUTION_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_RADIATION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_RADIATION_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_RADIATION_SUCCURSALE, converter.convert(TypeOfNotice.FOSC_RADIATION_SUCCURSALE));
		assertEquals(TypeEvenementEntreprise.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_REINSCRIPTION_ENTREPRISE, converter.convert(TypeOfNotice.FOSC_REINSCRIPTION_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, converter.convert(TypeOfNotice.FOSC_AUTRE_MUTATION));
		assertEquals(TypeEvenementEntreprise.IMPORTATION_ENTREPRISE, converter.convert(TypeOfNotice.IMPORTATION_ENTREPRISE));
		assertEquals(TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, converter.convert(TypeOfNotice.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, converter.convert(TypeOfNotice.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS));
		assertEquals(TypeEvenementEntreprise.FOSC_SUSPENSION_FAILLITE, converter.convert(TypeOfNotice.FOSC_SUSPENSION_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_CLOTURE_DE_LA_FAILLITE, converter.convert(TypeOfNotice.FOSC_CLOTURE_DE_LA_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_REVOCATION_DE_LA_FAILLITE, converter.convert(TypeOfNotice.FOSC_REVOCATION_DE_LA_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_FAILLITE, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_FAILLITE));
		assertEquals(TypeEvenementEntreprise.FOSC_DEMANDE_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_DEMANDE_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementEntreprise.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE, converter.convert(TypeOfNotice.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE));
		assertEquals(TypeEvenementEntreprise.FOSC_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT));
		assertEquals(TypeEvenementEntreprise.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF, converter.convert(TypeOfNotice.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF));
		assertEquals(TypeEvenementEntreprise.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementEntreprise.FOSC_ANNULATION_SURSIS_CONCORDATAIRE, converter.convert(TypeOfNotice.FOSC_ANNULATION_SURSIS_CONCORDATAIRE));
		assertEquals(TypeEvenementEntreprise.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS, converter.convert(TypeOfNotice.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS));
		assertEquals(TypeEvenementEntreprise.FOSC_HOMOLOGATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_HOMOLOGATION_DU_CONCORDAT));
		assertEquals(TypeEvenementEntreprise.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT));
		assertEquals(TypeEvenementEntreprise.FOSC_REVOCATION_DU_CONCORDAT, converter.convert(TypeOfNotice.FOSC_REVOCATION_DU_CONCORDAT));
		assertEquals(TypeEvenementEntreprise.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF, converter.convert(TypeOfNotice.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF));
		assertEquals(TypeEvenementEntreprise.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF, converter.convert(TypeOfNotice.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF));
		assertEquals(TypeEvenementEntreprise.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE, converter.convert(TypeOfNotice.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE));
		assertEquals(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_LE_CONCORDAT, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_LE_CONCORDAT));
		assertEquals(TypeEvenementEntreprise.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE, converter.convert(TypeOfNotice.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE));
		assertEquals(TypeEvenementEntreprise.FOSC_COMMANDEMENT_DE_PAYER, converter.convert(TypeOfNotice.FOSC_COMMANDEMENT_DE_PAYER));
		assertEquals(TypeEvenementEntreprise.FOSC_PROCES_VERBAL_SEQUESTRE, converter.convert(TypeOfNotice.FOSC_PROCES_VERBAL_SEQUESTRE));
		assertEquals(TypeEvenementEntreprise.FOSC_PROCES_VERBAL_SAISIE, converter.convert(TypeOfNotice.FOSC_PROCES_VERBAL_SAISIE));
		assertEquals(TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_LA_POURSUITE, converter.convert(TypeOfNotice.FOSC_COMMUNICATION_DANS_LA_POURSUITE));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL));
		assertEquals(TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER, converter.convert(TypeOfNotice.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER));
		assertEquals(TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, converter.convert(TypeOfNotice.IDE_NOUVELLE_INSCRIPTION));
		assertEquals(TypeEvenementEntreprise.IDE_MUTATION, converter.convert(TypeOfNotice.IDE_MUTATION));
		assertEquals(TypeEvenementEntreprise.IDE_RADIATION, converter.convert(TypeOfNotice.IDE_RADIATION));
		assertEquals(TypeEvenementEntreprise.IDE_REACTIVATION, converter.convert(TypeOfNotice.IDE_REACTIVATION));
		assertEquals(TypeEvenementEntreprise.IDE_ANNULATION, converter.convert(TypeOfNotice.IDE_ANNULATION));
		assertEquals(TypeEvenementEntreprise.RCPERS_DECES, converter.convert(TypeOfNotice.RCPERS_DECES));
		assertEquals(TypeEvenementEntreprise.RCPERS_ANNULATION_DECES, converter.convert(TypeOfNotice.RCPERS_ANNULATION_DECES));
		assertEquals(TypeEvenementEntreprise.RCPERS_DEPART, converter.convert(TypeOfNotice.RCPERS_DEPART));
		assertEquals(TypeEvenementEntreprise.RCPERS_ANNULATION_DEPART, converter.convert(TypeOfNotice.RCPERS_ANNULATION_DEPART));
		assertEquals(TypeEvenementEntreprise.RCPERS_CORRECTION_DONNEES, converter.convert(TypeOfNotice.RCPERS_CORRECTION_DONNEES));
		assertEquals(TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, converter.convert(TypeOfNotice.REE_NOUVELLE_INSCRIPTION));
		assertEquals(TypeEvenementEntreprise.REE_MUTATION, converter.convert(TypeOfNotice.REE_MUTATION));
		assertEquals(TypeEvenementEntreprise.REE_SUPPRESSION, converter.convert(TypeOfNotice.REE_SUPPRESSION));
		assertEquals(TypeEvenementEntreprise.REE_RADIATION, converter.convert(TypeOfNotice.REE_RADIATION));
		assertEquals(TypeEvenementEntreprise.REE_TRANSFERT_ETABLISSEMENT, converter.convert(TypeOfNotice.REE_TRANSFERT_ETABLISSEMENT));
		assertEquals(TypeEvenementEntreprise.REE_REACTIVATION, converter.convert(TypeOfNotice.REE_REACTIVATION));
		assertEquals(TypeEvenementEntreprise.REE_NOUVEL_ETABLISSEMENT, converter.convert(TypeOfNotice.REE_NOUVEL_ETABLISSEMENT));
	}
}