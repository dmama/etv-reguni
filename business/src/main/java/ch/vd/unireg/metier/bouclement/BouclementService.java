package ch.vd.unireg.metier.bouclement;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Service de fourniture d'information autour des bouclements / exercices commerciaux + mise-à-jour des données relatives sur les entreprises.
 */
public interface BouclementService {

	/**
	 * @param bouclements liste des entités {@link ch.vd.unireg.tiers.Bouclement} d'une entreprise
	 * @param dateReference date de référence
	 * @param dateReferenceAcceptee <code>true</code> si la date de référence est éligible comme prochaine date, <code>false</code> sinon
	 * @return la date de prochain bouclement (après la date de référence)
	 */
	RegDate getDateProchainBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee);

	/**
	 * @param bouclements liste des entités {@link ch.vd.unireg.tiers.Bouclement} d'une entreprise
	 * @param dateReference date de référence
	 * @param dateReferenceAcceptee <code>true</code> si la date de référence est éligible comme dernière date, <code>false</code> sinon
	 * @return la date de bouclement précédent (avant la date de référence)
	 */
	RegDate getDateDernierBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee);

	/**
	 * @param bouclements liste des entités {@link ch.vd.unireg.tiers.Bouclement} d'une entreprise
	 * @param range range de dates (<b>ne doit pas être ouvert ni à gauche ni à droite !</b>)
	 * @param intersecting <code>true</code> si les exercices renvoyés sont ceux qui intersectent le range donné, sans rognage, <code>false</code> s'il faut rogner
	 * @return la liste ordonnée des exercices commerciaux <b>bornés</b> avec le range donné
	 */
	List<ExerciceCommercial> getExercicesCommerciaux(Collection<Bouclement> bouclements, @NotNull DateRange range, boolean intersecting);

	/**
	 * Utilisable dans la migration des PM du mainframe en tout cas, pour construire une liste de {@link Bouclement} à partir d'une série de dates
	 * (la collection fournie en paramètre peut tout aussi bien contenir des dates de bouclement passées que la date de prochain bouclement)
	 * @param datesBouclements les dates (connues ou futures) de bouclement à prendre en compte
	 * @param periodeMoisFinale la période (en mois) des bouclements à prévoir après la dernière date fournie (en général 12 mois...)
	 * @return liste d'entités (encore transientes, non-liées à une entreprise) de {@link Bouclement} qui permettrait de regénérer les dates founies en entrée
	 */
	List<Bouclement> extractBouclementsDepuisDates(Collection<RegDate> datesBouclements, int periodeMoisFinale);

	/**
	 * Cette méthode permet de renseigner (si elle ne l'était pas) ou de corriger la date de début du premier exercice commercial.
	 * <p/>
	 * <b>Note:</b> dans le cas de la correction de la date de début, la valeur choisie ne doit pas définir un premier exercice commercial qui
	 * durerait plus qu'une année.
	 *
	 * @param entreprise   une entreprise
	 * @param nouvelleDate la date de début du premier exercice commercial à renseigner.
	 * @throws BouclementException si le date de début provoque un premier exercice commercial plus grand qu'une année
	 */
	void setDateDebutPremierExerciceCommercial(@NotNull Entreprise entreprise, @NotNull RegDate nouvelleDate) throws BouclementException;

	/**
	 * Cette méthode permet de corriger la date de début du premier exercice commercial. Contrairement à la méthode {@link #setDateDebutPremierExerciceCommercial(Entreprise, RegDate)}, la date de début du premier exercice commercial peut être
	 * déplacée de plusieurs années. La nouvelle date proposée doit cependant être antérieur à la date de début du premier exercice commercial actuel. Si nécessaire, des nouveaux ancrages de bouclements seront ajoutés.
	 *
	 * @param entreprise   une entreprise
	 * @param nouvelleDate la date de début du premier exercice commercial à renseigner.
	 * @throws BouclementException si la nouvelle date proposée provoque un changement incompatible avec les exercices commerciaux actuels.
	 */
	void corrigeDateDebutPremierExerciceCommercial(@NotNull Entreprise entreprise, @NotNull RegDate nouvelleDate) throws BouclementException;

	/**
	 * Change la date de fin d'un bouclement.
	 *
	 * @param entreprise   une entreprise
	 * @param ancienneDate l'ancienne date de fin de bouclement
	 * @param nouvelleDate la nouvelle date de fin de bouclement
	 * @throws BouclementException si le déplacement de la date de fin est impossible (par exemple parce qu'il existe une DI dans la période)
	 */
	void changeDateFinBouclement(@NotNull Entreprise entreprise, @NotNull RegDate ancienneDate, @NotNull RegDate nouvelleDate) throws BouclementException;
}
