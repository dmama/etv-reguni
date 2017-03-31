package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Iterateur qui découpe par immeuble et par bénéficiaire les servitudes exposées par l'itérateur source.
 * <p/>
 * Exemple:
 * <ul>
 *     <li>Source
 *         <ol>
 *             <li>servitude 1 + (bénéficiaire 1 + bénéficiaire 2) + (immeuble 1 + immeuble 2)</li>
 *             <li>servitude 2 + (bénéficiaire 3 + bénéficiaire 4) + immeuble 3</li>
 *             <li>servitude 3 + bénéficiaire 5 + (immeuble 4 + immeuble 5 + immeuble 6)</li>
 *         </ol>
 *     </li>
 *     <li>Résultat
 *         <ol>
 *             <li>servitude 1 + bénéficiaire 1 + immeuble 1</li>
 *             <li>servitude 1 + bénéficiaire 1 + immeuble 2</li>
 *             <li>servitude 1 + bénéficiaire 2 + immeuble 1</li>
 *             <li>servitude 1 + bénéficiaire 2 + immeuble 2</li>
 *             <li>servitude 2 + bénéficiaire 3 + immeuble 3</li>
 *             <li>servitude 2 + bénéficiaire 4 + immeuble 3</li>
 *             <li>servitude 3 + bénéficiaire 5 + immeuble 4</li>
 *             <li>servitude 3 + bénéficiaire 5 + immeuble 5</li>
 *             <li>servitude 3 + bénéficiaire 5 + immeuble 6</li>
 *         </ol>
 *     </li>
 * </ul>
 */
public class ServitudeCombinationIterator implements Iterator<ServitudeRF> {

	private final Iterator<? extends ServitudeRF> sourceIterator;
	private Iterator<AyantDroitRF> benenficiaireIterator;
	private Iterator<ImmeubleRF> immeubleIterator;
	private AyantDroitRF currentSourceBeneficiaire;
	private ServitudeRF currentSourceServitude;
	private ServitudeRF next;
	private final List<ServitudeRF> emptyServitudes = new ArrayList<>();

	public ServitudeCombinationIterator(@NotNull Iterator<? extends ServitudeRF> sourceIterator) {
		this.sourceIterator = sourceIterator;
		this.benenficiaireIterator = null;
		this.immeubleIterator = null;
		this.currentSourceBeneficiaire = null;
		this.currentSourceServitude = null;
		this.next = buildNext();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	/**
	 * @return la servitude suivante. La servitude retournée ne possède qu'un seul immeuble, par contrat.
	 */
	@Override
	public ServitudeRF next() {
		try {
			return next;
		}
		finally {
			this.next = buildNext();
		}
	}

	/**
	 * @return les servitudes qui n'ont pas de bénéficiaires. Cette collection est complète une fois que l'itérateur a été entièrement parcouru.
	 */
	public List<ServitudeRF> getEmptyServitudes() {
		return emptyServitudes;
	}

	/**
	 * Avance l'itérateur sur l'élément suivant et retourne cet élément.
	 *
	 * @return l'élément suivant ou <b>null</b> si on a atteint la fin de l'itérateur.
	 */
	@Nullable
	private ServitudeRF buildNext() {

		// on avance les itérateurs sur les servitudes et bénéficiaires si nécessaire
		while (immeubleIterator == null || !immeubleIterator.hasNext()) {
			while (benenficiaireIterator == null || !benenficiaireIterator.hasNext()) {
				if (!sourceIterator.hasNext()) {
					// il n'y a plus de données, on a terminé
					return null;
				}
				// on va chercher la source suivante
				currentSourceServitude = sourceIterator.next();
				final List<AyantDroitRF> beneficiaires = new ArrayList<>(currentSourceServitude.getAyantDroits());
				beneficiaires.sort(Comparator.comparing(AyantDroitRF::getIdRF, Comparator.nullsFirst(Comparator.naturalOrder())));
				if (beneficiaires.isEmpty()) {
					// [SIFISC-23744] on mémorise les servitudes vides pour les annoncer dans le rapport
					emptyServitudes.add(currentSourceServitude);
				}
				benenficiaireIterator = beneficiaires.iterator();
				immeubleIterator = null;
			}
			// on va chercher le bénéficiaire suivant
			currentSourceBeneficiaire = benenficiaireIterator.next();
			final List<ImmeubleRF> immeubles = new ArrayList<>(currentSourceServitude.getImmeubles());
			immeubles.sort(Comparator.comparing(ImmeubleRF::getIdRF, Comparator.nullsFirst(Comparator.naturalOrder())));
			immeubleIterator = immeubles.iterator();
		}
		// on va chercher l'immeuble suivant
		final ImmeubleRF immeuble = immeubleIterator.next();

		// on crée une copie de la servitude avec un seul immeuble et un seul propriétaire renseigné
		final ServitudeRF n;
		try {
			n = (ServitudeRF) currentSourceServitude.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		n.setAyantDroits(Collections.singleton(currentSourceBeneficiaire));
		n.setImmeubles(Collections.singleton(immeuble));

		return n;
	}
}
