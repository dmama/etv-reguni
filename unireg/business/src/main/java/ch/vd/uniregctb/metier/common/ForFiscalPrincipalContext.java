package ch.vd.uniregctb.metier.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

/**
 * Un for fiscal principal et son contexte, c'est-à-dire les fors fiscaux principaux qui précèdent et qui suivent immédiatement (et par extension en une suite continue).
 */
public final class ForFiscalPrincipalContext {

	private final ForFiscalPrincipal current;
	private final List<ForFiscalPrincipal> nexts;
	private final List<ForFiscalPrincipal> previouses;

	private ForFiscalPrincipalContext(ForFiscalPrincipal current, List<ForFiscalPrincipal> nexts, List<ForFiscalPrincipal> previouses) {
		this.current = current;
		this.nexts = nexts;
		this.previouses = previouses;
	}

	/**
	 * Construit le contenu d'un context à partir d'une fenêtre glissante sur les fors fiscaux (qui ne se touchent pas forcément)
	 * @param snapshot la vue actuelle de la fenêtre glissante
	 */
	public ForFiscalPrincipalContext(MovingWindow.Snapshot<ForFiscalPrincipal> snapshot) {
		current = snapshot.getCurrent();

		// remplissage avec les fors précédents qui se touchent
		final List<ForFiscalPrincipal> allPrevious = snapshot.getAllPrevious();
		previouses = new ArrayList<>(allPrevious.size());
		ForFiscalPrincipal ref = current;
		for (ForFiscalPrincipal candidate : allPrevious) {
			if (DateRangeHelper.isCollatable(candidate, ref)) {
				previouses.add(candidate);
				ref = candidate;
			}
			else {
				break;
			}
		}

		// remplissage avec les fors suivants qui se touchent
		final List<ForFiscalPrincipal> allNext = snapshot.getAllNext();
		nexts = new ArrayList<>(allNext.size());
		ref = current;
		for (ForFiscalPrincipal candidate : allNext) {
			if (DateRangeHelper.isCollatable(ref, candidate)) {
				nexts.add(candidate);
				ref = candidate;
			}
			else {
				break;
			}
		}
	}

	public ForFiscalPrincipal getCurrent() {
		return current;
	}

	public ForFiscalPrincipal getNext() {
		return nexts.isEmpty() ? null : nexts.get(0);
	}

	public  ForFiscalPrincipal getPrevious() {
		return previouses.isEmpty() ? null : previouses.get(0);
	}

	public boolean hasNext() {
		return !nexts.isEmpty();
	}

	public boolean hasPrevious() {
		return !previouses.isEmpty();
	}

	/**
	 * @return Tous les fors principaux après le for courant qui se touchent en une suite continue
	 */
	public List<ForFiscalPrincipal> getAllNext() {
		return nexts;
	}

	/**
	 * @return Tous les fors principaux avant le for courant qui se touchent en une suite continue (du plus proche au plus lointain)
	 */
	public List<ForFiscalPrincipal> getAllPrevious() {
		return previouses;
	}

	public ForFiscalPrincipalContext slideToNext() {
		if (current == null) {
			throw new IllegalStateException("Je refuse de glisser vers l'abîme !");
		}
		final List<ForFiscalPrincipal> newNext = nexts.size() > 1 ? new ArrayList<>(nexts.subList(1, nexts.size())) : Collections.<ForFiscalPrincipal>emptyList();
		final List<ForFiscalPrincipal> newPrevious = new ArrayList<>(previouses.size() + 1);
		newPrevious.add(current);
		newPrevious.addAll(previouses);
		final ForFiscalPrincipal newCurrent = nexts.isEmpty() ? null : nexts.get(0);
		return new ForFiscalPrincipalContext(newCurrent, newNext, newPrevious);
	}

	public ForFiscalPrincipalContext slideToPrevious() {
		if (current == null) {
			throw new IllegalStateException("Je refuse de glisser vers l'abîme !");
		}
		final List<ForFiscalPrincipal> newNext = new ArrayList<>(nexts.size() + 1);
		newNext.add(current);
		newNext.addAll(nexts);
		final List<ForFiscalPrincipal> newPrevious = previouses.size() > 1 ? new ArrayList<>(previouses.subList(1, previouses.size())) : Collections.<ForFiscalPrincipal>emptyList();
		final ForFiscalPrincipal newCurrent = previouses.isEmpty() ? null : previouses.get(0);
		return new ForFiscalPrincipalContext(newCurrent, newNext, newPrevious);
	}
}
