package ch.vd.unireg.tache.sync;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.type.TypeContribuable;

public abstract class UpdateDI<P extends PeriodeImposition, D extends DeclarationImpotOrdinaire> implements SynchronizeAction {

	public final P periodeImposition;
	public final Long diId;

	// quelques données pour le toString
	private final TypeContribuable typeContribuable;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public UpdateDI(P periodeImposition, D declaration) {
		this.periodeImposition = periodeImposition;
		this.diId = declaration.getId();
		this.typeContribuable = declaration.getTypeContribuable();
		this.dateDebut = declaration.getDateDebut();
		this.dateFin = declaration.getDateFin();
	}

	@Override
	public void execute(Context context) {
		//noinspection unchecked
		final D declaration = (D) context.diDAO.get(diId);
		if (declaration != null) {
			miseAJourDeclaration(declaration);
		}
	}

	protected void miseAJourDeclaration(@NotNull D declaration) {
		// [UNIREG-1303] Autant que faire se peut, on évite de créer des tâches d'envoi/annulation de DI et on met-à-jour les DIs existantes. L'idée est d'éviter d'incrémenter le numéro de
		// séquence des DIs parce que cela pose des problèmes lors du quittancement, et de toutes façons la période exacte n'est pas imprimée sur les DIs.
		declaration.setDateDebut(periodeImposition.getDateDebut());
		declaration.setDateFin(periodeImposition.getDateFin());
		declaration.setTypeContribuable(periodeImposition.getTypeContribuable());

		// [UNIREG-2735] Une DI qui s'est adaptée à une période d'imposition existante n'est plus une DI libre
		declaration.setLibre(false);
	}

	@Override
	public boolean willChangeEntity() {
		return true;
	}

	@Override
	public String toString() {
		// [UNIREG-3031] Certaines anciennes DIs ne possèdent pas de type de contribuable connu
		final String descriptionTypeContribuable = (typeContribuable == null ? "de type inconnu" : typeContribuable.description());
		return String.format("mise-à-jour de la déclaration d'impôt %s existante couvrant la période du %s au %s pour qu'elle devienne %s et qu'elle couvre la période du %s au %s",
		                     descriptionTypeContribuable, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin),
		                     periodeImposition.getTypeContribuable().description(), RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}

}
