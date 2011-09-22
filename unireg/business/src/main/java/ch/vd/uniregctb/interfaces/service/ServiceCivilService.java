package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
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
	 * CHECK_RCPERS (msi) : utilisé à 10 endroits, un peu partout.
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
	 * CHECK_RCPERS (msi) : utilisé dans l'adresse service et dans le web (affichage)
	 *
	 * @param noIndividu l'individu dont on recherche les adresses.
	 * @param strict     si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                   données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'historique des adresses civiles de l'individu spécifié.
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données civiles
	 */
	AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException;

	/**
	 * Construit la liste des communes de domiciles connues pour l'individu donné, et ce depuis une date de référence
	 *
	 * CHECK_RCPERS (msi) : utilisé uniquement dans l'événement d'arrivée.
	 *
	 * @param depuis        date de référence à partir de laquelle on cherche les domiciles successifs de l'individu
	 * @param noIndividu    l'individu dont on cherche les communes de domicile
	 * @param strict        si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les
	 *                      données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @param seulementVaud <code>true</code> si on ne s'intéresse qu'aux communes vaudoises (i.e. commune <code>null</code> pour HC/HS)
	 * @return une liste des communes de domiciles fréquentées depuis la date de référence
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données civiles
	 * @throws ServiceInfrastructureException
	 *          en cas d'erreur dans les données d'infrastructure
	 */
	List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException;

	/**
	 * Retourne la liste des adresses, valides <b>jusqu'à</b> l'année en paramètre, pour un individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * CHECK_RCPERS (msi) : utilisé en interne dans le service civil lui-même pour les méthodes getAdresses et getAdressesHisto
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
	 * CHECK_RCPERS (msi) : utilisé à 5 endroits
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée.
	 */
	Individu getIndividu(long noIndividu, int annee);

	/**
	 * Même chose que {@link #getIndividu(long, int)} avec la possibilité de demander des parties supplémentaires.
	 *
	 * CHECK_RCPERS (msi) : utilisé à plus de 20 endroits, un peu partout
	 */
	Individu getIndividu(long noIndividu, int annee, AttributeIndividu... parties);

	/**
	 * Retourne l'individu, valide <b>jusqu'à</b> l'année en paramètre, identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants : <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * CHECK_RCPERS (msi) : utilisé à plus de 20 endroits, un peu partout
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param date       la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée.
	 */
	Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties);

	/**
	 * Retourne l'individu conjoint valide <b>à la date</b> passée en paramètre, de l'indivu dont le numéro est  en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants : <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 * autrement dit l'abscence de conjoint à la date renseignée
	 *
	 * CHECK_RCPERS (msi) : utilisé à plus de 10 endroits, principalement dans les événements civils
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche le conjoint.
	 * @param date à laquelle on veut savoir si l'indivdu a un conjoint valide.
	 * @return l'individu conjoint populé avec les données valides à la date spécifiée.
	 */
	Individu getConjoint(Long noIndividuPrincipal, RegDate date);

	/**
	 * Retourne le numéro de l'individu conjoint valide <b>à la date</b> passée en paramètre, de l'indivu dont le numéro est  en paramètre.
	 * <p/>
	 *
	 * CHECK_RCPERS (msi) : utilisé en interne dans le service civil lui-même pour la méthode getConjoint
	 *
	 * @param noIndividuPrincipal le numéro technique de l'individu dont on cherche le conjoint.
	 * @param date à laquelle on veut savoir si l'indivdu a un conjoint valide.
	 * @return  le numéro de l'individu conjoint valide à la date spécifiée.
	 */
	Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date);

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * CHECK_RCPERS (msi) : utilisé à 7 endroits
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param annee        l'année de validité.
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, AttributeIndividu... parties);

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * CHECK_RCPERS (msi) : utilisé à 7 endroits
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param date         la date de validité des individus
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties);

	/**
	 * Retourne la liste des nationalités, valides <b>jusqu'à</b> l'année en paramètre, de l'individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * CHECK_RCPERS (msi) : historique pas nécessaire
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la liste des nationalités de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Collection<Nationalite> getNationalites(long noIndividu, int annee);

	/**
	 * Retourne les origines, valides <b>jusqu'à</b> l'année en paramètre, d'un individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * CHECK_RCPERS (msi) : uniquement pour l'affichage
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return les origines de l'individu, valides jusqu'à l'année spécifiée.
	 */
	Collection<Origine> getOrigines(long noIndividu, int annee);

	/**
	 * Retourne la liste des permis, valides <b>jusqu'à</b> l'année en paramètre, de l'individu identifié par le numéro en paramètre.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués.
	 * <p/>
	 * La liste retournée par ce service peut être vide, signifiant l'absence de données d'un point de vue métier pour les paramètres donnés.
	 *
	 * CHECK_RCPERS (msi) : permis courant pour le processing, historique pour l'affichage
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
	 * CHECK_RCPERS (msi) : utilisé uniquement pour le traitement de l'événement de tutelle
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param annee      l'année de validité.
	 * @return la tutelle, valide durant l'année en paramètre, à laquelle l'individu attendu est soumis.
	 */
	Tutelle getTutelle(long noIndividu, int annee);

	/**
	 * CHECK_RCPERS (msi) : utilisé à plus de 20 endroits
	 *
	 * @param noIndividu
	 * @param date       la date de référence, ou null pour obtenir l'état-civil actif
	 * @return l'état civil actif d'un individu à une date donnée.
	 */
	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date);

	/**
	 * CHECK_RCPERS (msi) : utilisé uniquement pour le testing
	 *
	 * @return le permis actif d'un individu à une date donnée.
	 * @param date la date de validité du permis, ou <b>null</b> pour obtenir le dernis permis valide.
	 */
	public Permis getPermisActif(long noIndividu, RegDate date);

	/**
	 * Retourne les nom et prénoms pour l'adressage de l'individu spécifié.
	 *
	 * CHECK_RCPERS (msi) : utilisé à 4 endroits
	 *
	 * @param individu un individu
	 * @return le prénom + le nom du l'individu
	 */
	public String getNomPrenom(Individu individu);

	/**
	 * Retourne les nom et prénoms de l'individu spécifié, dans deux champs distincts
	 *
	 * CHECK_RCPERS (msi) : utilisé à 3 endroits
	 *
	 * @param individu un individu
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de l'individu (ou {@link NomPrenom.VIDE} si l'individu est inconnu)
	 */
	public NomPrenom getDecompositionNomPrenom(Individu individu);

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec un appel à getIndividus().
	 */
	boolean isWarmable();

}
