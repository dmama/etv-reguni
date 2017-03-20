package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.capitastra.rechteregister.DienstbarkeitExtended;

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
public class DienstbarkeitDiscreteIterator implements Iterator<DienstbarkeitDiscrete> {

	private final Iterator<? extends DienstbarkeitExtended> sourceIterator;
	private Iterator<BerechtigtePerson> benenficiaireIterator;
	private Iterator<BelastetesGrundstueck> immeubleIterator;
	private BerechtigtePerson currentSourceBeneficiaire;
	private DienstbarkeitExtended currentSourceServitude;
	private DienstbarkeitDiscrete next;
	private final List<DienstbarkeitExtended> emptyServitudes = new ArrayList<>();

	public DienstbarkeitDiscreteIterator(@NotNull Iterator<? extends DienstbarkeitExtended> sourceIterator) {
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
	public DienstbarkeitDiscrete next() {
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
	public List<DienstbarkeitExtended> getEmptyServitudes() {
		return emptyServitudes;
	}

	/**
	 * Avance l'itérateur sur l'élément suivant et retourne cet élément.
	 *
	 * @return l'élément suivant ou <b>null</b> si on a atteint la fin de l'itérateur.
	 */
	@Nullable
	private DienstbarkeitDiscrete buildNext() {

		// on avance les itérateurs sur les servitudes et bénéficiaires si nécessaire
		while (immeubleIterator == null || !immeubleIterator.hasNext()) {
			while (benenficiaireIterator == null || !benenficiaireIterator.hasNext()) {
				if (!sourceIterator.hasNext()) {
					// il n'y a plus de données, on a terminé
					return null;
				}
				// on va chercher la source suivante
				currentSourceServitude = sourceIterator.next();
				final List<BerechtigtePerson> beneficiaires = currentSourceServitude.getLastRechtGruppe().getBerechtigtePerson();
				if (beneficiaires.isEmpty()) {
					// [SIFISC-23744] on mémorise les servitudes vides pour les annoncer dans le rapport
					emptyServitudes.add(currentSourceServitude);
				}
				benenficiaireIterator = beneficiaires.iterator();
				immeubleIterator = null;
			}
			// on va chercher le bénéficiaire suivant
			currentSourceBeneficiaire = benenficiaireIterator.next();
			immeubleIterator = currentSourceServitude.getLastRechtGruppe().getBelastetesGrundstueck().iterator();
		}
		// on va chercher l'immeuble suivant
		final BelastetesGrundstueck immeuble = immeubleIterator.next();

		// on crée une copie de la servitude avec un seul immeuble et un seul propriétaire renseigné
		final DienstbarkeitDiscrete n = new DienstbarkeitDiscrete();
		n.setDienstbarkeit(currentSourceServitude.getDienstbarkeit());
		n.setBerechtigtePerson(currentSourceBeneficiaire);
		n.setBelastetesGrundstueck(immeuble);

		final List<BerechtigtePerson> beneficiaires = currentSourceServitude.getLastRechtGruppe().getBerechtigtePerson();
		if (beneficiaires.size() > 1) {
			// on considère que s'il y a plus qu'un bénéficiaire, il s'agit d'une communauté implicite
			n.getGemeinschaft().addAll(beneficiaires);
		}

		return n;
	}
}
