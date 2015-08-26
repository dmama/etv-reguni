package ch.vd.uniregctb.validation.declaration;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

public class DeclarationImpotOrdinaireValidator extends DeclarationValidator<DeclarationImpotOrdinaire> {

	@Override
	protected Class<DeclarationImpotOrdinaire> getValidatedClass() {
		return DeclarationImpotOrdinaire.class;
	}

	@Override
	public ValidationResults validate(DeclarationImpotOrdinaire di) {
		final ValidationResults vr = super.validate(di);
		if (!di.isAnnule()) {

			if (di.getPeriode() == null) {
				vr.addError("La période ne peut pas être nulle.");
			}

			// TODO [SIPM] pour le moment, on ne veut pas faire sortir cette erreur pour les DI PM (problématique de migration : quel modèle de document ?)
			if (di.getTiers() instanceof ContribuableImpositionPersonnesPhysiques) {
				if (di.getModeleDocument() == null) {
					vr.addError("Le modèle de document ne peut pas être nul.");
				}
			}

			if (di.getNumero() == null) {
				vr.addError("Le numéro de séquence de la déclaration ne peut pas être nul.");
			}

			// TODO [SIPM] le code segment n'a pas vraiment de sens (en tout cas pas forcément depuis 2011) sur les DI PM
			if (di.getTiers() instanceof ContribuableImpositionPersonnesPhysiques) {
				if (di.getPeriode() != null
						&& di.getPeriode().getAnnee() >= DeclarationImpotOrdinaire.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE
						&& di.getCodeSegment() == null) {
					vr.addError(String.format("Le code segment ne peut être nul pour une déclaration à partir de %d", DeclarationImpotOrdinaire.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE));
				}
			}
		}

		return vr;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La DI";
	}
}
