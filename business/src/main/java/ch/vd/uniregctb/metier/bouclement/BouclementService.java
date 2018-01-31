package ch.vd.uniregctb.metier.bouclement;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Bouclement;

/**
 * Service de fourniture d'information autour des bouclements / exercices commerciaux...
 */
public interface BouclementService {

	/**
	 * @param bouclements liste des entités {@link ch.vd.uniregctb.tiers.Bouclement} d'une entreprise
	 * @param dateReference date de référence
	 * @param dateReferenceAcceptee <code>true</code> si la date de référence est éligible comme prochaine date, <code>false</code> sinon
	 * @return la date de prochain bouclement (après la date de référence)
	 */
	RegDate getDateProchainBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee);

	/**
	 * @param bouclements liste des entités {@link ch.vd.uniregctb.tiers.Bouclement} d'une entreprise
	 * @param dateReference date de référence
	 * @param dateReferenceAcceptee <code>true</code> si la date de référence est éligible comme dernière date, <code>false</code> sinon
	 * @return la date de bouclement précédent (avant la date de référence)
	 */
	RegDate getDateDernierBouclement(Collection<Bouclement> bouclements, @NotNull RegDate dateReference, boolean dateReferenceAcceptee);

	/**
	 * @param bouclements liste des entités {@link ch.vd.uniregctb.tiers.Bouclement} d'une entreprise
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
}
