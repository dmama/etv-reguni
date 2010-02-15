package ch.vd.uniregctb.identification.contribuable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;

public class IdentificationMapHelper extends CommonMapHelper {

	private IdentCtbDAO identCtbDAO;

	private Map<PrioriteEmetteur, String> mapPrioriteEmetteur;

	private Map<String, String> mapTypeMessage;

	private Map<ErreurMessage, String> mapErreurMessage;

	private Map<String, String> mapEmetteur;

	private Map<Etat, String> mapEtatMessage;

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	/**
	 * Initialise la map des priorités émetteurs
	 * @return une map
	 */
	public Map<PrioriteEmetteur, String> initMapPrioriteEmetteur() {
		if (mapPrioriteEmetteur == null) {
			mapPrioriteEmetteur = initMapEnum(ApplicationConfig.masterKeyPrioriteEmetteur, PrioriteEmetteur.class);
		}
		return mapPrioriteEmetteur;
	}

	/**
	 * Initialise la map des états du message
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessage() {

			mapEtatMessage = initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class,Etat.RECU,Etat.SUSPENDU);

		return mapEtatMessage;
	}


	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatEnCoursMessage() {

			mapEtatMessage = initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class,
					Etat.A_EXPERTISER_SUSPENDU,
					Etat.A_TRAITER_MAN_SUSPENDU,
					Etat.RECU,
					Etat.EXCEPTION,
					Etat.NON_IDENTIFIE,
					Etat.SUSPENDU,
					Etat.TRAITE_AUTOMATIQUEMENT,
					Etat.TRAITE_MANUELLEMENT,
					Etat.TRAITE_MAN_EXPERT);

		return mapEtatMessage;
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatEnCoursSuspenduMessage() {

			mapEtatMessage = initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class,
					Etat.RECU,
					Etat.EXCEPTION,
					Etat.NON_IDENTIFIE,
					Etat.SUSPENDU,
					Etat.TRAITE_AUTOMATIQUEMENT,
					Etat.TRAITE_MANUELLEMENT,
					Etat.TRAITE_MAN_EXPERT);

		return mapEtatMessage;
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages archivées
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatArchivewMessage() {

			mapEtatMessage = initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class,
					Etat.A_EXPERTISER_SUSPENDU,
					Etat.A_TRAITER_MAN_SUSPENDU,
					Etat.RECU,
					Etat.EXCEPTION,
					Etat.A_EXPERTISER,
					Etat.A_TRAITER_MANUELLEMENT,
					Etat.SUSPENDU);

		return mapEtatMessage;
	}

	/**
	 * Initialise la map des types du message
	 * @return une map
	 */
	public Map<String, String> initMapTypeMessage() {
		if (mapTypeMessage == null) {
			mapTypeMessage = new HashMap<String, String>();
			List<String> typesMessage = identCtbDAO.getTypesMessage();
			Iterator<String> itMessages = typesMessage.iterator();
			while (itMessages.hasNext()) {
				String typeMessage = itMessages.next();
				String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
				mapTypeMessage.put(typeMessage, typeMessageValeur);
			}
		}
		return mapTypeMessage;
	}

	/**
	 * Initialise la map des types du message
	 * @return une map
	 */
	public Map<String, String> initMapEmetteurId() {
		if (mapEmetteur == null) {
			mapEmetteur = new HashMap<String, String>();
			List<String> emetteurs = identCtbDAO.getEmetteursId();
			Iterator<String> itEmetteurs = emetteurs.iterator();
			while (itEmetteurs.hasNext()) {
				String emetteur = itEmetteurs.next();
				mapEmetteur.put(emetteur, emetteur);
			}
		}
		return mapEmetteur;
	}

	public Map<ErreurMessage, String>  initErreurMessage() {
		if (mapErreurMessage == null) {
			mapErreurMessage = initMapEnum(ApplicationConfig.masterKeyErreurMessage,ErreurMessage.class);

		}

		return mapErreurMessage;
	}

}
