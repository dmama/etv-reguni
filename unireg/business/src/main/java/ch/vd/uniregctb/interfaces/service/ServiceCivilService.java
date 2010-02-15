package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;

public interface ServiceCivilService {

	public static final String SERVICE_NAME = "ServiceCivil";

	/**
	 * Retourne les adresses civiles valide à la date donnée.
	 *
	 * @param noIndividu l'individu dont on recherche les adresses.
	 * @param date       la date de référence (attention, la précision est l'année !), ou null pour obtenir toutes les adresses existantes.
	 * @param strict     si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                   données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles de l'individu spécifié.
	 */
	AdressesCiviles getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException;

	/**
	 * Retourne l'historique des adresses civiles.
	 *
	 * @param noIndividu l'individu dont on recherche les adresses.
	 * @param strict     si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                   données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'historique des adresses civiles de l'individu spécifié.
	 */
	AdressesCivilesHisto getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException;

	/**
	 * Retourne la liste des adresses, valides <b>jusqu'à</b> l'année en paramètre, pour un individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la liste des adresses, valides jusqu'à l'année, de l'individu.
	 */
	Collection<Adresse> getAdresses(long noIndividu, int annee);

	/**
	 * Retourne l'individu, valide <b>jusqu'à</b> l'année en paramètre, identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants : <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée.
	 */
	Individu getIndividu(long noIndividu, int annee);

	/**
	 * Même chose que {@link #getIndividu(long, int)} avec la possibilité de demander des parties supplémentaires.
	 */
	Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties);

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param date         la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties);

	/**
	 * Retourne la liste des nationalités, valides <b>jusqu'à</b> l'année en paramètre, de l'individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la liste des nationalités de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Collection<Nationalite> getNationalites(long noIndividu, int annee);

	/**
	 * Retourne l'origine, valides <b>jusqu'à</b> l'année en paramètre, d'un individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return l'origine de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Origine getOrigine(long noIndividu, int annee);

	/**
	 * Retourne la liste des permis, valides <b>jusqu'à</b> l'année en paramètre, de l'individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la liste des permis de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Collection<Permis> getPermis(long noIndividu, int annee);

	/**
	 * Retourne la tutelle, valide durant l'année en paramètre, à laquelle l'individu attendu est soumis, l'individu attendu étant identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la tutelle, valide durant l'année en paramètre, à laquelle l'individu attendu est soumis.
	 */
	Tutelle getTutelle(long noIndividu, int annee);

	/**
	 * @param noIndividu
	 * @param date       la date de référence, ou null pour obtenir l'état-civil actif
	 * @return l'état civil actif d'un individu à une date donnée.
	 */
	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date);

	/**
	 * @return le permis actif d'un individu à une date donnée.
	 * @date date la date de validité du permis, ou <b>null</b> pour obtenir le dernis permis valide.
	 */
	public Permis getPermisActif(long noIndividu, RegDate date);

	/**
	 * Utile pour la sous-classe Proxy
	 *
	 * @param target
	 */
	public void setUp(ServiceCivilService target);

	public void tearDown();

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec des données externes.
	 */
	boolean isWarmable();

	/**
	 * Préchauffe le cache du service civil avec les individus spécifiés.
	 * <p/>
	 * Note: cette méthode n'a aucun effet sur la plupart des implémentations des services civils. Seul l'implémentation de caching peut en faire quelque chose.
	 *
	 * @param individus les individus à stocker dans le cache du service civil.
	 * @param date      la date de validité des individus spécifiés.
	 * @param parties   les parties renseignées sur les individus
	 */
	void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties);

	/**
	 * Enregistre un listener.
	 */
	public void register(CivilListener listener);

	/**
	 * Notifie au service que l'individu spécifié a changé dans le registre civil.
	 *
	 * @param numero le numéro d'individu
	 */
	void onIndividuChange(long numero);
}
