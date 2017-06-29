package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

public class IdentificationMapHelper extends CommonMapHelper {

	private IdentificationContribuableService identCtbService;

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

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	/**
	 * Initialise la map des priorités émetteurs
	 *
	 * @return une map
	 */
	public Map<PrioriteEmetteur, String> initMapPrioriteEmetteur() {

		final Map<PrioriteEmetteur, String> allPrioriteEmetteur = new TreeMap<>();

		final String libellePrioritaire = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyPrioriteEmetteur + PrioriteEmetteur.PRIORITAIRE);
		final String libelleNonPrioritaire = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyPrioriteEmetteur + PrioriteEmetteur.NON_PRIORITAIRE);
		allPrioriteEmetteur.put(PrioriteEmetteur.PRIORITAIRE, libellePrioritaire);
		allPrioriteEmetteur.put(PrioriteEmetteur.NON_PRIORITAIRE, libelleNonPrioritaire);

		return allPrioriteEmetteur;
	}

	/**
	 * Initialise la map des états du message
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessage() {
		return initMapEnum(ApplicationConfig.masterKeyEtatMessage, Etat.class, Etat.RECU, Etat.SUSPENDU);
	}


	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessageEnCours() {
		return initMapEtatMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours ou en exception
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessageEnCoursEtException() {
		return initMapEtatMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages en cours
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessageSuspendu() {
		return initMapEtatMessage(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);
	}

	/**
	 * Initialise la map des états du message pour l'ecran des messages archivées
	 *
	 * @return une map
	 */
	public Map<Etat, String> initMapEtatMessage(final IdentificationContribuableEtatFilter filter) {
		final Map<Etat, String> mapEtat = new EnumMap<>(Etat.class);
		final Collection<Etat> etats = identCtbService.getEtats(filter);

		return getEtatAndLibelle(mapEtat, etats);
	}

	private Map<Etat, String> getEtatAndLibelle(Map<Etat, String> mapEtat, Collection<Etat> typesMessage) {
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

	public Map<String, String> initMapTypeMessage(IdentificationContribuableEtatFilter filter) {
		final Map<String, String> mapMessage = new HashMap<>();
		final Collection<String> typesMessage = identCtbService.getTypesMessages(filter);
		return getTypeMessageAndLibelle(mapMessage, typesMessage);
	}

	private Map<String, String> getTypeMessageAndLibelle(Map<String, String> mapMessage, Collection<String> typesMessage) {
		for (String typeMessage : typesMessage) {
			final String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
			mapMessage.put(typeMessage, typeMessageValeur);
		}
		return sortMapAccordingToValues(mapMessage);
	}


	/**
	 * Initialise la map des types de message
	 */
	public Map<String, String> initMapTypeMessage(final IdentificationContribuableEtatFilter filter, TypeDemande... typesDemande) {
		final Map<String, String> mapMessage = new HashMap<>();
		final Collection<String> typesMessage = identCtbService.getTypeMessages(filter, typesDemande);
		return getTypeMessageAndLibelle(mapMessage, typesMessage);
	}

	/**
	 * Initialise la map des utilisateurs traitants
	 *
	 * @return une map
	 */

	public Map<String, String> initMapUser() {

		final Map<String, String> mapUtilisateur = new HashMap<>();
		final List<String> listVisaUser = identCtbService.getTraitementUser();

		for (String visaUser : listVisaUser) {
			IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(visaUser);
			visaUser = identifiantUtilisateur.getVisa();
			String nom = identifiantUtilisateur.getNomComplet();
			mapUtilisateur.putIfAbsent(visaUser, nom);

		}
		//Ajout du user de traitement automatique ignoré dans la requete
		mapUtilisateur.put("Traitement automatique", "Traitement automatique");
		return sortMapAccordingToValues(mapUtilisateur);
	}

	/**
	 * Initialise la map des types du message
	 *
	 * @return une map
	 * @param filter
	 */

	public Map<String, String> initMapEmetteurId(IdentificationContribuableEtatFilter filter) {
		final Collection<String> emetteurs = identCtbService.getEmetteursId(filter);
		return getEmetteursIdAndLibelle(emetteurs);

	}

	private Map<String, String> getEmetteursIdAndLibelle(Collection<String> emetteurs) {
		// [SIFISC-5847] Le tri des identifiants d'émetteurs doit être <i>case-insensitive</i>
		// [SIFISC-5847] Il ne faut pas tenir compte des espaces initiaux dans le tri et l'affichage des libellés
		final StringComparator comparator = new StringComparator(false, false, false, null);
		final Map<String, String> allEmetteurs = new TreeMap<>(comparator);
		for (String emetteur : emetteurs) {
			final String libelle = StringUtils.trimToEmpty(emetteur);
			allEmetteurs.put(emetteur, libelle);
		}
		return sortMapAccordingToValues(allEmetteurs, comparator);
	}

	public Map<ErreurMessage, String> initErreurMessage() {
		return initMapEnum(ApplicationConfig.masterKeyErreurMessage, ErreurMessage.class);
	}


	public Map<Etat, String> initMapEtatMessageArchive() {
		return initMapEtatMessage(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES);
	}

	/**
	 * Initialise la map des periodes fiscales
	 *
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale(IdentificationContribuableEtatFilter filter) {
		final Collection<Integer> periodes = identCtbService.getPeriodesFiscales(filter);
		return getPeriodesValues(periodes);
	}

	private Map<Integer, String> getPeriodesValues(Collection<Integer> periodes) {
		final Map<Integer, String> allPeriodeFiscale = new TreeMap<>();
		for (Integer periode : periodes) {
			allPeriodeFiscale.put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}

	public Map<String, Object> getMaps(IdentificationContribuableEtatFilter filter) {
		final Map<String, Object> data = new HashMap<>(7);
		data.put(PERIODE_FISCALE_MAP_NAME, initMapPeriodeFiscale(filter));
		data.put(EMETTEUR_MAP_NAME, initMapEmetteurId(filter));
		data.put(ETAT_MESSAGE_MAP_NAME, initMapEtatMessage());
		data.put(TYPE_MESSAGE_MAP_NAME, initMapTypeMessage(IdentificationContribuableEtatFilter.TOUS));
		data.put(PRIORITE_EMETTEUR_MAP_NAME, initMapPrioriteEmetteur());
		data.put(ERREUR_MESSAGE_MAP_NAME, initErreurMessage());
		data.put(TRAITEMENT_USER_MAP_NAME, initMapUser());
		return data;
	}

	/**
	 * Tri les éléments dans une map (i.e. l'itérateur fournira les éléments dans cet ordre) par rapport au tri naturel de la valeur
	 *
	 * @param source map dont les éléments doivent être triés
	 * @param <K>    type des clés de la map
	 * @param <V>    type des valeurs de la map (doit implémenter {@link Comparable})
	 * @return une nouvelle map triée
	 */
	private static <K, V extends Comparable<V>> Map<K, V> sortMapAccordingToValues(Map<K, V> source) {
		return sortMapAccordingToValues(source, new Comparator<V>() {
			@Override
			public int compare(V v1, V v2) {
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
	}

	/**
	 * Tri les éléments dans une map (i.e. l'itérateur fournira les éléments dans cet ordre) par rapport au tri de la valeur imposé par le comparateur donné
	 *
	 * @param source map dont les éléments doivent être triés
	 * @param valueComparator comparateur utilisé pour le tri
	 * @param <K>    type des clés de la map
	 * @param <V>    type des valeurs de la map
	 * @return une nouvelle map triée
	 */
	private static <K, V> Map<K, V> sortMapAccordingToValues(Map<K, V> source, final Comparator<V> valueComparator) {
		if (source == null) {
			return null;
		}
		final List<Map.Entry<K, V>> content = new ArrayList<>(source.entrySet());
		content.sort(new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return valueComparator.compare(o1.getValue(), o2.getValue());
			}
		});
		final Map<K, V> sorted = new LinkedHashMap<>(source.size());
		for (Map.Entry<K, V> item : content) {
			sorted.put(item.getKey(), item.getValue());
		}
		return sorted;
	}
}
