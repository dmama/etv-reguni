package ch.vd.uniregctb.efacture;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.HistoriqueDestinataireWrapper;
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
	 *
	 * @return l'archivage id
	 */
	String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException;

	/**
	 *
	 * @param ctbId L'id du contribuable dont on veut recuperer la demande en cours
	 *
	 * @return retrouve la demande d'inscription en cours de traitment pour un contribuable, null s'il n'y en a pas.
	 */
	@Nullable
	DemandeValidationInscriptionDejaSoumise getDemandeInscriptionEnCoursDeTraitement(long ctbId);

	/**
	 * Identifie le contribuable avec son numero de contribuable
	 * et son numero AVS lors de la procédure d'inscription à la e-Facture
	 *
	 * @param ctbId le numero du contribualble a identifier
	 * @param noAvs le numero AVS du contribuable a identifier
	 *
	 * @return null si l'identification est ok (le numero de contribuable existe et le numero AVS match) sinon renvoie le type de refus pour e-facture
	 */
	@Nullable
	TypeRefusEFacture identifieContribuablePourInscription(long ctbId, String noAvs);

	/**
	 * Met à jour l'adresse e-mail du contribuable
	 *
	 * @param ctbId l'id du contribuable
	 * @param email l'adresse e-mail
	 */
	void updateEmailContribuable(long ctbId, String email);

	/**
	 * Valide l'état du contribuable lors de la procédure d'inscription à la e-Facture
	 *
	 * @param ctbId l'id du contribuale a valider
	 *
	 * @return <code>true</code> si l'état est cohérent
	 */
	boolean valideEtatContribuablePourInscription(long ctbId);


	/**Recupère l'historique des demandes pour un contribuable donné au format interne unireg
	 *
	 *
	 * @param ctbId
	 * @return l'historique complet des demandes d'un contribuables au format interne unireg
	 */
	HistoriqueDestinataireWrapper getHistoriqueDestiantaire(long ctbId);

	/**
	 * Demande la suspension d'un contribuable à la e-facture
	 *
	 * @param ctbId id du contribuable à suspendre
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @return le business id du message demandant la suspension
	 */
	public String suspendreContribuable(long ctbId, boolean retourAttendu) throws EvenementEfactureException;


	/**
	 * Demande l'activation d'un contribuable à la e-facture
	 *
	 * @param ctbId id du contribuable à activer
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @return le business id du message demandant l'activation
	 */
	public String activerContribuable(long ctbId, boolean retourAttendu) throws EvenementEfactureException;


}
