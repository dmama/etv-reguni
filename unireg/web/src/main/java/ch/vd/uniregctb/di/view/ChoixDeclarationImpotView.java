package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class ChoixDeclarationImpotView {
	private final DateRange range;
	/**
	 * <b>vrai</b> s'il s'agit d'une déclaration qui peut être émise sur demande, mais n'est pas obligatoire (cas du contribuable
	 * hors-Suisse et possédant un immeuble dans le canton -> seule la première DI est obligatoire, les suivantes peuvent être émises à la
	 * demande).
	 */
	private final boolean optionnelle;

	public ChoixDeclarationImpotView(DateRange range, boolean optionnelle) {
		this.range = range;
		this.optionnelle = optionnelle;
	}

	public String getId() {
		return String.format("%d-%d", range.getDateDebut().index(), range.getDateFin().index());
	}

	public String getDescription() {
		final RegDate debut = range.getDateDebut();
		final RegDate fin = range.getDateFin();
		if (debut.month() == 1 && debut.day() == 1 && fin.month() == 12 && fin.day() == 31) {
			return "Année " + debut.year() + " complète";
		}
		else {
			return "Période du " + RegDateHelper.dateToDisplayString(debut) + " au " + RegDateHelper.dateToDisplayString(fin);
		}
	}

	public boolean isOptionnelle() {
		return optionnelle;
	}
}
