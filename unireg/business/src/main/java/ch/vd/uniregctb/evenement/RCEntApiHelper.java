package ch.vd.uniregctb.evenement;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.evd0022.v3.Notice;
import ch.vd.evd0022.v3.NoticeRequestIdentification;
import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
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
public class RCEntApiHelper {

	/*
		Configuration des schémas applicables pour le décodage des annonces RCEnt
    */
	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-2.xsd",
			"eVD-0023-3-2.xsd",
			"eVD-0024-3-2.xsd"
	};

	public static Resource[] getRCEntSchemaClassPathResource() {
		Resource[] ar = new Resource[RCEntApiHelper.RCENT_SCHEMA.length];
		for (int i = 0; i < RCEntApiHelper.RCENT_SCHEMA.length; i++) {
			ar[i] = new ClassPathResource(RCEntApiHelper.RCENT_SCHEMA[i]);
		}
		return ar;
	}

	public static Source[] getRCEntClasspathSources() throws IOException {
		final Source[] sources = new Source[RCENT_SCHEMA.length];
		for (int i = 0, pathLength = RCENT_SCHEMA.length; i < pathLength; i++) {
			final String path = RCENT_SCHEMA[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

	/**
	 * Extraire le numéro de l'annonce à l'IDE si l'événement rapporte un changement issu d'une annonce à l'IDE émise par Unireg.
	 *
	 * @param notice l'événement RCEnt.
	 * @return le numéro d'annonce, ou null si l'événement ne contient pas de référence à une annonce émise par Unireg.
	 * @throws EvenementOrganisationException en cas d'incohérence dans la référence.
	 */
	public static Long extractNoAnnonceIDE(Notice notice) throws EvenementOrganisationException {
		final NoticeRequestIdentification noticeRequestIdent = notice.getNoticeRequest();
		if (noticeRequestIdent != null) {
			final String applicationId = noticeRequestIdent.getReportingApplication().getId();
			final String applicationName = noticeRequestIdent.getReportingApplication().getApplicationName();
/*  SIFISC-9682 en cours: le no IDE source n'est pas encore ajouté par RCEnt. Cas en cours pour 17L1. Pour l'instant, on se contente de l'identifiant de l'application, qui suffit.
			final NamedOrganisationId ideSource = noticeRequestIdent.getIDESource();
			if (ideSource == null || ideSource.getOrganisationId() == null || ideSource.getOrganisationId().isEmpty()) {
				throw new EvenementOrganisationException(String.format("L'événement organisation n°%s est issu d'une annonce, mais le numéro IDE de l'institution source n'est pas inclu! Impossible de vérifier l'origine de l'annonce.", notice.getNoticeId().longValue()));
			}
			if (RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS.getValeur().equals(ideSource.getOrganisationId()) && RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG.equals(applicationId)) {
*/
			// TODO: Spécifier le mapping de l'énumération de reportingApplication dans jaxb2.
			if (RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG.equals(applicationId) && applicationName != null && applicationName.equals(RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG)) {
				final String noticeRequestId = noticeRequestIdent.getNoticeRequestId();
				if (noticeRequestId != null) {
					return Long.parseLong(noticeRequestId);
				} else {
					throw new EvenementOrganisationException(String.format("L'événement organisation n°%s semble provenir d'une annonce à l'IDE d'Unireg, mais le numéro d'annonce n'est pas inclus!", notice.getNoticeId().longValue()));
				}
			}
		}
		return null;
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
		case IDE_NOUVELLE_INSCRIPTION:
			return TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION;
		case IDE_MUTATION:
			return TypeEvenementOrganisation.IDE_MUTATION;
		case IDE_RADIATION:
			return TypeEvenementOrganisation.IDE_RADIATION;
		case IDE_REACTIVATION:
			return TypeEvenementOrganisation.IDE_REACTIVATION;
		case IDE_ANNULATION:
			return TypeEvenementOrganisation.IDE_ANNULATION;
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
		case REE_NOUVELLE_INSCRIPTION:
			return TypeEvenementOrganisation.REE_NOUVELLE_INSCRIPTION;
		case REE_MUTATION:
			return TypeEvenementOrganisation.REE_MUTATION;
		case REE_SUPPRESSION:
			return TypeEvenementOrganisation.REE_SUPPRESSION;
		case REE_RADIATION:
			return TypeEvenementOrganisation.REE_RADIATION;
		case REE_TRANSFERT_ETABLISSEMENT:
			return TypeEvenementOrganisation.REE_TRANSFERT_ETABLISSEMENT;
		case REE_REACTIVATION:
			return TypeEvenementOrganisation.REE_REACTIVATION;
		default:
			throw new IllegalArgumentException("TypeEvenementOrganisation inconnu: " + typeOfNotice.name());
		}
	}
}
