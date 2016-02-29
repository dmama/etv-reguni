package ch.vd.uniregctb.metier.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

/**
 * Un for fiscal principal et son contexte, c'est-à-dire les fors fiscaux principaux qui précèdent et qui suivent immédiatement (et par extension en une suite continue).
 */
public final class ForFiscalPrincipalContext<FFP extends ForFiscalPrincipal> {

	private final FFP current;
	private final List<FFP> nexts;
	private final List<FFP> previouses;

	private ForFiscalPrincipalContext(FFP current, List<FFP> nexts, List<FFP> previouses) {
		this.current = current;
		this.nexts = nexts;
		this.previouses = previouses;
	}

	/**
	 * Construit le contenu d'un context à partir d'une fenêtre glissante sur les fors fiscaux (qui ne se touchent pas forcément)
	 * @param snapshot la vue actuelle de la fenêtre glissante
	 */
	public ForFiscalPrincipalContext(MovingWindow.Snapshot<FFP> snapshot) {
		current = snapshot.getCurrent();

		// remplissage avec les fors précédents qui se touchent
		final List<FFP> allPrevious = snapshot.getAllPrevious();
		previouses = new ArrayList<>(allPrevious.size());
		ForFiscalPrincipal ref = current;
		for (FFP candidate : allPrevious) {
			if (DateRangeHelper.isCollatable(candidate, ref)) {
				previouses.add(candidate);
				ref = candidate;
			}
			else {
				break;
			}
		}

		// remplissage avec les fors suivants qui se touchent
		final List<FFP> allNext = snapshot.getAllNext();
		nexts = new ArrayList<>(allNext.size());
		ref = current;
		for (FFP candidate : allNext) {
			if (DateRangeHelper.isCollatable(ref, candidate)) {
				nexts.add(candidate);
				ref = candidate;
			}
			else {
				break;
			}
		}
	}

	public FFP getCurrent() {
		return current;
	}

	public FFP getNext() {
		return nexts.isEmpty() ? null : nexts.get(0);
	}

	public FFP getPrevious() {
		return previouses.isEmpty() ? null : previouses.get(0);
	}

	public boolean hasNext() {
		return !nexts.isEmpty();
	}

	public boolean hasPrevious() {
		return !previouses.isEmpty();
	}

	/**
	 * @return la liste triée de tous les fors successifs du contexte (= couverture des fors qui se touchent)
	 */
	public List<FFP> getAll() {
		final List<FFP> all = new ArrayList<>(nexts.size() + previouses.size() + 1);
		for (FFP ffp : CollectionsUtils.revertedOrder(previouses)) {
			all.add(ffp);
		}
		all.add(current);
		all.addAll(nexts);
		return all;
	}

	/**
	 * @return Tous les fors principaux après le for courant qui se touchent en une suite continue
	 */
	public List<FFP> getAllNext() {
		return nexts;
	}

	/**
	 * @return Tous les fors principaux avant le for courant qui se touchent en une suite continue (du plus proche au plus lointain)
	 */
	public List<FFP> getAllPrevious() {
		return previouses;
	}

	/**
	 * @return une nouvelle instance de contexte (l'instance source n'est pas modifiée) correspondant à un glissement d'un for à droite (= vers le futur)
	 */
	public ForFiscalPrincipalContext<FFP> slideToNext() {
		if (current == null) {
			throw new IllegalStateException("Je refuse de glisser vers l'abîme !");
		}
		final List<FFP> newNext = nexts.size() > 1 ? new ArrayList<>(nexts.subList(1, nexts.size())) : Collections.<FFP>emptyList();
		final List<FFP> newPrevious = new ArrayList<>(previouses.size() + 1);
		newPrevious.add(current);
		newPrevious.addAll(previouses);
		final FFP newCurrent = nexts.isEmpty() ? null : nexts.get(0);
		return new ForFiscalPrincipalContext<>(newCurrent, newNext, newPrevious);
	}

	/**
	 * @return une nouvelle instance de contexte (l'instance source n'est pas modifiée) correspondant à un glissement d'un for à gauche (= vers le passé)
	 */
	public ForFiscalPrincipalContext<FFP> slideToPrevious() {
		if (current == null) {
			throw new IllegalStateException("Je refuse de glisser vers l'abîme !");
		}
		final List<FFP> newNext = new ArrayList<>(nexts.size() + 1);
		newNext.add(current);
		newNext.addAll(nexts);
		final List<FFP> newPrevious = previouses.size() > 1 ? new ArrayList<>(previouses.subList(1, previouses.size())) : Collections.<FFP>emptyList();
		final FFP newCurrent = previouses.isEmpty() ? null : previouses.get(0);
		return new ForFiscalPrincipalContext<>(newCurrent, newNext, newPrevious);
	}
}
