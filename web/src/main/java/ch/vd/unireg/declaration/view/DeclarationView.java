package ch.vd.uniregctb.declaration.view;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class DeclarationView extends DocumentFiscalView {

	private final int periodeFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public DeclarationView(Declaration declaration, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(declaration, infraService, messageSource);
		this.periodeFiscale = declaration.getPeriode().getAnnee();
		this.dateDebut = declaration.getDateDebut();
		this.dateFin = declaration.getDateFin();
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}
}
