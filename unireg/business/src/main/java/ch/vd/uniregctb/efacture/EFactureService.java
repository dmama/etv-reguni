package ch.vd.uniregctb.efacture;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeHistorisee;
import ch.vd.unireg.interfaces.efacture.data.DestinataireHistorise;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public interface EFactureService {

	/**
	 * Constante à utiliser comme billerId dans toutes les correspondances avec la e-facture
	 */
	String ACI_BILLER_ID = "ACI";

	/**
	 * Permet de notfier à l'E-facture que la demande est mise en attente
	 * @param idDemande identifiant de la demande
	 * @param typeAttenteEFacture
	 * @param description la description de la mise en attente
	 * @param idArchivage la clé d'archivage générée
	 * @param retourAttendu vrai si un retour est attendu suite à la notification
	 */

	String notifieMiseEnattenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException;

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
	DemandeHistorisee getDemandeInscriptionEnCoursDeTraitement(long ctbId);

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
	TypeRefusDemande identifieContribuablePourInscription(long ctbId, String noAvs) throws AdresseException;

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
	 * @param ctbId l'id du contribuable
	 * @return l'historique complet des demandes d'un contribuables au format interne unireg
	 */
	DestinataireHistorise getHistoriqueDestinataire(long ctbId);

	/**
	 * Demande la suspension d'un contribuable à la e-facture
	 *
	 *
	 * @param ctbId id du contribuable à suspendre
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description
	 * @return le business id du message demandant la suspension
	 */
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException;


	/**
	 * Demande l'activation d'un contribuable à la e-facture
	 *
	 *
	 * @param ctbId id du contribuable à activer
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description
	 * @return le business id du message demandant l'activation
	 */
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException;

	/**Permet l'envoi d'un message d'acceptation pour une demande d'inscription
	 *
	 *
	 *
	 * @param idDemande identifiant de la demande
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description
	 * @return le business id du message demandant l'acceptation
	 * @throws EvenementEfactureException
	 */
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException;

	/**
	 * Permet l'envoi d'un message de refus pour une demande d'inscription
	 *
	 * @param idDemande
	 * @param retourAttendu
	 * @param description
	 * @return le business id du message demandant le refus
	 */
	String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException;

}
