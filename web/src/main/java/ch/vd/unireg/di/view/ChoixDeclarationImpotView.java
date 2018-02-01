package ch.vd.unireg.di.view;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FiscalDateHelper;

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
		final int longueurEnJours = FiscalDateHelper.getLongueurEnJours(debut, fin);
		return String.format("Période du %s au %s (%d jour%s)",
		                     RegDateHelper.dateToDisplayString(debut),
		                     RegDateHelper.dateToDisplayString(fin),
		                     longueurEnJours,
		                     longueurEnJours > 1 ? "s" : StringUtils.EMPTY);
	}

	public boolean isOptionnelle() {
		return optionnelle;
	}
}
