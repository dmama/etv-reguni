package ch.vd.unireg.documentfiscal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class AjouterEtatAutreDocumentFiscalView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private String libelleTypeDocument;
	private List<EtatDocumentFiscalView> etats;

	// id du document
	private Long id;

	// Données modifiables du formulaire
	private RegDate dateRetour;

	public AjouterEtatAutreDocumentFiscalView() {
	}

	public AjouterEtatAutreDocumentFiscalView(AutreDocumentFiscal doc, ServiceInfrastructureService infraService, MessageSource messageSource) {
		resetDocumentInfo(doc, infraService, messageSource);
		this.dateRetour = doc.getDateRetour();
	}

	public void resetDocumentInfo(AutreDocumentFiscal doc, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.tiersId = doc.getTiers().getId();
		this.id = doc.getId();
		this.periodeFiscale = doc.getPeriodeFiscale();
		this.etats = initEtats(doc.getEtats(), infraService, messageSource);
		final AutreDocumentFiscalView autreDocumentFiscalView = AutreDocumentFiscalViewFactory.buildView(doc, infraService, messageSource);
		this.libelleTypeDocument = autreDocumentFiscalView.getLibelleTypeDocument();
	}

	public static TypeEtatDocumentFiscal getDernierEtat(AutreDocumentFiscal doc) {
		final EtatDocumentFiscal etatDoc = doc.getDernierEtat();
		return etatDoc == null ? null : etatDoc.getEtat();
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDocumentFiscal> etats, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDocumentFiscal etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageSource));
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
