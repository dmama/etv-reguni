package ch.vd.unireg.validation.declaration;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class DelaiDeclarationValidator extends EntityValidatorImpl<DelaiDeclaration> {

	@Override
	protected Class<DelaiDeclaration> getValidatedClass() {
		return DelaiDeclaration.class;
	}

	@Override
	public ValidationResults validate(DelaiDeclaration delai) {
		final ValidationResults vr = new ValidationResults();
		if (delai.isAnnule()) {
			return vr;
		}

		final Integer pf;
		final RegDate dateFin;
		if (delai.getDeclaration() != null && delai.getDeclaration().getPeriode() != null) {
			pf = delai.getDeclaration().getPeriode().getAnnee();
			dateFin = delai.getDeclaration().getDateFin();
		}
		else {
			pf = null;
			dateFin = null;
		}

		if (delai.getDateTraitement() == null) {
			vr.addError(String.format("La date de traitement n'est pas renseignée sur le délai de la déclaration %d (fin de période au %s).",
			                          pf,
			                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
		}

		if (delai.getEtat() == null) {
			vr.addError(String.format("L'état du délai n'est pas renseigné sur le délai de la déclaration %d (fin de période au %s).",
			                          pf,
			                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
		}
		else {
			if (delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
				if (delai.getDelaiAccordeAu() == null) {
					vr.addError(String.format("La date de délai accordé est obligatoire sur un délai dans l'état 'accordé' de la déclaration %d (fin de période au %s).",
					                          pf,
					                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
				}
			}
			else {
				if (delai.getDelaiAccordeAu() != null) {
					vr.addError(String.format("La date de délai accordé est interdite sur un délai dans un état différent de 'accordé' (déclaration %d, fin de période au %s).",
					                          pf,
					                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
				}
				if (delai.isSursis()) {
					vr.addError(String.format("Seuls les délais accordés peuvent être dotés du flag 'sursis' (déclaration %s, fin de période au %s).",
					                          pf,
					                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
				}
			}
		}

		if (delai.getTypeDelai() == null) {
			vr.addError(String.format("Le type de délai n'est pas renseigné sur le délai de la déclaration %d (fin de période au %s).",
			                          pf,
			                          StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?")));
		}

		return vr;
	}
}
