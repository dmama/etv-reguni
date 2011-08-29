package ch.vd.uniregctb.evenement.di;

import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.validation.ValidationService;

public class EvenementDeclarationServiceImpl implements EvenementDeclarationService, EvenementDeclarationHandler {

	private TiersDAO tiersDAO;
	private ValidationService validationService;
	private DeclarationImpotService diService;

	@Override
	public void onEvent(EvenementDeclaration event) throws EvenementDeclarationException {

		if (event instanceof QuittancementDI) {
			onQuittancementDI((QuittancementDI) event);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	private void onQuittancementDI(QuittancementDI quittance) throws EvenementDeclarationException {
		// On récupère le contribuable correspondant
		final long ctbId = quittance.getNumeroContribuable();
		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " n'existe pas.");
		}

		final ValidationResults results = validationService.validate(ctb);
		if (results.hasErrors()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " ne valide pas (" + results.toString() + ").");
		}

		// On s'assure que l'on est bien cohérent avec les données en base
		if (ctb.isDebiteurInactif()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " est un débiteur inactif, il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		final int annee = quittance.getPeriodeFiscale();
		final List<Declaration> declarations = ctb.getDeclarationsForPeriode(annee, false);
		if (declarations == null || declarations.isEmpty()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + ".");
		}

		quittancerDeclarations(ctb,declarations,quittance);

	}

	private void quittancerDeclarations(Contribuable ctb, List<Declaration> declarations,QuittancementDI quittance) {
		for (Declaration declaration : declarations) {
			if(!declaration.isAnnule()){
			  diService.retourDI(ctb,(DeclarationImpotOrdinaire)declaration,quittance.getDate());
			}
		}
	}


	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}


	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

		public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/di/evenementDeclarationImpot-input-1.xsd");
	}
}
