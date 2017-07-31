package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class PeriodeImpositionPersonnesMorales extends PeriodeImposition {

	private final List<ExerciceCommercial> exercicesCommerciaux;
	private final TypeContribuable typeContribuable;
	private final TypeDocument typeDocument;
	private final CategorieEntreprise categorieEntreprise;

	public PeriodeImpositionPersonnesMorales(RegDate debut, RegDate fin, ContribuableImpositionPersonnesMorales contribuable,
	                                         boolean declarationOptionnelle, boolean declarationRemplaceeParNote, CauseFermeture causeFermeture, Integer codeSegment,
	                                         List<ExerciceCommercial> exercicesCommerciaux, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                         CategorieEntreprise categorieEntreprise) {
		super(debut, fin, contribuable, declarationOptionnelle, declarationRemplaceeParNote, causeFermeture, codeSegment);
		this.exercicesCommerciaux = exercicesCommerciaux;
		this.typeContribuable = typeContribuable;
		this.typeDocument = typeDocument;
		this.categorieEntreprise = categorieEntreprise;

		if (!typeContribuable.isUsedForPM()) {
			throw new IllegalArgumentException("Le type de contribuable " + typeContribuable + " n'est pas supporté pour les PM.");
		}
	}

	@Override
	protected boolean isCompatibleWith(PeriodeImposition next) {
		if (next instanceof PeriodeImpositionPersonnesMorales) {
			final PeriodeImpositionPersonnesMorales nextPeriode = (PeriodeImpositionPersonnesMorales) next;
			return typeDocument == nextPeriode.typeDocument
					&& categorieEntreprise == nextPeriode.categorieEntreprise       // [SIFISC-18529] plutôt que le type de contribuable, c'est la catégorie d'entreprise qui doit être conservée
					&& getExerciceCommercial().isValidAt(next.getDateDebut());      // pas de changement d'exercice commercial
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

	public CategorieEntreprise getCategorieEntreprise() {
		return categorieEntreprise;
	}

	@NotNull
	@Override
	protected RegDate getDernierJourPourPeriodeFiscale() {
		return getExerciceCommercial().getDateFin();
	}

	@NotNull
	public ExerciceCommercial getExerciceCommercial() {
		final ExerciceCommercial ex = DateRangeHelper.rangeAt(exercicesCommerciaux, getDateFin());
		if (ex == null) {
			throw new IllegalArgumentException("Pas d'exercice commercial sous-jacent!");
		}
		return ex;
	}

	@NotNull
	@Override
	public ContribuableImpositionPersonnesMorales getContribuable() {
		return (ContribuableImpositionPersonnesMorales) super.getContribuable();
	}

	@Override
	public PeriodeImpositionPersonnesMorales collate(PeriodeImposition next) {
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
		                                             nextPeriode.getTypeContribuable(),
		                                             typeDocument,
		                                             categorieEntreprise);
	}

	@Override
	protected void fillDisplayValues(@NotNull Map<String, String> map) {
		super.fillDisplayValues(map);
		map.put("typeContribuable", String.valueOf(typeContribuable));
		map.put("typeDocumentDeclaration", String.valueOf(typeDocument));
		map.put("categorieEntreprise", String.valueOf(categorieEntreprise));
	}
}
