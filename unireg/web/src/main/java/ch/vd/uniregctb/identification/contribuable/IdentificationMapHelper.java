package ch.vd.uniregctb.identification.contribuable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class IdentificationMapHelper extends CommonMapHelper {

	private IdentCtbDAO identCtbDAO;

	private IdentificationContribuableService identCtbService;


	private Map<PrioriteEmetteur, String> mapPrioriteEmetteur;

	private Map<String, String> mapTypeMessage;

	private Map<ErreurMessage, String> mapErreurMessage;

	private Map<String, String> mapEmetteur;

	private Map<String, String> mapUser;

	private Map<Etat, String> mapEtatMessage;

	private PlatformTransactionManager transactionManager;

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}


	/**
	 * Initialise la map des priorités émetteurs
	 *
	 * @return une map
	 */
	public Map<PrioriteEmetteur, String> initMapPrioriteEmetteur() {

		mapPrioriteEmetteur = initMapEnum(ApplicationConfig.masterKeyPrioriteEmetteur, PrioriteEmetteur.class);

		return mapPrioriteEmetteur;
	}

	/**
	 * Initialise la map des états du message
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessage() {

		mapEtatMessage = initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class, Etat.RECU, Etat.SUSPENDU);

		return mapEtatMessage;
	}


	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 *
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
	 *
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
	 *
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
	 *
	 * @return une map
	 */

	public Map<String, String> initMapTypeMessage() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		mapTypeMessage = new HashMap<String, String>();
		final List<String> typesMessage = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTypesMessage();
			}
		});

		Iterator<String> itMessages = typesMessage.iterator();
		while (itMessages.hasNext()) {
			String typeMessage = itMessages.next();
			String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapTypeMessage.put(typeMessage, typeMessageValeur);
		}

		return mapTypeMessage;
	}

	/**
	 * Initialise la map des utilisateurs traitants
	 *
	 * @return une map
	 */

	public Map<String, String> initMapUser() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		mapUser = new HashMap<String, String>();
		final List<String> listVisaUser = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTraitementUser();
			}
		});

		Iterator<String> itUser = listVisaUser.iterator();
		while (itUser.hasNext()) {
			String visaUser = itUser.next();
			List<String> resultatsNoms = identCtbService.getNomUtilisateurFromVisaUser(visaUser);
			visaUser = resultatsNoms.get(0);
			String nom = resultatsNoms.get(1);
			if (mapUser.get(visaUser) == null) {
				mapUser.put(visaUser, nom);
			}

		}
		//Ajout du user de traitement automatique
		mapUser.put("Traitement automatique","Traitement automatique");
		return mapUser;
	}

	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapEmetteurId() {

		mapEmetteur = new HashMap<String, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<String> emetteurs = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getEmetteursId();
			}
		});

		Iterator<String> itEmetteurs = emetteurs.iterator();
		while (itEmetteurs.hasNext()) {
			String emetteur = itEmetteurs.next();

			String nomCantonFromEmetteurId = emetteur;
			try {
				nomCantonFromEmetteurId = identCtbService.getNomCantonFromEmetteurId(emetteur);
			}
			catch (InfrastructureException e) {
				//on revoie l'emetteurId Tel Quel
				nomCantonFromEmetteurId = emetteur;
			}
			mapEmetteur.put(emetteur, nomCantonFromEmetteurId);
		}

		return mapEmetteur;
	}

	public Map<ErreurMessage, String> initErreurMessage() {

		mapErreurMessage = initMapEnum(ApplicationConfig.masterKeyErreurMessage, ErreurMessage.class);

		return mapErreurMessage;
	}


}
