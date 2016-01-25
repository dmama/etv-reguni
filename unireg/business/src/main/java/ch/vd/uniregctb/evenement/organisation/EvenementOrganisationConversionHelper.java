package ch.vd.uniregctb.evenement.organisation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.evd0022.v1.Header;
import ch.vd.evd0022.v1.Notice;
import ch.vd.evd0022.v1.SenderIdentification;
import ch.vd.evd0022.v1.TypeOfNotice;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Classe utilitaire aidant à la prise en charge directe des données au format RCEnt.
 * <p>
 * En effet, il se trouve qu'on effectue le décodage des événements dans le package "business", alors que le format de données exposé par RCEnt ne devrait
 * en principe pas sortir du package "interface". Seulement, décoder ces données au niveau de "interface" implique:
 * - soit convertir dans un format intermédiaire converti ensuite par "business" en donnée de persistence
 * - soit convertir directement dans le format de persistence, ce qui aurait l'effet de lier "interface" à business. Ce que l'on veut éviter à tout prix
 * <p>
 * Le plus simple est encore de faire une petite entorse à nos principes et s'occupper de la conversion directe dans "business". C'est à ca que sert ce helper.
 *
 * @author Raphaël Marmier, 2015-08-03
 */
public class EvenementOrganisationConversionHelper {

