package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;

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
	public String toString() {
		String tacheDetail;

		if (tache instanceof TacheAnnulationDeclarationImpot) {
			final TacheAnnulationDeclarationImpot annule = (TacheAnnulationDeclarationImpot) tache;
			final DeclarationImpotOrdinaire di = annule.getDeclarationImpotOrdinaire();
			tacheDetail = String.format("d'annulation de la déclaration d'impôt %s couvrant la période du %s au %s", di.getTypeContribuable().description(),
					RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()));
		}
		else if (tache instanceof TacheEnvoiDeclarationImpot) {
			final TacheEnvoiDeclarationImpot envoi = (TacheEnvoiDeclarationImpot) tache;
			tacheDetail = String.format("d'envoi de la déclaration d'impôt %s couvrant la période du %s au %s", envoi.getTypeContribuable().description(),
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
			throw new IllegalArgumentException("Type de tache inconnue = [" + tache.getClass().getSimpleName() + "]");
		}

		return "annulation de la tâche " + tacheDetail;
	}
}
