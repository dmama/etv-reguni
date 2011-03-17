package ch.vd.uniregctb.identification.contribuable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;

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

		final Map <Etat,String> etatsMessages = initMapEtatMessage(false);

		if(etatsMessages.containsValue(Etat.A_EXPERTISER_SUSPENDU)){
			etatsMessages.remove(Etat.A_EXPERTISER_SUSPENDU);
		}

		if(etatsMessages.containsValue(Etat.A_TRAITER_MAN_SUSPENDU)){
			etatsMessages.remove(Etat.A_TRAITER_MAN_SUSPENDU);
		}

		return etatsMessages;
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatEnCoursSuspenduMessage() {

		final Map <Etat,String> etatsMessages = initMapEtatMessage(false);

		return etatsMessages;
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages archivées
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessage(final boolean isTraite) {

	final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<Etat, String> mapEtat = new HashMap<Etat, String>();
		final List<Etat> typesMessage = (List<Etat>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if(isTraite){
					return identCtbDAO.getListeEtatsMessagesTraites();
				}
				else{
					return identCtbDAO.getListeEtatsMessagesNonTraites();
				}

			}
		});

		Iterator<Etat> itEtat = typesMessage.iterator();
		while (itEtat.hasNext()) {
			Etat etat = itEtat.next();
			String libelleEtat = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyEtatMessage + etat);
			mapEtat.put(etat, libelleEtat);
		}
		return mapEtat;
	}

	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapTypeMessage() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<String, String> mapMessage = new HashMap<String, String>();
		final List<String> typesMessage = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTypesMessage();
			}
		});

		Iterator<String> itMessages = typesMessage.iterator();
		while (itMessages.hasNext()) {
			String typeMessage = itMessages.next();
			String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapMessage.put(typeMessage, typeMessageValeur);
		}

		return mapMessage;
	}


	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapTypeMessage(final boolean isTraite) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<String, String> mapMessage = new HashMap<String, String>();
		final List<String> typesMessage = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
					if(isTraite){
					return identCtbDAO.getTypesMessageEtatsTraites();
				}
				else{
					return identCtbDAO.getTypesMessageEtatsNonTraites();
				}
			}
		});

		Iterator<String> itMessages = typesMessage.iterator();
		while (itMessages.hasNext()) {
			String typeMessage = itMessages.next();
			String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapMessage.put(typeMessage, typeMessageValeur);
		}

		return mapMessage;
	}

	/**
	 * Initialise la map des utilisateurs traitants
	 *
	 * @return une map
	 */

	public Map<String, String> initMapUser() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<String, String>	mapUtilisateur = new HashMap<String, String>();
		final List<String> listVisaUser = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTraitementUser();
			}
		});

		Iterator<String> itUser = listVisaUser.iterator();
		while (itUser.hasNext()) {
			String visaUser = itUser.next();
			IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(visaUser);
			visaUser = identifiantUtilisateur.getVisa();
			String nom = identifiantUtilisateur.getNomComplet();
			if (mapUtilisateur.get(visaUser) == null) {
				mapUtilisateur.put(visaUser, nom);
			}

		}
		//Ajout du user de traitement automatique ignoré dans la requete
		mapUtilisateur.put("Traitement automatique", "Traitement automatique");
		return mapUtilisateur;
	}

	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapEmetteurId(final boolean isTraite) {

		final Map<String, String> allEmetteur = new HashMap<String, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<String> emetteurs = (List<String>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if(isTraite){
					return identCtbDAO.getEmetteursIdEtatsTraites();
				}
				else{
					return identCtbDAO.getEmetteursIdEtatsNonTraites();
				}

			}
		});

		Iterator<String> itEmetteurs = emetteurs.iterator();
		while (itEmetteurs.hasNext()) {
			String emetteur = itEmetteurs.next();

			allEmetteur.put(emetteur, emetteur);
		}

		return allEmetteur;
	}

	public Map<ErreurMessage, String> initErreurMessage() {

		mapErreurMessage = initMapEnum(ApplicationConfig.masterKeyErreurMessage, ErreurMessage.class);

		return mapErreurMessage;
	}


	public Map<Etat, String> initMapEtatArchiveMessage() {
		final Map <Etat,String> etatsMessages = initMapEtatMessage(true);

		return etatsMessages;
	}

	/**
	 * Initialise la map des periodes fiscales
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale(final boolean isTraite) {
		final Map<Integer, String> allPeriodeFiscale = new TreeMap<Integer, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Integer> periodes = (List<Integer>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if(isTraite){
					return identCtbDAO.getPeriodeEtatsTraites();
				}
				else{
					return identCtbDAO.getPeriodeEtatsNonTraites();
				}

			}
		});

		Iterator<Integer> itPeriode = periodes.iterator();
		while (itPeriode.hasNext()) {
			Integer periode = itPeriode.next();

			allPeriodeFiscale .put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}

		/**
	 * Initialise la map des periodes fiscales
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale() {
		final Map<Integer, String> allPeriodeFiscale = new TreeMap<Integer, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Integer> periodes = (List<Integer>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return identCtbDAO.getPeriodes();

			}
		});

		Iterator<Integer> itPeriode = periodes.iterator();
		while (itPeriode.hasNext()) {
			Integer periode = itPeriode.next();

			allPeriodeFiscale .put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}
}
