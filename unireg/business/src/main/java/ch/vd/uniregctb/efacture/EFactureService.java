package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
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
	 * @param typeAttenteEFacture le type d'attente
	 * @param description la description de la mise en attente
	 * @param idArchivage la clé d'archivage générée
	 * @param retourAttendu vrai si un retour est attendu suite à la notification
	 */
	String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException;

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
	 * Recupère l'historique e-facture pour un contribuable
	 *
	 * @param ctbId l'id du contribuable
	 * @return l'historique complet des demandes d'un contribuables au format interne unireg
	 */
	DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId);

	/**
	 * Demande la suspension d'un contribuable à la e-facture
	 *
	 *
	 * @param ctbId id du contribuable à suspendre
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description ...
	 * @return le business id du message demandant la suspension
	 */
	String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException;


	/**
	 * Demande l'activation d'un contribuable à la e-facture
	 *
	 *
	 * @param ctbId id du contribuable à activer
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description ...
	 * @return le business id du message demandant l'activation
	 */
	String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException;

	/**Permet l'envoi d'un message d'acceptation pour une demande d'inscription
	 *
	 *
	 *
	 * @param idDemande identifiant de la demande
	 * @param retourAttendu <code>True</code> si on veut que e-facture nous renvoie un accusé de reception <code>False</code> sinon
	 * @param description ...
	 * @return le business id du message demandant l'acceptation
	 * @throws EvenementEfactureException
	 */
	String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException;

	/**
	 * Permet l'envoi d'un message de refus pour une demande d'inscription
	 *
	 * @param idDemande id de la demande à refuser
	 * @param retourAttendu true si on doit attendre le retour e-facture via l'esb
	 * @param description ...
	 * @return le business id du message demandant le refus
	 */
	String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException;

	/**
	 * Quittance l'inscription e-facture pour un contribuable
	 *
	 *
	 * @param noCtb contribuable à quittancer
	 * @return null si le quittancement est ok
	 * @throws EvenementEfactureException
	 */
	ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException;
}
