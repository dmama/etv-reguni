package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;

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
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données civiles
	 */
	AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException;

	/**
	 * Retourne l'historique des adresses civiles.
	 *
	 * @param noIndividu l'individu dont on recherche les adresses.
	 * @param strict     si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                   données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'historique des adresses civiles de l'individu spécifié; ou <b>null</b> si l'individu n'existe pas.
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données civiles
	 */
	AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException;

	/**
	 * Construit la liste des communes de domiciles connues pour l'individu donné, et ce depuis une date de référence
	 *
	 * @param depuis        date de référence à partir de laquelle on cherche les domiciles successifs de l'individu
	 * @param noIndividu    l'individu dont on cherche les communes de domicile
	 * @param strict        si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                      données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @param seulementVaud <code>true</code> si on ne s'intéresse qu'aux communes vaudoises (i.e. commune <code>null</code> pour HC/HS)
	 * @return une liste des communes de domiciles fréquentées depuis la date de référence
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données civiles
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException
	 *          en cas d'erreur dans les données d'infrastructure
	 */
	List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException;

	/**
	 * Retourne l'individu, valide <b>jusqu'à</b> l'année en paramètre, identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée; ou <b>null</b> si l'individu n'existe pas.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération de l'individu.
	 */
	Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Retourne l'individu, valide <b>jusqu'à</b> l'année en paramètre, concerné par l'événement civil dont l'identifié est donné en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param eventId    l'identifiant technique de l'événement civil de référence
	 * @param date       la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée; ou <b>null</b> si l'individu n'existe pas.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération de l'individu.
	 */
	Individu getIndividuByEvent(long eventId, @Nullable RegDate date, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Retourne l'individu conjoint valide <b>à la date</b> passée en paramètre, de l'indivu dont le numéro est  en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 * autrement dit l'abscence de conjoint à la date renseignée
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche le conjoint.
	 * @param date à laquelle on veut savoir si l'indivdu a un conjoint valide.
	 * @return l'individu conjoint populé avec les données valides à la date spécifiée.
	 */
	Individu getConjoint(Long noIndividuPrincipal, @Nullable RegDate date);

	/**
	 * Retourne le numéro de l'individu conjoint valide <b>à la date</b> passée en paramètre, de l'indivu dont le numéro est  en paramètre.
	 * <p/>
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche le conjoint.
	 * @param date à laquelle on veut savoir si l'indivdu a un conjoint valide.
	 * @return  le numéro de l'individu conjoint valide à la date spécifiée.
	 */
	Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date);

	/**
	 * Retourne l'ensemble des numéros d'individu des conjoints passés et présent d'un individu donné.
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche les conjoints.
	 * @return l'ensemble de numéros d'individus, qui peut être vide si l'individu n'a jamais été marié.
	 */
	Set<Long> getNumerosIndividusConjoint(Long noIndividuPrincipal);

	/**
	 * Retourne l'ensemble des numéros d'individu des parents d'un individu donné.
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche les parents.
	 * @return l'ensemble de numéros d'individus.
	 */
	Set<Long> getNumerosIndividusParents(Long noIndividuPrincipal);

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param date         la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération d'un ou plusieurs individus.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Retourne la nationalité d'un individu à une date donnée.
	 * <p/>
	 * <b>Note:</b> la nationalité est une des rares informations non-historisée sur les individus exposés par RcPers. C'est pourquoi cette méthode existe.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de référence
	 * @return la nationalité de l'individu à la date demandée; ou <b>null</b> si l'individu n'existe pas.
	 */
	Nationalite getNationaliteAt(long noIndividu, @Nullable RegDate date);

	/**
	 * Retourne les origines, valides <b>jusqu'à</b> la date en paramètre, d'un individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de validité des origines demandées
	 * @return les origines de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Collection<Origine> getOrigines(long noIndividu, @Nullable RegDate date);

	/**
	 * Retourne la liste des permis, valides <b>jusqu'à</b> la date en paramètre, de l'individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date à laquelle on va connaître le permis
	 * @return la liste des permis de l'individu, valides jusqu'à la date spécifiée.
	 */
	Collection<Permis> getPermis(long noIndividu, @Nullable RegDate date);

	/**
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de référence, ou null pour obtenir l'état-civil actif
	 * @return l'état civil actif d'un individu à une date donnée.
	 */
	EtatCivil getEtatCivilActif(long noIndividu, @Nullable RegDate date);

	/**
	 * Retourne les nom et prénoms pour l'adressage de l'individu spécifié.
	 *
	 * @param individu un individu
	 * @return le prénom + le nom du l'individu
	 */
	String getNomPrenom(Individu individu);

	/**
	 * Retourne les nom et prénoms de l'individu spécifié, dans deux champs distincts
	 *
	 * @param individu un individu
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de l'individu (ou {@link NomPrenom#VIDE} si l'individu est inconnu)
	 */
	NomPrenom getDecompositionNomPrenom(Individu individu);

	/**
	 * Renvoie un individu correspondant à l'événement donné
	 * <p/>
	 * <b>Attention : </b> les relations ne sont pas renseignées sur cet individu
	 * @param eventId identifiant de l'événement
	 * @return l'individu correspondant à l'état juste après le traitement civil de l'événement, ou <code>null</code> si l'id ne correspond à rien
	 */
	IndividuApresEvenement getIndividuAfterEvent(long eventId);

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec un appel à getIndividus().
	 */
	boolean isWarmable();

	/**
	 * Active ou non le dump des individus récupérés dans le registre civil <b>pour le thread courant</b>.
	 *
	 * @param value <b>vrai</b> si le dump est activé; <b>faux</b> s'il n'est pas activé.
	 */
	void setIndividuLogging(boolean value);
}
