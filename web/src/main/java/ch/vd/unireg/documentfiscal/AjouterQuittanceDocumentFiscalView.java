package ch.vd.unireg.documentfiscal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class AjouterQuittanceDocumentFiscalView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private String libelleTypeDocument;
	private List<EtatDocumentFiscalView> etats;

	// id du document
	private Long id;

	// Données modifiables du formulaire
	private RegDate dateRetour;

	public AjouterQuittanceDocumentFiscalView() {
	}

	public AjouterQuittanceDocumentFiscalView(AutreDocumentFiscal doc, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		resetDocumentInfo(doc, infraService, messageHelper);
		this.dateRetour = doc.getDateRetour();
	}

	public void resetDocumentInfo(AutreDocumentFiscal doc, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this.tiersId = doc.getTiers().getId();
		this.id = doc.getId();
		this.periodeFiscale = doc.getPeriodeFiscale();
		this.etats = initEtats(doc.getEtats(), infraService, messageHelper);
		final AutreDocumentFiscalView autreDocumentFiscalView = AutreDocumentFiscalViewFactory.buildView(doc, infraService, messageHelper);
		this.libelleTypeDocument = autreDocumentFiscalView.getLibelleTypeDocument();
	}

	public static TypeEtatDocumentFiscal getDernierEtat(AutreDocumentFiscal doc) {
		final EtatDocumentFiscal etatDoc = doc.getDernierEtat();
		return etatDoc == null ? null : etatDoc.getEtat();
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDocumentFiscal> etats, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDocumentFiscal etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageHelper));
		}
		Collections.sort(list);
		return list;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public List<EtatDocumentFiscalView> getEtats() {
		return etats;
	}

	public String getLibelleTypeDocument() {
		return libelleTypeDocument;
	}
}
