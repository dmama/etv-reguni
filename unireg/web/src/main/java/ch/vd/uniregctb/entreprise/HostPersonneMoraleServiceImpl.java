package ch.vd.uniregctb.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.EtatPM;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.RegimeFiscal;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.service.PartPM;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.view.EtatPMView;
import ch.vd.uniregctb.tiers.view.RegimeFiscalView;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 */
public class HostPersonneMoraleServiceImpl implements HostPersonneMoraleService {

	private ServicePersonneMoraleService servicePersonneMoraleService;
	private ServiceInfrastructureService serviceInfra;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePersonneMoraleService(ServicePersonneMoraleService servicePersonneMoraleService) {
		this.servicePersonneMoraleService = servicePersonneMoraleService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	public EntrepriseView get(Long numeroEntreprise) {

		EntrepriseView entrepriseView = new EntrepriseView();

		final PersonneMorale pm = servicePersonneMoraleService.getPersonneMorale(numeroEntreprise, PartPM.ADRESSES, PartPM.FORMES_JURIDIQUES, PartPM.CAPITAUX, PartPM.SIEGES, PartPM.REGIMES_FISCAUX, PartPM.ETATS);
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
			entrepriseView.setRegimesFiscauxVD(getRegimesFiscaux(pm.getRegimesVD()));
			entrepriseView.setRegimesFiscauxCH(getRegimesFiscaux(pm.getRegimesCH()));
			entrepriseView.setEtats(getEtatsPM(pm.getEtats()));
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

	private List<RegimeFiscalView> getRegimesFiscaux(List<RegimeFiscal> regimes) {
		if (regimes == null) {
			return null;
		}
		final List<RegimeFiscalView> list = new ArrayList<RegimeFiscalView>(regimes.size());
		for (RegimeFiscal r : regimes) {
			final RegimeFiscalView v = new RegimeFiscalView();
			v.setDateDebut(r.getDateDebut());
			v.setDateFin(r.getDateFin());
			v.setCode(r.getCode());
			try {
				final TypeRegimeFiscal type = serviceInfra.getTypeRegimeFiscal(r.getCode());
				if (type != null) {
					v.setLibelle(type.getLibelle());
				}
			}
			catch (InfrastructureException e) {
				throw new RuntimeException(e);
			}
			list.add(v);
		}
		Collections.sort(list, new DateRangeComparator<RegimeFiscalView>());
		Collections.reverse(list);
		return list;
	}

	private List<EtatPMView> getEtatsPM(List<EtatPM> etats) {
		if (etats == null) {
			return null;
		}
		final List<EtatPMView> list = new ArrayList<EtatPMView>(etats.size());
		for (EtatPM r : etats) {
			final EtatPMView v = new EtatPMView();
			v.setDateDebut(r.getDateDebut());
			v.setDateFin(r.getDateFin());
			v.setCode(r.getCode());
			try {
				final TypeEtatPM type = serviceInfra.getTypeEtatPM(r.getCode());
				if (type != null) {
					v.setLibelle(type.getLibelle());
				}
			}
			catch (InfrastructureException e) {
				throw new RuntimeException(e);
			}
			list.add(v);
		}
		Collections.sort(list, new DateRangeComparator<EtatPMView>());
		Collections.reverse(list);
		return list;
	}


}
