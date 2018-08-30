package ch.vd.unireg.registrefoncier.allegement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.DelaiDocumentFiscalView;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;

public class EditDemandeDegrevementView extends AbstractEditDemandeDegrevementView {

	private long idDemandeDegrevement;
	private List<EtatDocumentFiscalView> etats = new ArrayList<>();
	private List<DelaiDocumentFiscalView> delais = new ArrayList<>();

	public EditDemandeDegrevementView() {
	}

	public EditDemandeDegrevementView(DemandeDegrevementICI demande, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		super(demande);
		this.idDemandeDegrevement = demande.getId();
		this.etats = initEtats(demande.getEtats(), infraService, messageHelper);
		this.delais = initDelais(demande.getDelais(), demande.getPremierDelai(), infraService, messageHelper);
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
			final DelaiDocumentFiscalView delaiView = new DelaiDocumentFiscalView(delai, infraService, messageHelper);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
		return list;
	}

	public long getIdDemandeDegrevement() {
		return idDemandeDegrevement;
	}

	public void setIdDemandeDegrevement(long idDemandeDegrevement) {
		this.idDemandeDegrevement = idDemandeDegrevement;
	}

	public List<EtatDocumentFiscalView> getEtats() {
		return etats;
	}

	public List<DelaiDocumentFiscalView> getDelais() {
		return delais;
	}
}
