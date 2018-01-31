package ch.vd.uniregctb.metier.assujettissement;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class PeriodeImpositionPersonnesPhysiques extends PeriodeImposition {

	private static final Set<TypeDocument> COMPLETE_OU_VAUDTAX = EnumSet.of(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
	                                                                        TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL,
	                                                                        TypeDocument.DECLARATION_IMPOT_VAUDTAX);

	private final CategorieEnvoiDIPP categorieEnvoiDI;
	private final TypeAdresseRetour adresseRetour; // [UNIREG-1741]

	public PeriodeImpositionPersonnesPhysiques(RegDate debut, RegDate fin, ContribuableImpositionPersonnesPhysiques contribuable,
	                                           boolean declarationOptionnelle, boolean declarationRemplaceeParNote, CauseFermeture causeFermeture, Integer codeSegment,
	                                           CategorieEnvoiDIPP categorieEnvoiDI, TypeAdresseRetour adresseRetour) {
		super(debut, fin, contribuable, declarationOptionnelle, declarationRemplaceeParNote, causeFermeture, codeSegment);
		this.categorieEnvoiDI = categorieEnvoiDI;
		this.adresseRetour = adresseRetour;
	}

	@Override
	protected boolean isCompatibleWith(PeriodeImposition next) {
		return next instanceof PeriodeImpositionPersonnesPhysiques && isEquivalent(categorieEnvoiDI, ((PeriodeImpositionPersonnesPhysiques) next).categorieEnvoiDI);
	}

	@Override
	public boolean isDeclarationMandatory() {
		return super.isDeclarationMandatory() && !isDiplomateSuisseSansImmeuble();
	}

	@Override
	public TypeContribuable getTypeContribuable() {
		return categorieEnvoiDI.getTypeContribuable();
	}

	@Override
	public TypeDocument getTypeDocumentDeclaration() {
		return categorieEnvoiDI.getTypeDocument();
	}

	public CategorieEnvoiDIPP getCategorieEnvoiDI() {
		return categorieEnvoiDI;
	}

	public TypeAdresseRetour getAdresseRetour() {
		return adresseRetour;
	}

	@Override
	public boolean isDiplomateSuisseSansImmeuble() {
		return categorieEnvoiDI == CategorieEnvoiDIPP.DIPLOMATE_SUISSE;
	}

	/**
	 * Pour les personnes physiques, les périodes fiscales sont des années civiles
	 * @return le 31.12 de l'année de la période d'imposition
	 */
	@NotNull
	@Override
	protected RegDate getDernierJourPourPeriodeFiscale() {
		return RegDate.get(getPeriodeFiscale(), 12, 31);
	}

	/**
	 * @param left  le type de gauche
	 * @param right le type de droite
	 * @return <b>vrai</b> si les deux types de documents sont égaux, en <b>ne faisant pas</b> de différence entre DECLARATION_IMPOT_COMPLETE et DECLARATION_IMPOT_VAUDTAX.
	 */
	private static boolean isEquivalent(CategorieEnvoiDIPP left, CategorieEnvoiDIPP right) {
		return (left == right) || (COMPLETE_OU_VAUDTAX.contains(left.getTypeDocument()) && COMPLETE_OU_VAUDTAX.contains(right.getTypeDocument()));
	}

	private static CategorieEnvoiDIPP collateCategorieEnvoi(CategorieEnvoiDIPP left, CategorieEnvoiDIPP right) {
		// Dans la plupart des cas, on prend le type de contribuable le plus à jour, c'est-à-dire la valeur 'right'.
		// Sauf lorsque on a le choix entre en déclaration d'impôt vaudtax et une complète; dans ce cas on préfère la vaudtax.
		if (left == CategorieEnvoiDIPP.VAUDOIS_VAUDTAX && right == CategorieEnvoiDIPP.VAUDOIS_COMPLETE) {
			return CategorieEnvoiDIPP.VAUDOIS_VAUDTAX;
		}
		else {
			return right;
		}
	}

	@NotNull
	@Override
	public ContribuableImpositionPersonnesPhysiques getContribuable() {
		return (ContribuableImpositionPersonnesPhysiques) super.getContribuable();
	}

	@Override
	public PeriodeImpositionPersonnesPhysiques collate(PeriodeImposition next) {
		final PeriodeImpositionPersonnesPhysiques nextPeriode = (PeriodeImpositionPersonnesPhysiques) next;
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ne devrait pas être appelé si la période suivante n'est pas collatable...");
		}

		final TypeAdresseRetour adresseRetour = nextPeriode.adresseRetour; // [UNIREG-1741] en prenant le second type, on est aussi correct en cas de décès.
		return new PeriodeImpositionPersonnesPhysiques(getDateDebut(),
		                                               nextPeriode.getDateFin(),
		                                               getContribuable(),
		                                               isDeclarationOptionnelle() && nextPeriode.isDeclarationOptionnelle(),
		                                               isDeclarationRemplaceeParNote()&& nextPeriode.isDeclarationRemplaceeParNote(),
		                                               nextPeriode.getCauseFermeture(),
		                                               getCodeSegment(),
		                                               collateCategorieEnvoi(categorieEnvoiDI, nextPeriode.categorieEnvoiDI),
		                                               adresseRetour);
	}

	@Override
	protected void fillDisplayValues(@NotNull Map<String, String> map) {
		super.fillDisplayValues(map);
		map.put("categorieEnvoiDI", String.valueOf(categorieEnvoiDI));
		map.put("adresseRetour", String.valueOf(adresseRetour));
	}
}
