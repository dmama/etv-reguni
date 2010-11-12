package ch.vd.uniregctb.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.service.PartPM;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 */
public class HostPersonneMoraleServiceImpl implements HostPersonneMoraleService {

	private ServicePersonneMoraleService servicePersonneMoraleService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePersonneMoraleService(ServicePersonneMoraleService servicePersonneMoraleService) {
		this.servicePersonneMoraleService = servicePersonneMoraleService;
	}

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	public EntrepriseView get(Long numeroEntreprise) {

		EntrepriseView entrepriseView = new EntrepriseView();

		final PersonneMorale pm = servicePersonneMoraleService.getPersonneMorale(numeroEntreprise, PartPM.ADRESSES, PartPM.FORMES_JURIDIQUES, PartPM.CAPITAUX, PartPM.SIEGES);
		if (pm != null) {
			entrepriseView.setNumeroIPMRO(pm.getNumeroIPMRO());
			entrepriseView.setDesignationAbregee(pm.getDesignationAbregee());
			entrepriseView.setRaisonSociale(pm.getRaisonSociale());
			entrepriseView.setRaisonSociale1(pm.getRaisonSociale1());
			entrepriseView.setRaisonSociale2(pm.getRaisonSociale2());
			entrepriseView.setRaisonSociale3(pm.getRaisonSociale3());
			entrepriseView.setDateFinDernierExerciceCommercial(null);
			entrepriseView.setDateBouclementFuture(pm.getDateBouclementFuture());
			entrepriseView.setSieges(getSieges(pm.getSieges()));
			entrepriseView.setFormesJuridiques(getFormesJuridiques(pm.getFormesJuridiques()));
			entrepriseView.setCapitaux(getCapitaux(pm.getCapitaux()));
		}

		return entrepriseView;
	}

	private List<SiegeView> getSieges(List<Siege> sieges) {
		if (sieges == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<SiegeView>(sieges.size());
		for (Siege siege : sieges) {
			list.add(new SiegeView(siege));
		}
		Collections.sort(list, new DateRangeComparator<SiegeView>());
		Collections.reverse(list);
		return list;
	}

	private List<FormeJuridiqueView> getFormesJuridiques(List<FormeJuridique> formesJuridiques) {
		if (formesJuridiques == null) {
			return null;
		}
		final List<FormeJuridiqueView> list = new ArrayList<FormeJuridiqueView>(formesJuridiques.size());
		for (FormeJuridique formeJuridique : formesJuridiques) {
			list.add(new FormeJuridiqueView(formeJuridique));
		}
		Collections.sort(list, new DateRangeComparator<FormeJuridiqueView>());
		Collections.reverse(list);
		return list;
	}

	private List<CapitalView> getCapitaux(List<Capital> capitaux) {
		if (capitaux == null) {
			return null;
		}
		final List<CapitalView> list = new ArrayList<CapitalView>(capitaux.size());
		for (Capital capital : capitaux) {
			list.add(new CapitalView(capital));
		}
		Collections.sort(list, new DateRangeComparator<CapitalView>());
		Collections.reverse(list);
		return list;
	}

}
