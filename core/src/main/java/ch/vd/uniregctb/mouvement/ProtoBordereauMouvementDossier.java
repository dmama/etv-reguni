package ch.vd.uniregctb.mouvement;

import ch.vd.uniregctb.type.TypeMouvement;

/**
 * Classe renvoyée par la méthode {@link MouvementDossierDAO#getAllProtoBordereaux(Integer)}
 */
public class ProtoBordereauMouvementDossier {

	/**
	 * Type de mouvement : envoi (vers collectivité administrative) ou réception (archives)
	 */
	public final TypeMouvement type;

	/**
	 * ID technique (= numéro de tiers) de la collectivité administrative initiatrice du mouvement
	 * (= source pour un envoi, seule collectivité administrative impliquée pour une réception aux archives)
	 */
	public final long idCollAdmInitiatrice;

	/**
	 * Numéro de collectivité administrative de l'entité initiatrice du mouvement
	 * (= source pour un envoi, seule collectivité administrative impliquée pour une réception aux archives)
	 */
	public final int noCollAdmInitiatrice;

	/**
	 * ID technique (= numéro de tiers) de la collectivité administrative de destination d'un
	 * mouvement d'envoi (<code>null</code> pour une réception aux archives)
	 */
	public final Long idCollAdmDestinaire;

	/**
	 * Numéro de collectivité administrative de destination d'un
	 * mouvement d'envoi (<code>null</code> pour une réception aux archives)
	 */
	public final Integer noCollAdmDestinataire;

	/**
	 * Nombre de mouvements de dossiers impliqués par ce bordereau
	 */
	public final int nbMouvements;

	/**
	 * Constructeur pour un bordereau d'envoi d'OID à OID
	 */
	public static ProtoBordereauMouvementDossier createEnvoi(long idCollAdminSource, int noCollAdminSource, long idCollAdminDestination, int noCollAdminDestination, int nbMouvement) {
		return new ProtoBordereauMouvementDossier(TypeMouvement.EnvoiDossier, idCollAdminSource, noCollAdminSource, idCollAdminDestination, noCollAdminDestination, nbMouvement);
	}

	/**
	 * Constructeur pour un bordereau de réception aux archives
	 */
	public static ProtoBordereauMouvementDossier createArchivage(long idCollAdmin, int noCollAdmin, int nbMouvements) {
		return new ProtoBordereauMouvementDossier(TypeMouvement.ReceptionDossier, idCollAdmin, noCollAdmin, null, null, nbMouvements);
	}

	private ProtoBordereauMouvementDossier(TypeMouvement type, long idCollAdmInitiatrice, int noCollAdmInitiatrice,
	                                       Long idCollAdmDestinaire, Integer noCollAdmDestinataire, int nbMouvements) {
		this.type = type;
		this.idCollAdmInitiatrice = idCollAdmInitiatrice;
		this.noCollAdmInitiatrice = noCollAdmInitiatrice;
		this.idCollAdmDestinaire = idCollAdmDestinaire;
		this.noCollAdmDestinataire = noCollAdmDestinataire;
		this.nbMouvements = nbMouvements;
	}
}