	/*
		Configuration des schémas applicables pour le décodage des annonces RCEnt
    */
	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0021-1-1.xsd",
			"eVD-0022-1-3.xsd",
			"eVD-0023-1-3.xsd",
			"eVD-0024-1-3.xsd"
	};

	public static Resource[] getRCEntSchemaClassPathResource() {
		Resource[] ar = new Resource[EvenementOrganisationConversionHelper.RCENT_SCHEMA.length];
		for (int i = 0; i < EvenementOrganisationConversionHelper.RCENT_SCHEMA.length; i++) {
			ar[i] = new ClassPathResource(EvenementOrganisationConversionHelper.RCENT_SCHEMA[i]);
		}
		return ar;
	}

	public static EvenementOrganisation createEvenement(ch.vd.evd0022.v1.NoticeRoot message) {
		Header header = message.getHeader();
		Notice notice = header.getNotice();
		return new EvenementOrganisation(
				notice.getNoticeId().longValue(),
				convertSenderIdentification(header.getSenderIdentification()),
				header.getSenderReferenceData(),
				convertTypeOfNotice(notice.getTypeOfNotice()),
				notice.getNoticeDate(),
				message.getNoticeOrganisation().get(0).getOrganisationIdentification().getCantonalId().longValue(),
				EtatEvenementOrganisation.A_TRAITER
		);
	}

	public static EmetteurEvenementOrganisation convertSenderIdentification(SenderIdentification senderId) {

		switch (senderId) {
		case FOSC:
			return EmetteurEvenementOrganisation.FOSC;
		case IDE:
			return EmetteurEvenementOrganisation.IDE;
		case REE:
			return EmetteurEvenementOrganisation.REE;
		case AUTRE:
			return EmetteurEvenementOrganisation.AUTRE;
		default:
			throw new IllegalArgumentException("SenderIdentification inconnue: " + senderId.name());
		}
	}

	public static TypeEvenementOrganisation convertTypeOfNotice(TypeOfNotice typeOfNotice) {
		switch (typeOfNotice) {

		case FOSC_NOUVELLE_ENTREPRISE:
			return TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE;
		case FOSC_NOUVELLE_SUCCURSALE:
			return TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE;
		case FOSC_DISSOLUTION_ENTREPRISE:
			return TypeEvenementOrganisation.FOSC_DISSOLUTION_ENTREPRISE;
		case FOSC_RADIATION_ENTREPRISE:
			return TypeEvenementOrganisation.FOSC_RADIATION_ENTREPRISE;
		case FOSC_RADIATION_SUCCURSALE:
			return TypeEvenementOrganisation.FOSC_RADIATION_SUCCURSALE;
		case FOSC_REVOCATION_DISSOLUTION_ENTREPRISE:
			return TypeEvenementOrganisation.FOSC_REVOCATION_DISSOLUTION_ENTREPRISE;
		case FOSC_REINSCRIPTION_ENTREPRISE:
			return TypeEvenementOrganisation.FOSC_REINSCRIPTION_ENTREPRISE;
		case FOSC_AUTRE_MUTATION:
			return TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;
		case IMPORTATION_ENTREPRISE:
			return TypeEvenementOrganisation.IMPORTATION_ENTREPRISE;
		case FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE;
		case FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS:
			return TypeEvenementOrganisation.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS;
		case FOSC_SUSPENSION_FAILLITE:
			return TypeEvenementOrganisation.FOSC_SUSPENSION_FAILLITE;
		case FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE:
			return TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_ET_INVENTAIRE_DANS_FAILLITE;
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE:
			return TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_FAILLITE;
		case FOSC_CLOTURE_DE_LA_FAILLITE:
			return TypeEvenementOrganisation.FOSC_CLOTURE_DE_LA_FAILLITE;
		case FOSC_REVOCATION_DE_LA_FAILLITE:
			return TypeEvenementOrganisation.FOSC_REVOCATION_DE_LA_FAILLITE;
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE:
			return TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_FAILLITE;
		case FOSC_ETAT_DES_CHARGES_DANS_FAILLITE:
			return TypeEvenementOrganisation.FOSC_ETAT_DES_CHARGES_DANS_FAILLITE;
		case FOSC_COMMUNICATION_DANS_FAILLITE:
			return TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE;
		case FOSC_DEMANDE_SURSIS_CONCORDATAIRE:
			return TypeEvenementOrganisation.FOSC_DEMANDE_SURSIS_CONCORDATAIRE;
		case FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE:
			return TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE_PROVISOIRE;
		case FOSC_SURSIS_CONCORDATAIRE:
			return TypeEvenementOrganisation.FOSC_SURSIS_CONCORDATAIRE;
		case FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT;
		case FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF:
			return TypeEvenementOrganisation.FOSC_AUDIENCE_DE_LIQUIDATION_PAR_ABANDON_ACTIF;
		case FOSC_PROLONGATION_SURSIS_CONCORDATAIRE:
			return TypeEvenementOrganisation.FOSC_PROLONGATION_SURSIS_CONCORDATAIRE;
		case FOSC_ANNULATION_SURSIS_CONCORDATAIRE:
			return TypeEvenementOrganisation.FOSC_ANNULATION_SURSIS_CONCORDATAIRE;
		case FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS:
			return TypeEvenementOrganisation.FOSC_CONVOCATION_A_ASSEMBLEE_DES_CREANCIERS;
		case FOSC_HOMOLOGATION_DU_CONCORDAT:
			return TypeEvenementOrganisation.FOSC_HOMOLOGATION_DU_CONCORDAT;
		case FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT:
			return TypeEvenementOrganisation.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT;
		case FOSC_REVOCATION_DU_CONCORDAT:
			return TypeEvenementOrganisation.FOSC_REVOCATION_DU_CONCORDAT;
		case FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return TypeEvenementOrganisation.FOSC_ETAT_DE_COLLOCATION_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF;
		case FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF:
			return TypeEvenementOrganisation.FOSC_TABLEAU_DE_DISTRIBUTION_ET_DECOMPTE_FINAL_DANS_CONCORDAT_PAR_ABANDON_D_ACTIF;
		case FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE:
			return TypeEvenementOrganisation.FOSC_CONCORDAT_DE_BANQUE_ET_DE_CAISSE_EPARGNE;
		case FOSC_COMMUNICATION_DANS_LE_CONCORDAT:
			return TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LE_CONCORDAT;
		case FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE:
			return TypeEvenementOrganisation.FOSC_VENTE_AUX_ENCHERES_FORCEE_IMMEUBLES_DANS_POURSUITE;
		case FOSC_COMMANDEMENT_DE_PAYER:
			return TypeEvenementOrganisation.FOSC_COMMANDEMENT_DE_PAYER;
		case FOSC_PROCES_VERBAL_SEQUESTRE:
			return TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SEQUESTRE;
		case FOSC_PROCES_VERBAL_SAISIE:
			return TypeEvenementOrganisation.FOSC_PROCES_VERBAL_SAISIE;
		case FOSC_COMMUNICATION_DANS_LA_PROUSUITE:
			return TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LA_POURSUITE;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_LIQUIDATION;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_REDUCTION_CAPITAL;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFORMATION_SA_EN_SARL;
		case FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER:
			return TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_SUITE_TRANSFERT_ETRANGER;
		case IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE:
			return TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE;
		case IDE_MUTATION_DANS_REGISTRE:
			return TypeEvenementOrganisation.IDE_MUTATION_DANS_REGISTRE;
		case IDE_RADIATION_DANS_REGISTRE:
			return TypeEvenementOrganisation.IDE_RADIATION_DANS_REGISTRE;
		case IDE_REACTIVATION_DANS_REGISTRE:
			return TypeEvenementOrganisation.IDE_REACTIVATION_DANS_REGISTRE;
		case IDE_ANNULATION_DANS_REGISTRE:
			return TypeEvenementOrganisation.IDE_ANNULATION_DANS_REGISTRE;
		case RCPERS_DECES:
			return TypeEvenementOrganisation.RCPERS_DECES;
		case RCPERS_ANNULATION_DECES:
			return TypeEvenementOrganisation.RCPERS_ANNULATION_DECES;
		case RCPERS_DEPART:
			return TypeEvenementOrganisation.RCPERS_DEPART;
		case RCPERS_ANNULATION_DEPART:
			return TypeEvenementOrganisation.RCPERS_ANNULATION_DEPART;
		case RCPERS_CORRECTION_DONNEES:
			return TypeEvenementOrganisation.RCPERS_CORRECTION_DONNEES;
		default:
			throw new IllegalArgumentException("TypeEvenementOrganisation inconnu: " + typeOfNotice.name());
		}
	}
}
