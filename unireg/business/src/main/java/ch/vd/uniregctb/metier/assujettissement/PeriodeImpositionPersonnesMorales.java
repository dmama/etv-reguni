package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class PeriodeImpositionPersonnesMorales extends PeriodeImposition {

	private final List<ExerciceCommercial> exercicesCommerciaux;
	private final TypeContribuable typeContribuable;
	private final TypeDocument typeDocument;

	public PeriodeImpositionPersonnesMorales(RegDate debut, RegDate fin, ContribuableImpositionPersonnesMorales contribuable,
	                                         boolean declarationOptionnelle, boolean declarationRemplaceeParNote, CauseFermeture causeFermeture, Integer codeSegment,
	                                         List<ExerciceCommercial> exercicesCommerciaux, TypeContribuable typeContribuable, TypeDocument typeDocument) {
		super(debut, fin, contribuable, declarationOptionnelle, declarationRemplaceeParNote, causeFermeture, codeSegment);
		this.exercicesCommerciaux = exercicesCommerciaux;
		this.typeContribuable = typeContribuable;
		this.typeDocument = typeDocument;
	}

	@Override
	protected boolean isCompatibleWith(PeriodeImposition next) {
		if (next instanceof PeriodeImpositionPersonnesMorales) {
			final PeriodeImpositionPersonnesMorales nextPeriode = (PeriodeImpositionPersonnesMorales) next;
			return typeDocument == nextPeriode.typeDocument
					&& typeContribuable == nextPeriode.typeContribuable
					&& DateRangeHelper.indexAt(exercicesCommerciaux, getDateFin(), NullDateBehavior.EARLIEST) == DateRangeHelper.indexAt(exercicesCommerciaux, next.getDateDebut(), NullDateBehavior.EARLIEST);
		}
		return false;
	}

	@Override
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	@Override
	public TypeDocument getTypeDocumentDeclaration() {
		return typeDocument;
	}

	@Override
	public boolean isDiplomateSuisseSansImmeuble() {
		return false;
	}

	@NotNull
	@Override
	protected RegDate getDernierJourPourPeriodeFiscale(@NotNull RegDate dateReference) {
		final ExerciceCommercial ex = DateRangeHelper.rangeAt(exercicesCommerciaux, dateReference);
		if (ex == null) {
			throw new IllegalArgumentException("Pas d'exercice commercial présent à la date de référence " + dateReference);
		}
		return ex.getDateFin();
	}

	@NotNull
	@Override
	public ContribuableImpositionPersonnesMorales getContribuable() {
		return (ContribuableImpositionPersonnesMorales) super.getContribuable();
	}

	@Override
	public PeriodeImpositionPersonnesMorales collate(DateRange next) {
		final PeriodeImpositionPersonnesMorales nextPeriode = (PeriodeImpositionPersonnesMorales) next;
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ne devrait pas être appelé si la période suivante n'est pas collatable...");
		}

		return new PeriodeImpositionPersonnesMorales(getDateDebut(),
		                                             nextPeriode.getDateFin(),
		                                             getContribuable(),
		                                             isDeclarationOptionnelle() && nextPeriode.isDeclarationOptionnelle(),
		                                             isDeclarationRemplaceeParNote()&& nextPeriode.isDeclarationRemplaceeParNote(),
		                                             nextPeriode.getCauseFermeture(),
		                                             getCodeSegment(),
		                                             exercicesCommerciaux,
		                                             typeContribuable,
		                                             typeDocument);
	}

	@Override
	protected void fillDisplayValues(@NotNull Map<String, String> map) {
		super.fillDisplayValues(map);
		map.put("typeContribuable", String.valueOf(typeContribuable));
		map.put("typeDocumentDeclaration", String.valueOf(typeDocument));
	}
}
