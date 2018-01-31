package ch.vd.unireg.interfaces.organisation.data;

import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Pair;

/**
 * @author Raphaël Marmier, 2016-09-09, <raphael.marmier@vd.ch>
 */
public interface BaseAnnonceIDE {

	TypeAnnonce getType();

	/**
	 * @return la date de programmation de l'annonce
	 */
	Date getDateAnnonce();

	/**
	 * @return l'identification de l'utilisateur à l'origine de l'annonce
	 */
	Utilisateur getUtilisateur();

	/**
	 * @return Le service IDE au nom duquel on fait l'annonce, avec l'application utilisée.
	 */
	InfoServiceIDEObligEtendues getInfoServiceIDEObligEtendues();

	/**
	 * @return le dernier statut actuel de l'annonce
	 */
	Statut getStatut();

	/**
	 * @return le type d'établissement (principal ou secondaire) visé par l'annonce
	 */
	TypeDeSite getTypeDeSite();

	/**
	 * Dans le contexte d'une annonce de création, ce champs représente le numéro IDE provisoire attribué par l'IDE lors de la quittance de l'annonce. Dans les autres contexte,
	 * il représente le numéro IDE définitif.
	 *
	 * @return le numéro IDE (potentiellement temporaire) de l'entreprise.
	 */
	@Nullable
	NumeroIDE getNoIde();

	/**
	 * Ce champ, s'il est renseigné, porte le numéro IDE "vrai" de l'entreprise dont on a annoncé la création à l'IDE, pour laquelle on a reçu un numéro IDE
	 * "temporaire" via la quittance de cette annonce, et qui s'avère déjà répertoriée à l'IDE. L'IDE rejetant notre demande nous fourni par ce biais le
	 * numéro IDE à adopter déshormais pour cette entreprise.
	 *
	 * @return le vrai numéro IDE de l'entreprise.
	 */
	@Nullable
	NumeroIDE getNoIdeRemplacant();

	/**
	 * Dans le contexte d'un établissement secondaire rattaché à une entité hors Vaud. (Il n'y a
	 * pas encore de numéro cantonal dans ce cas)
	 *
	 * @return le numéro IDE de l'entreprise faîtière de l'établissement
	 */
	@Nullable
	NumeroIDE getNoIdeEtablissementPrincipal();

	/**
	 * Dans le contexte d'une radiation, la raison doit en être précisée.
	 * @return la raison de la radiation
	 */
	@Nullable
	RaisonDeRadiationRegistreIDE getRaisonDeRadiation();

	/**
	 * @return un commentaire rédigé par l'utilisateur
	 */
	@Nullable
	String getCommentaire();

	/**
	 * @return les informations ayant trait à l'identification de l'entreprise dans le registre cantonal (RCEnt)
	 */
	@Nullable
	InformationOrganisation getInformationOrganisation();

	/**
	 * @return Les données à proprement parler de l'entreprise
	 */
	@Nullable
	Contenu getContenu();


	interface Statut {

		/**
		 * @return le statut
		 */
		StatutAnnonce getStatut();

		/**
		 * @return la date du statut du statut en cours
		 */
		Date getDateStatut();

		/**
		 * @return la liste des erreurs attachées au statut en cours
		 */
		List<Pair<String, String>> getErreurs();

		/**
 		 * @return La liste d'erreur sous forme d'une ligne de texte
		 */
		String getTexteErreurs();
	}

	interface Utilisateur {

		/**
		 * @return l'identifiant IAM de l'utilisateur à l'origine de l'annonce
		 */
		String getUserId();

		/**
		 * @return un numéro de téléphone où l'utilisateur peut être joint
		 */
		@Nullable
		String getTelephone();
	}

	interface InfoServiceIDEObligEtendues {

		/**
		 * @return le numéro IDE du service IDE
		 */
		NumeroIDE getNoIdeServiceIDEObligEtendues();

		/**
		 * @return l'identifiant de l'application utilisée pour faire la demande d'annonce.
		 */
		String getApplicationId();

		/**
		 * @return le nom de l'application utilisée pour faire la demande d'annonce.
		 */
		String getApplicationName();
	}

	interface InformationOrganisation {

		/**
		 * @return le numéro cantonal de l'établissement concerné par l'annonce (nul en cas de création)
		 */
		Long getNumeroSite();

		/**
		 * @return le numéro cantonal de l'organisation faîtière de l'établissement
		 */
		@Nullable
		Long getNumeroOrganisation();

		/**
		 * Dans un contexte de radiation, lorsqu'un établissement en supplante un autre, il faut transmettre
		 * au registre cantonal le numéro de l'établissement qui remplace l'actuel.
		 *
		 * @return le numéro de l'établissement qui supplante l'établissement visé par l'annonce de radiation
		 */
		@Nullable
		Long getNumeroSiteRemplacant();
	}

	interface Contenu {

		@Nullable
		String getNom();

		@Nullable
		String getNomAdditionnel();

		@Nullable
		AdresseAnnonceIDE getAdresse();

		@Nullable
		FormeLegale getFormeLegale();

		@Nullable
		String getSecteurActivite();
	}
}
