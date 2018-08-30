package ch.vd.unireg.declaration.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;

public class DeclarationView extends DocumentFiscalView {

	private final int periodeFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public DeclarationView(Declaration declaration, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		super(declaration, infraService, messageHelper);
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
