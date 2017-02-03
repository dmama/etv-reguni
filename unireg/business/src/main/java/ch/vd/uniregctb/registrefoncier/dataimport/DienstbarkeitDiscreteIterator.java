package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

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

	public DienstbarkeitDiscreteIterator(@NotNull Iterator<? extends DienstbarkeitExtended> sourceIterator) {
		this.sourceIterator = sourceIterator;
		this.benenficiaireIterator = null;
		this.immeubleIterator = null;
		this.currentSourceBeneficiaire = null;
		this.currentSourceServitude = null;
	}

	@Override
	public boolean hasNext() {
		return (immeubleIterator != null && immeubleIterator.hasNext()) || (benenficiaireIterator != null && benenficiaireIterator.hasNext()) || sourceIterator.hasNext();
	}

	/**
	 * @return la servitude suivante. La servitude retournée ne possède qu'un seul immeuble, par contrat.
	 */
	@Override
	public DienstbarkeitDiscrete next() {

		// on avance les itérateurs sur les servitudes et bénéficiaires si nécessaire
		if (immeubleIterator == null || !immeubleIterator.hasNext()) {
			if (benenficiaireIterator == null || !benenficiaireIterator.hasNext()) {
				nextServitude();
			}
			nextBeneficiaire();
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

	private void nextBeneficiaire() {
		currentSourceBeneficiaire = benenficiaireIterator.next();
		immeubleIterator = currentSourceServitude.getLastRechtGruppe().getBelastetesGrundstueck().iterator();
		if (!immeubleIterator.hasNext()) {
			throw new IllegalArgumentException("La servitude standardRechtID=[" + currentSourceServitude.getDienstbarkeit().getStandardRechtID() + "] ne possède pas d'immeuble.");
		}
	}

	private void nextServitude() {
		currentSourceServitude = sourceIterator.next();
		benenficiaireIterator = currentSourceServitude.getLastRechtGruppe().getBerechtigtePerson().iterator();
		if (!benenficiaireIterator.hasNext()) {
			throw new IllegalArgumentException("La servitude standardRechtID=[" + currentSourceServitude.getDienstbarkeit().getStandardRechtID() + "] ne possède pas de bénéficiaire.");
		}
		immeubleIterator = null;
	}
}
