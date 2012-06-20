package ch.vd.uniregctb.efacture;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public interface EFactureService {

	/**
	 * Permet de notfier à l'E-facture que la demande est mise en attente
	 * @param idDemande identifiant de la demande
	 * @param typeDocument le type de document qui a été imprimée
	 * @param idArchivage la clé d'archivage générée
	 * @param retourAttendu vrai si un retour est attendu suite à la notification
	 */

	void notifieMiseEnattenteInscription(String idDemande, TypeDocument typeDocument, String idArchivage, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Demande l'impression du document de demande de signature ou de demande de contact
	 *
	 * @param ctbId     le numéro de contribuable traité
	 * @param typeDocument permet de determiner le type de document à envoyer au contribuable
	 * @param dateDemande date à laquel le contribuable a fait sa demande d'inscription
	 */
	String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException;

	/**
	 *
	 * @param ctbId L'id du contribuable dont on veut recuperer la demande en cours
	 *
	 * @return retrouve la demande d'inscription en cours de traitment pour un contribuable, null s'il n'y en a pas.
	 */
	@Nullable
	DemandeValidationInscriptionDejaSoumise getDemandeInscritpionEnCoursDeTraitement(long ctbId);

	/**
	 * Identifie le contribuable avec son numero de contribualble et son numero AVS
	 *
	 * @param ctbId le numero du contribualble a identifier
	 * @param noAvs le numero AVS du contribuable a identifier
	 *
	 * @return null si l'identification est ok (le numero de contribuable existe et le numero AVS match) sinon renvoie le type de refus pour e-facture
	 */
	@Nullable
	TypeRefusEFacture identifieContribuable(long ctbId, String noAvs);
}
