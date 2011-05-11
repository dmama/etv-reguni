package ch.vd.uniregctb.di.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.DeclarationImpotSelectView;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationImpotEditValidator implements Validator {

	private DeclarationImpotOrdinaireDAO diDAO;

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DeclarationImpotDetailView.class.equals(clazz) || DeclarationImpotListView.class.equals(clazz)
				|| DeclarationImpotSelectView.class.equals(clazz);
	}

	@Transactional(readOnly = true)
	public void validate(Object target, Errors errors) {

		if (target instanceof DeclarationImpotDetailView) {
			final DeclarationImpotDetailView details = (DeclarationImpotDetailView) target;
			if (details != null) {
				if (details.getId() == null) {
					if (details.getDelaiAccorde() == null) {
						ValidationUtils.rejectIfEmpty(errors, "delaiAccorde", "error.delai.accorde.vide");
						details.setImprimable(true);
					}
					else {
						if (details.getRegDelaiAccorde().isBefore(RegDate.get()) ||
								details.getRegDelaiAccorde().isAfter(RegDate.get().addMonths(6))) {
							errors.rejectValue("delaiAccorde", "error.delai.accorde.invalide");
							details.setImprimable(true);
						}
					}
				}
				else {
					if (details.getRegDateRetour() != null && details.getRegDateRetour().isAfter(RegDate.get())) {
						errors.rejectValue("dateRetour", "error.date.retour.future");
					}
					DeclarationImpotOrdinaire di = diDAO.get(details.getId());
					EtatDeclaration dernierEtat = getDernierEtatEmisOuSommee(di);
					if (details.getRegDateRetour() != null && details.getRegDateRetour().isBefore(dernierEtat.getDateObtention())) {
						if(dernierEtat instanceof EtatDeclarationSommee){
							errors.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission.sommation");
						}
						if(dernierEtat instanceof EtatDeclarationEmise){

							errors.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission");

						}

					}
				}
			}
		}
	}

	private EtatDeclaration getDernierEtatEmisOuSommee(DeclarationImpotOrdinaire di) {
		EtatDeclaration emis = di.getEtatDeclarationActif(TypeEtatDeclaration.EMISE);
		EtatDeclaration sommee = di.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE);
	    //On aura toujours un état émis sur une déclaration sinon bug
		if(sommee == null){
			return  emis;

		}else{
			return sommee;
		}
	}


}
