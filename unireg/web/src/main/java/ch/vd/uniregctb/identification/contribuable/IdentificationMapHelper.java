package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.ui.Model;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

public class IdentificationMapHelper extends CommonMapHelper {

	private IdentificationContribuableService identCtbService;

	private Map<ErreurMessage, String> mapErreurMessage;

	private Map<Etat, String> mapEtatMessage;

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
	public Map<PrioriteEmetteur, String> initMapPrioriteEmetteur(final boolean isTraite) {

		final Map<PrioriteEmetteur, String> allPrioriteEmetteur = new TreeMap<PrioriteEmetteur, String>();

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
		etatsMessages.remove(Etat.A_EXPERTISER_SUSPENDU);
		etatsMessages.remove(Etat.A_TRAITER_MAN_SUSPENDU);
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
		final Map<Etat, String> mapEtat = new EnumMap<Etat, String>(Etat.class);
		final Collection<Etat> typesMessage = identCtbService.getEtats(isTraite ? IdentificationContribuableEtatFilter.SEULEMENT_TRAITES : IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);

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
		final Map<String, String> mapMessage = new HashMap<String, String>();
		final Collection<String> typesMessage = identCtbService.getTypesMessages(IdentificationContribuableEtatFilter.TOUS);
		for (String typeMessage : typesMessage) {
			final String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
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
		final Map<String, String> mapMessage = new HashMap<String, String>();
		final Collection<String> typesMessage = identCtbService.getTypeMessages(typeDemande, isTraite ? IdentificationContribuableEtatFilter.SEULEMENT_TRAITES : IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
		for (String typeMessage : typesMessage) {
			final String typeMessageValeur = this.getMessageSourceAccessor().getMessage(ApplicationConfig.masterKeyTypeMessage + typeMessage);
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

		final Map<String, String> mapUtilisateur = new HashMap<String, String>();
		final List<String> listVisaUser = identCtbService.getTraitementUser();

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
		// [SIFISC-5847] Il ne faut pas tenir compte des espaces initiaux dans le tri et l'affichage des libellés
		final StringComparator comparator = new StringComparator(false, false, false, null);
		final Map<String, String> allEmetteurs = new TreeMap<String, String>(comparator);
		final Collection<String> emetteurs = identCtbService.getEmetteursId(isTraite ? IdentificationContribuableEtatFilter.SEULEMENT_TRAITES : IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);

		for (String emetteur : emetteurs) {
			final String libelle = StringUtils.trimToEmpty(emetteur);
			allEmetteurs.put(emetteur, libelle);
		}
		return sortMapAccordingToValues(allEmetteurs, comparator);
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
		final Collection<Integer> periodes = identCtbService.getPeriodesFiscales(isTraite ? IdentificationContribuableEtatFilter.SEULEMENT_TRAITES : IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);

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
		final Collection<Integer> periodes = identCtbService.getPeriodesFiscales(IdentificationContribuableEtatFilter.TOUS);

		for (Integer periode : periodes) {
			allPeriodeFiscale.put(periode, Integer.toString(periode));
		}

		return allPeriodeFiscale;
	}

	public Map<String, Object> getMaps(final boolean isTraite) {
		final Map<String, Object> data = new HashMap<String, Object>(7);
		data.put(PERIODE_FISCALE_MAP_NAME, initMapPeriodeFiscale(isTraite));
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
	 * @param comparator comparateur utilisé pour le tri
	 * @param <K>    type des clés de la map
	 * @param <V>    type des valeurs de la map
	 * @return une nouvelle map triée
	 */
	private static <K, V> Map<K, V> sortMapAccordingToValues(Map<K, V> source, final Comparator<V> comparator) {
		if (source == null) {
			return null;
		}
		final List<Map.Entry<K, V>> content = new ArrayList<Map.Entry<K, V>>(source.entrySet());
		Collections.sort(content, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				final V v1 = o1.getValue();
				final V v2 = o2.getValue();
				return comparator.compare(v1, v2);
			}
		});
		final Map<K, V> sorted = new LinkedHashMap<K, V>(source.size());
		for (Map.Entry<K, V> item : content) {
			sorted.put(item.getKey(), item.getValue());
		}
		return sorted;
	}
}
