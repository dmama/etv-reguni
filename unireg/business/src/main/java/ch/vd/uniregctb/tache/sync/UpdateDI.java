package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

/**
 * Action permettant de mettre-à-jour les dates de début/fin et le type de contribuable d'une déclaration d'impôt.
 */
public class UpdateDI extends SynchronizeAction {
	public final PeriodeImposition periodeImposition;
	public final DeclarationImpotOrdinaire declaration;

	public UpdateDI(PeriodeImposition periodeImposition, DeclarationImpotOrdinaire declaration) {
		this.periodeImposition = periodeImposition;
		this.declaration = declaration;
	}

	@Override
	public void execute(Context context) {
		// [UNIREG-1303] Autant que faire se peut, on évite de créer des tâches d'envoi/annulation de DI et on met-à-jour les DIs existantes. L'idée est d'éviter d'incrémenter le numéro de
		// séquence des DIs parce que cela pose des problèmes lors du quittancement, et de toutes façons la période exacte n'est pas imprimée sur les DIs.
		declaration.setDateDebut(periodeImposition.getDateDebut());
		declaration.setDateFin(periodeImposition.getDateFin());
		declaration.setTypeContribuable(periodeImposition.getTypeContribuable());

		// [UNIREG-2735] Une DI qui s'est adapté à une période d'imposition existante n'est plus une DI libre
		declaration.setLibre(false);
	}

	@Override
	public String toString() {
		// [UNIREG-3031] Certaines anciennes DIs ne possèdent pas de type de contribuable connu
		final String descriptionTypeContribuable = (declaration.getTypeContribuable() == null ? "de type inconnu" : declaration.getTypeContribuable().description());
		return String.format("mise-à-jour de la déclaration d'impôt %s existante couvrant la période du %s au %s pour qu'elle devienne %s et qu'elle couvre la période du %s au %s",
				descriptionTypeContribuable, RegDateHelper.dateToDisplayString(declaration.getDateDebut()), RegDateHelper.dateToDisplayString(declaration.getDateFin()),
				periodeImposition.getTypeContribuable().description(), RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()),
				RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}
}
