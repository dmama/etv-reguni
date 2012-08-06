package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

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

	/**
	 * Le nom de l'attribut utilise pour la liste des types d'identification
	 */
	public static final String TYPE_MESSAGE_MAP_NAME = "typesMessage";

	/**
	 * Le nom de l'attribut utilise pour la liste des émetteurs
	 */
	public static final String EMETTEUR_MAP_NAME = "emetteurs";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats du message
	 */
	public static final String ETAT_MESSAGE_MAP_NAME = "etatsMessage";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats du message
	 */
	public static final String ERREUR_MESSAGE_MAP_NAME = "erreursMessage";


	/**
	 * Le nom de l'attribut utilise pour la liste des priorités
	 */
	public static final String PRIORITE_EMETTEUR_MAP_NAME = "priorites";

	/**
	 * Le nom de l'attribut utilise pour la liste des priorités
	 */
	public static final String TRAITEMENT_USER_MAP_NAME = "traitementUsers";

	/**
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	public static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";

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
	public Map<PrioriteEmetteur, String> initMapPrioriteEmetteur(final boolean isTraite) {

		final Map<PrioriteEmetteur, String> allPrioriteEmetteur = new TreeMap<PrioriteEmetteur, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<PrioriteEmetteur> listesPriorites = template.execute(new TransactionCallback<List<PrioriteEmetteur>>() {
			@Override
			public List<PrioriteEmetteur> doInTransaction(TransactionStatus status) {
				if (isTraite) {
					return identCtbDAO.getListePrioriteMessagesTraites();
				}
				else {
					return identCtbDAO.getListePrioriteMessagesNonTraites();
				}

			}
		});

		for (PrioriteEmetteur prioriteEmetteur : listesPriorites) {
			final String libellePriorite = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyPrioriteEmetteur + prioriteEmetteur);
			allPrioriteEmetteur.put(prioriteEmetteur, libellePriorite);
		}

		return allPrioriteEmetteur;
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

		final Map<Etat, String> etatsMessages = initMapEtatMessage(false);

		if (etatsMessages.containsKey(Etat.A_EXPERTISER_SUSPENDU)) {
			etatsMessages.remove(Etat.A_EXPERTISER_SUSPENDU);
		}

		if (etatsMessages.containsKey(Etat.A_TRAITER_MAN_SUSPENDU)) {
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

		final Map<Etat, String> etatsMessages = initMapEtatMessage(false);

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

		final Map<Etat, String> mapEtat = new EnumMap<Etat, String>(Etat.class);
		final List<Etat> typesMessage = template.execute(new TransactionCallback<List<Etat>>() {
			@Override
			public List<Etat> doInTransaction(TransactionStatus status) {
				if (isTraite) {
					return identCtbDAO.getListeEtatsMessagesTraites();
				}
				else {
					return identCtbDAO.getListeEtatsMessagesNonTraites();
				}

			}
		});

		for (Etat etat : typesMessage) {
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
		final List<String> typesMessage = template.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTypesMessage();
			}
		});

		for (String typeMessage : typesMessage) {
			String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapMessage.put(typeMessage, typeMessageValeur);
		}

		return sortMapAccordingToValues(mapMessage);
	}


	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapTypeMessage(final boolean isTraite, @Nullable final TypeDemande typeDemande) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<String, String> mapMessage = new HashMap<String, String>();
		final List<String> typesMessage = template.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				if (isTraite) {
					return identCtbDAO.getTypesMessageEtatsTraites(typeDemande);
				}
				else {
					return identCtbDAO.getTypesMessageEtatsNonTraites(typeDemande);
				}
			}
		});

		for (String typeMessage : typesMessage) {
			String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapMessage.put(typeMessage, typeMessageValeur);
		}

		return sortMapAccordingToValues(mapMessage);
	}


	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapTypeMessage(final boolean isTraite) {
		return initMapTypeMessage(isTraite, null);
	}

	/**
	 * Initialise la map des utilisateurs traitants
	 *
	 * @return une map
	 */

	public Map<String, String> initMapUser() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<String, String> mapUtilisateur = new HashMap<String, String>();
		final List<String> listVisaUser = template.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTraitementUser();
			}
		});

		for (String visaUser : listVisaUser) {
			IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(visaUser);
			visaUser = identifiantUtilisateur.getVisa();
			String nom = identifiantUtilisateur.getNomComplet();
			if (mapUtilisateur.get(visaUser) == null) {
				mapUtilisateur.put(visaUser, nom);
			}

		}
		//Ajout du user de traitement automatique ignoré dans la requete
		mapUtilisateur.put("Traitement automatique", "Traitement automatique");
		return sortMapAccordingToValues(mapUtilisateur);
	}

	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 */

	public Map<String, String> initMapEmetteurId(final boolean isTraite) {

		// [SIFISC-5847] Le tri des identifiants d'émetteurs doit être <i>case-insensitive</i>
		final Map<String, String> allEmetteur = new TreeMap<String, String>(new StringComparator(false, false, false, null));
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<String> emetteurs = template.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				if (isTraite) {
					return identCtbDAO.getEmetteursIdEtatsTraites();
				}
				else {
					return identCtbDAO.getEmetteursIdEtatsNonTraites();
				}
			}
		});

		for (String emetteur : emetteurs) {
			allEmetteur.put(emetteur, emetteur);
		}
		return allEmetteur;
	}

	public Map<ErreurMessage, String> initErreurMessage() {

		mapErreurMessage = initMapEnum(ApplicationConfig.masterKeyErreurMessage, ErreurMessage.class);

		return mapErreurMessage;
	}


	public Map<Etat, String> initMapEtatArchiveMessage() {
		final Map<Etat, String> etatsMessages = initMapEtatMessage(true);

		return etatsMessages;
	}

	/**
	 * Initialise la map des periodes fiscales
	 *
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale(final boolean isTraite) {
		final Map<Integer, String> allPeriodeFiscale = new TreeMap<Integer, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Integer> periodes = template.execute(new TransactionCallback<List<Integer>>() {
			@Override
			public List<Integer> doInTransaction(TransactionStatus status) {
				if (isTraite) {
					return identCtbDAO.getPeriodeEtatsTraites();
				}
				else {
					return identCtbDAO.getPeriodeEtatsNonTraites();
				}
			}
		});

		for (Integer periode : periodes) {
			allPeriodeFiscale.put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}


	/**
	 * Initialise la map des periodes fiscales
	 *
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale() {
		final Map<Integer, String> allPeriodeFiscale = new TreeMap<Integer, String>();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Integer> periodes = template.execute(new TransactionCallback<List<Integer>>() {
			@Override
			public List<Integer> doInTransaction(TransactionStatus status) {
				return identCtbDAO.getPeriodes();
			}
		});

		for (Integer periode : periodes) {
			allPeriodeFiscale.put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}

	public Map<String, Object> getMaps(final boolean isTraite) {
		final Map<String, Object> data = new HashMap<String, Object>(7);
		data.put(PERIODE_FISCALE_MAP_NAME, initMapPeriodeFiscale());
		data.put(EMETTEUR_MAP_NAME, initMapEmetteurId(isTraite));
		data.put(ETAT_MESSAGE_MAP_NAME, initMapEtatMessage());
		data.put(TYPE_MESSAGE_MAP_NAME, initMapTypeMessage());
		data.put(PRIORITE_EMETTEUR_MAP_NAME, initMapPrioriteEmetteur(isTraite));
		data.put(ERREUR_MESSAGE_MAP_NAME, initErreurMessage());
		data.put(TRAITEMENT_USER_MAP_NAME, initMapUser());
		return data;
	}

	public void putMapsIntoModel(Model model, boolean isTraite) {
		final Map<String, Object> maps = getMaps(isTraite);
		for (Map.Entry<String, Object> entry : maps.entrySet()) {
			model.addAttribute(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Tri les éléments dans une map (i.e. l'itérateur fournira les éléments dans cet ordre)
	 * par rapport au tri naturel de la valeur
	 * @param source map dont les éléments doivent être triés
	 * @param <K> type des clés de la map
	 * @param <V> type des valeurs de la map (doit implémenter {@link Comparable})
	 * @return une nouvelle map triée
	 */
	private static <K, V extends Comparable<V>> Map<K, V> sortMapAccordingToValues(Map<K, V> source) {
		if (source == null) {
			return null;
		}
		final List<Map.Entry<K, V>> content = new ArrayList<Map.Entry<K, V>>(source.entrySet());
		Collections.sort(content, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				final V v1 = o1.getValue();
				final V v2 = o2.getValue();
				if (v1 == v2) {
					return 0;
				}
				else if (v1 == null) {
					return -1;
				}
				else if (v2 == null) {
					return 1;
				}
				else {
					return v1.compareTo(v2);
				}
			}
		});
		final Map<K, V> sorted = new LinkedHashMap<K, V>(source.size());
		for (Map.Entry<K, V> item : content) {
			sorted.put(item.getKey(), item.getValue());
		}
		return sorted;
	}
}
