package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Classe de base des actions d'annulation de DI
 * @param <D> type concret de la déclaration
 */
public abstract class DeleteDI<D extends DeclarationImpotOrdinaire> implements TacheSynchronizeAction {

	public final Long diId;

	// quelques données pour le toString
	protected final TypeContribuable typeContribuable;
	protected final RegDate dateDebut;
	protected final RegDate dateFin;

	public DeleteDI(D declaration) {
		this.diId = declaration.getId();
		this.typeContribuable = declaration.getTypeContribuable();
		this.dateDebut = declaration.getDateDebut();
		this.dateFin = declaration.getDateFin();
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public int getPeriodeFiscale() {
		return dateFin.year();
	}
}
