package ch.vd.unireg.registrefoncier.allegement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.DelaiDocumentFiscalView;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class EditDemandeDegrevementView extends AbstractEditDemandeDegrevementView {

	private long idDemandeDegrevement;
	private List<EtatDocumentFiscalView> etats = new ArrayList<>();
	private List<DelaiDocumentFiscalView> delais = new ArrayList<>();

	public EditDemandeDegrevementView() {
	}

	public EditDemandeDegrevementView(DemandeDegrevementICI demande, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(demande);
		this.idDemandeDegrevement = demande.getId();
		this.etats = initEtats(demande.getEtats(), infraService, messageSource);
		this.delais = initDelais(demande.getDelais(), demande.getPremierDelai(), infraService, messageSource);
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDocumentFiscal> etats, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDocumentFiscal etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageSource));
		}
		Collections.sort(list);
		return list;
	}

	private static List<DelaiDocumentFiscalView> initDelais(Set<DelaiDocumentFiscal> delais, RegDate premierDelai, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<DelaiDocumentFiscalView> list = new ArrayList<>();
		for (DelaiDocumentFiscal delai : delais) {
			final DelaiDocumentFiscalView delaiView = new DelaiDocumentFiscalView(delai, infraService, messageSource);
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
