package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Action permettant d'annuler une tâche devenue obsolète.
 */
public class AnnuleTache extends SynchronizeAction {
	public final Tache tache;

	public AnnuleTache(Tache tache) {
		this.tache = tache;
	}

	@Override
	public void execute(Context context) {
		tache.setAnnule(true);
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	private static String toString(TypeContribuable typeContribuable) {
		if (typeContribuable == null) {
			return "de type inconnu";
		}
		return typeContribuable.description();
	}

	@Override
	public String toString() {
		final String tacheDetail;
		if (tache instanceof TacheAnnulationDeclarationImpot) {
			final TacheAnnulationDeclarationImpot annule = (TacheAnnulationDeclarationImpot) tache;
			final DeclarationImpotOrdinaire di = annule.getDeclarationImpotOrdinaire();
			if (di instanceof DeclarationImpotOrdinairePP) {
				tacheDetail = String.format("d'annulation de la déclaration d'impôt %s couvrant la période du %s au %s",
				                            toString(((DeclarationImpotOrdinairePP) di).getTypeContribuable()),
				                            RegDateHelper.dateToDisplayString(di.getDateDebut()),
				                            RegDateHelper.dateToDisplayString(di.getDateFin()));
			}
			else {
				tacheDetail = String.format("d'annulation de la déclaration d'impôt couvrant la période du %s au %s",
				                            RegDateHelper.dateToDisplayString(di.getDateDebut()),
				                            RegDateHelper.dateToDisplayString(di.getDateFin()));
			}
		}
		else if (tache instanceof TacheEnvoiDeclarationImpotPP) {
			final TacheEnvoiDeclarationImpotPP envoi = (TacheEnvoiDeclarationImpotPP) tache;
			tacheDetail = String.format("d'envoi de la déclaration d'impôt %s couvrant la période du %s au %s", toString(envoi.getTypeContribuable()),
					RegDateHelper.dateToDisplayString(envoi.getDateDebut()), RegDateHelper.dateToDisplayString(envoi.getDateFin()));
		}
		else if (tache instanceof TacheControleDossier) {
			tacheDetail = "de contrôle de dossier";
		}
		else if (tache instanceof TacheNouveauDossier) {
			tacheDetail = "de nouveau dossier";
		}
		else if (tache instanceof TacheTransmissionDossier) {
			final TacheTransmissionDossier trans = (TacheTransmissionDossier) tache;
			tacheDetail = "de transmission de dossier";
		}
		else {
			throw new IllegalArgumentException("Type de tache inconnue = [" + tache.getClass().getSimpleName() + ']');
		}

		return "annulation de la tâche " + tacheDetail;
	}
}
