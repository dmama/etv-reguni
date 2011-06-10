package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

/**
 * Itérateur qui détermine et retourne tous les événements existants sur des fors fiscaux pour une période données. Un événement correspondant à une ouverture ou une fermeture de for fiscal.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementForsIterator implements Iterator<EvenementFors> {

	private EvenementFors next;

	private int indexDebut = 0;
	private int indexFin = 0;

	/**
	 * Contient tous les fors fiscaux de la période + ceux venant immédiatement avant et immédiatement après
	 */
	private final List<ForFiscal> allFors;

	/**
	 * Contient tous les fors fiscaux triés par ordre croissant des dates d'ouverture.
	 */
	private final List<ForFiscal> forsParDatesDebut;

	/**
	 * Contient les fors fiscaux triés par ordre croissant des dates de fermeture, mais seulement ceux ayant une date de fin non-nulle.
	 */
	private final List<ForFiscal> forsParDatesFin;

	public EvenementForsIterator(DecompositionFors fors) {

		final List<ForFiscal> forsDansLaPeriode = extractForsDansPeriode(fors);
		this.forsParDatesDebut = extractForsQuiSOuvrent(forsDansLaPeriode, fors.debut);
		this.forsParDatesFin = extractForsQuiSeFerment(forsDansLaPeriode, fors.fin);
		this.allFors = extractAllFors(fors);

		fetchNext();
	}

	/**
	 * Extrait la liste de tous les fors fiscaux actifs dans la période plus les fors actifs juste avant et juste après la période (de manière à générer des listes de fors 'veille' et 'lendemain'
	 * complètes sur les événements (voir extractNextEvent)).
	 *
	 * @param fors la décomposition des fors
	 * @return une liste de fors fiscaux, qui peut être vide
	 */
	private ArrayList<ForFiscal> extractAllFors(DecompositionFors fors) {

		final Set<ForFiscal> forsPeriode = new HashSet<ForFiscal>();
		forsPeriode.addAll(fors.principauxDansLaPeriode);
		forsPeriode.addAll(fors.secondairesDansLaPeriode);

		if (fors.principalAvantLaPeriode != null) {
			forsPeriode.add(fors.principalAvantLaPeriode);
		}
		if (fors.principalApresLaPeriode != null) {
			forsPeriode.add(fors.principalApresLaPeriode);
		}

		forsPeriode.addAll(fors.secondairesAvantLaPeriode);
		forsPeriode.addAll(fors.secondairesApresLaPeriode);

		return new ArrayList<ForFiscal>(forsPeriode);
	}

	/**
	 * Crée la liste des fors fiscaux qui se ferment dans la période triés par ordre croissant de dates.
	 *
	 * @param forsDansLaPeriode tous les fors fiscaux dans la période
	 * @param finPeriode        la date de fin de la période
	 * @return une liste de fors fiscaux, qui peut être vide.
	 */
	private List<ForFiscal> extractForsQuiSeFerment(List<ForFiscal> forsDansLaPeriode, RegDate finPeriode) {

		List<ForFiscal> fors = new ArrayList<ForFiscal>(forsDansLaPeriode.size());

		for (ForFiscal f : forsDansLaPeriode) {
			if (RegDateHelper.isBeforeOrEqual(f.getDateFin(), finPeriode, NullDateBehavior.LATEST)) {
				fors.add(f);
			}
		}
		Collections.sort(fors, new Comparator<ForFiscal>() {
			@Override
			public int compare(ForFiscal o1, ForFiscal o2) {
				if (o1.getDateFin() == null && o2.getDateFin() == null) {
					return 0;
				}
				if (o1.getDateFin() == null && o2.getDateFin() != null) {
					return 1;
				}
				if (o1.getDateFin() != null && o2.getDateFin() == null) {
					return -1;
				}
				return o1.getDateFin().compareTo(o2.getDateFin());
			}
		});

		return fors;
	}

	/**
	 * Crée la liste des fors fiscaux qui s'ouvrent dans la période triés par ordre croissant de dates
	 *
	 * @param forsDansLaPeriode tous les fors fiscaux dans la période
	 * @param debutPeriode      la date de début de la période
	 * @return une liste de fors fiscaux, qui peut être vide.
	 */
	private List<ForFiscal> extractForsQuiSOuvrent(List<ForFiscal> forsDansLaPeriode, RegDate debutPeriode) {

		List<ForFiscal> fors = new ArrayList<ForFiscal>(forsDansLaPeriode.size());

		for (ForFiscal f : forsDansLaPeriode) {
			if (f.getDateDebut().isAfterOrEqual(debutPeriode)) {
				fors.add(f);
			}
		}
		Collections.sort(fors, new Comparator<ForFiscal>() {
			@Override
			public int compare(ForFiscal o1, ForFiscal o2) {
				return o1.getDateDebut().compareTo(o2.getDateDebut());
			}
		});

		return fors;
	}

	/**
	 * Exrtait les fors fiscaux qui existent dans la période
	 *
	 * @param fors la décomposition des fors
	 * @return une liste de fors fiscaux, qui peut être vide.
	 */
	private List<ForFiscal> extractForsDansPeriode(DecompositionFors fors) {
		final List<ForFiscal> forsDansLaPeriode = new ArrayList<ForFiscal>();
		forsDansLaPeriode.addAll(fors.principauxDansLaPeriode);
		forsDansLaPeriode.addAll(fors.secondairesDansLaPeriode);
		return forsDansLaPeriode;
	}

	private void fetchNext() {
		final RegDate dateEvent = determineNextEventDate();
		if (dateEvent == null) {
			next = null;
		}
		else {
			next = extractNextEvent(dateEvent);
		}
	}

	private RegDate determineNextEventDate() {

		final ForFiscal nextDebut = (indexDebut < forsParDatesDebut.size() ? forsParDatesDebut.get(indexDebut) : null);
		final ForFiscal nextFin = (indexFin < forsParDatesFin.size() ? forsParDatesFin.get(indexFin) : null);

		RegDate dateEvent;

		if (nextDebut == null && nextFin == null) {
			dateEvent = null;   // terminé
		}
		else if (nextDebut == null) {
			dateEvent = nextFin.getDateFin();
		}
		else if (nextFin == null) {
			dateEvent = nextDebut.getDateDebut();
		}
		else {
			dateEvent = RegDateHelper.minimum(nextDebut.getDateDebut(), nextFin.getDateFin(), NullDateBehavior.LATEST);
		}

		return dateEvent;
	}

	private EvenementFors extractNextEvent(RegDate dateEvent) {

		// Extrait les fors ouverts, fermés et actifs à la date donnée
		final ForsAt forsOuverts = extractForsOuvertsAt(forsParDatesDebut, dateEvent);
		final ForsAt forsFermes = extractForsFermesAt(forsParDatesFin, dateEvent);
		final ForsAt forsActifs = extractForsActifsAt(allFors, dateEvent);
		final ForsAt forsActifsVeille = extractForsActifsAt(allFors, dateEvent.getOneDayBefore());
		final ForsAt forsActifsLendemain = extractForsActifsAt(allFors, dateEvent.getOneDayAfter());

		// On incrémente les indexes maintenant que ces fors ont été extraits
		indexDebut += forsOuverts.count;
		indexFin += forsFermes.count;

		return new EvenementFors(dateEvent, forsOuverts, forsFermes, forsActifs, forsActifsVeille, forsActifsLendemain);
	}

	private static ForsAt extractForsActifsAt(List<ForFiscal> fors, RegDate dateEvent) {

		ForFiscalPrincipal principalActif = null;
		List<ForFiscalSecondaire> secondairesActifs = null;

		for (ForFiscal f : fors) {
			if (f.isValidAt(dateEvent)) {
				if (f instanceof ForFiscalPrincipal) {
					Assert.isNull(principalActif);
					principalActif = (ForFiscalPrincipal) f;
				}
				else {
					if (secondairesActifs == null) {
						secondairesActifs = new ArrayList<ForFiscalSecondaire>();
					}
					secondairesActifs.add((ForFiscalSecondaire) f);
				}
			}
		}

		if (principalActif == null && secondairesActifs != null) {
			final String message = "Incohérence des données. Le contribuable = " + fors.get(0).getTiers().getNumero() + " à la date " + dateEvent +
					" possède un ou plusieurs fors secondaires et pas de for principal.";
			throw new IllegalArgumentException(message);
		}
		
		return new ForsAt(principalActif, secondairesActifs);
	}

	private static ForsAt extractForsFermesAt(List<ForFiscal> fors, RegDate dateEvent) {

		ForFiscalPrincipal principalFerme = null;
		List<ForFiscalSecondaire> secondairesFermes = null;

		for (ForFiscal ff : fors) {
			if (ff.getDateFin() == dateEvent) {
				if (ff instanceof ForFiscalPrincipal) {
					Assert.isNull(principalFerme);
					principalFerme = (ForFiscalPrincipal) ff;
				}
				else {
					if (secondairesFermes == null) {
						secondairesFermes = new ArrayList<ForFiscalSecondaire>();
					}
					secondairesFermes.add((ForFiscalSecondaire) ff);
				}
			}
		}

		return new ForsAt(principalFerme, secondairesFermes);
	}

	private static ForsAt extractForsOuvertsAt(List<ForFiscal> fors, RegDate dateEvent) {

		ForFiscalPrincipal principal = null;
		List<ForFiscalSecondaire> secondaires = null;

		for (ForFiscal ff : fors) {
			if (ff.getDateDebut() == dateEvent) {
				if (ff instanceof ForFiscalPrincipal) {
					Assert.isNull(principal);
					principal = (ForFiscalPrincipal) ff;
				}
				else {
					if (secondaires == null) {
						secondaires = new ArrayList<ForFiscalSecondaire>();
					}
					secondaires.add((ForFiscalSecondaire) ff);
				}
			}
		}

		return new ForsAt(principal, secondaires);
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public EvenementFors next() {
		EvenementFors n = next;
		fetchNext();
		return n;
	}

	@Override
	public void remove() {
		throw new NotImplementedException();
	}
}
