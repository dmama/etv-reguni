package ch.vd.unireg.declaration.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.documentfiscal.SourceQuittancement;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public abstract class DocumentFiscalView implements Annulable {

	private final long id;
	private final Long tiersId;
	private final boolean annule;
	private final TypeEtatDocumentFiscal etat;
	private final List<EtatDocumentFiscalView> etats;
	private final List<DelaiDocumentFiscalView> delais;
	private final RegDate delaiAccorde;
	private final RegDate dateRetour;
	private final String sourceRetour;

	public DocumentFiscalView(DocumentFiscal documentFiscal, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this.id = documentFiscal.getId();
		this.tiersId = documentFiscal.getTiers().getNumero();
		this.annule = documentFiscal.isAnnule();

		final EtatDocumentFiscal etat = documentFiscal.getDernierEtat();
		this.etat = (etat == null ? null : etat.getEtat());
		if (etat instanceof SourceQuittancement) {
			this.sourceRetour = ((SourceQuittancement) etat).getSource();
		}
		else {
			this.sourceRetour = null;
		}

		this.etats = initEtats(documentFiscal.getEtats(), infraService, messageHelper);
		this.delais = initDelais(documentFiscal.getDelais(), documentFiscal.getPremierDelai(), infraService, messageHelper);

		this.delaiAccorde = documentFiscal.getDelaiAccordeAu();
		this.dateRetour = documentFiscal.getDateRetour();
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDocumentFiscal> etats, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDocumentFiscal etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageHelper));
		}
		Collections.sort(list);
		return list;
	}

	private static List<DelaiDocumentFiscalView> initDelais(Set<DelaiDocumentFiscal> delais, RegDate premierDelai, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final List<DelaiDocumentFiscalView> list = new ArrayList<>();
		for (DelaiDocumentFiscal delai : delais) {
			final DelaiDocumentFiscalView delaiView = new DelaiDocumentFiscalView(delai, infraService);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public long getId() {
		return id;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public TypeEtatDocumentFiscal getEtat() {
		return etat;
	}

	public List<EtatDocumentFiscalView> getEtats() {
		return etats;
	}

	public List<DelaiDocumentFiscalView> getDelais() {
		return delais;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public String getSourceRetour() {
		return sourceRetour;
	}
}
