package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntrepriseImpl;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.EtatPM;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.RegimeFiscal;
import ch.vd.uniregctb.interfaces.model.Siege;

public class PersonneMoraleImpl implements PersonneMorale, Serializable {

	private static final long serialVersionUID = 8851449527201576037L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateBouclementFuture;
	private final String designationAbregee;
	private final String telephoneContact;
	private final String titulaireCompte;
	private final String numeroIPMRO;
	private final String telecopieContact;
	private final String raisonSociale3;
	private final String raisonSociale2;
	private final String raisonSociale1;
	private final String raisonSociale;
	private final long numeroEntreprise;
	private final String nomContact;

	private Collection<AdresseEntreprise> adresses = null;
	private List<FormeJuridique> formesJuridiques = null;
	private List<CompteBancaire> comptesBancaires = null;
	private List<Capital> capitaux = null;
	private List<EtatPM> etats = null;
	private List<RegimeFiscal> regimesVD = null;
	private List<RegimeFiscal> regimesCH = null;
	private List<Siege> sieges = null;
	private List<AssujettissementPM> assujettissementsLIC = null;
	private List<AssujettissementPM> assujettissementsLIFD = null;
	private List<ForPM> forsFiscauxPrincipaux = null;
	private List<ForPM> forsFiscauxSecondaires = null;
	private List<Mandat> mandats = null;


	public static PersonneMoraleImpl get(ch.vd.registre.pm.model.PersonneMorale target) {
		if (target == null) {
			return null;
		}
		return new PersonneMoraleImpl(target);
	}

	private PersonneMoraleImpl(ch.vd.registre.pm.model.PersonneMorale target) {
		this.dateDebut = RegDate.get(target.getDateConstitution());
		this.dateFin = RegDate.get(target.getDateFinActivite());
		this.dateBouclementFuture = RegDate.get(target.getDateBouclementFuture());
		this.designationAbregee = target.getDesignationAbregee();
		this.telephoneContact = target.getTelephoneContact();
		this.titulaireCompte = target.getTitulaireCompte();
		this.numeroIPMRO = target.getNumeroIPMRO();
		this.telecopieContact = target.getTelecopieContact();
		this.raisonSociale1 = target.getRaisonSociale1();
		this.raisonSociale2 = target.getRaisonSociale2();
		this.raisonSociale3 = target.getRaisonSociale3();
		this.raisonSociale = target.getRaisonSociale();
		this.numeroEntreprise = target.getNumeroEntreprise();
		this.nomContact = target.getNomContact();

		this.adresses = initAdresses(target.getAdresses());
		this.formesJuridiques = initFormesJuridiques(target.getFormesJuridiques());
		this.comptesBancaires = initComptesBancaires(target.getComptesBancaires());
		this.capitaux = initCapitaux(target.getCapitaux());
		this.etats = initEtats(target.getEtats());
		this.regimesVD = initRegimesVD(target.getRegimesVD());
		this.regimesCH = initRegimesCH(target.getRegimesCH());
		this.sieges = initSieges(target.getSieges());
		this.assujettissementsLIC = initAssujettissementsLIC(target.getAssujettissementsLIC());
		this.assujettissementsLIFD = initAssujettissementsLIFD(target.getAssujettissementsLIFD());
		this.forsFiscauxPrincipaux = initForsFiscauxPrincipaux(target.getForsFiscauxPrincipaux());
		this.forsFiscauxSecondaires = initForsFiscauxSecondaires(target.getForsFiscauxSecondaires());
		this.mandats = initMandats(target.getMandats());
	}

	public PersonneMoraleImpl(PersonneMoraleImpl right, Set<PartPM> parts) {
		this.dateDebut = right.dateDebut;
		this.dateFin = right.dateFin;
		this.dateBouclementFuture = right.dateBouclementFuture;
		this.designationAbregee = right.designationAbregee;
		this.telephoneContact = right.telephoneContact;
		this.titulaireCompte = right.titulaireCompte;
		this.numeroIPMRO = right.numeroIPMRO;
		this.telecopieContact = right.telecopieContact;
		this.raisonSociale3 = right.raisonSociale3;
		this.raisonSociale2 = right.raisonSociale2;
		this.raisonSociale1 = right.raisonSociale1;
		this.raisonSociale = right.raisonSociale;
		this.numeroEntreprise = right.numeroEntreprise;
		this.nomContact = right.nomContact;
		this.comptesBancaires = right.comptesBancaires;

		if (parts != null && parts.contains(PartPM.ADRESSES)) {
			this.adresses = right.adresses;
		}
		if (parts != null && parts.contains(PartPM.FORMES_JURIDIQUES)) {
			this.formesJuridiques = right.formesJuridiques;
		}
		if (parts != null && parts.contains(PartPM.CAPITAUX)) {
			this.capitaux = right.capitaux;
		}
		if (parts != null && parts.contains(PartPM.ETATS)) {
			this.etats = right.etats;
		}
		if (parts != null && parts.contains(PartPM.REGIMES_FISCAUX)) {
			this.regimesVD = right.regimesVD;
			this.regimesCH = right.regimesCH;
		}
		if (parts != null && parts.contains(PartPM.SIEGES)) {
			this.sieges = right.sieges;
		}
		if (parts != null && parts.contains(PartPM.ASSUJETTISSEMENTS)) {
			this.assujettissementsLIC = right.assujettissementsLIC;
			this.assujettissementsLIFD = right.assujettissementsLIFD;
		}
		if (parts != null && parts.contains(PartPM.FORS_FISCAUX)) {
			this.forsFiscauxPrincipaux = right.forsFiscauxPrincipaux;
			this.forsFiscauxSecondaires = right.forsFiscauxSecondaires;
		}
		if (parts != null && parts.contains(PartPM.MANDATS)) {
			this.mandats = right.mandats;
		}
	}
	
	@Override
	public Collection<AdresseEntreprise> getAdresses() {
		return adresses;
	}

	private List<AdresseEntreprise> initAdresses(Collection<?> targetAdresses) {
		List<AdresseEntreprise> adresses = new ArrayList<AdresseEntreprise>();
		if (targetAdresses != null) {
			for (Object o : targetAdresses) {
				ch.vd.registre.pm.model.AdresseEntreprise a = (ch.vd.registre.pm.model.AdresseEntreprise) o;
				AdresseEntrepriseImpl ae = AdresseEntrepriseImpl.get(a);
				if (ae != null) {
					adresses.add(ae);
				}
			}
		}
		return adresses;
	}

	@Override
	public RegDate getDateConstitution() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinActivite() {
		return dateFin;
	}

	@Override
	public List<FormeJuridique> getFormesJuridiques() {
		return formesJuridiques;
	}

	private List<FormeJuridique> initFormesJuridiques(List<?> targetFormes) {
		List<FormeJuridique> formesJuridiques = new ArrayList<FormeJuridique>();
		if (targetFormes != null) {
			for (Object o : targetFormes) {
				ch.vd.registre.pm.model.FormeJuridique f = (ch.vd.registre.pm.model.FormeJuridique) o;
				formesJuridiques.add(FormeJuridiqueImpl.get(f));
			}
		}
		return formesJuridiques;
	}

	@Override
	public String getNomContact() {
		return nomContact;
	}

	@Override
	public List<CompteBancaire> getComptesBancaires() {
		return comptesBancaires;
	}

	private List<CompteBancaire> initComptesBancaires(Collection<?> targetComptes) {
		List<CompteBancaire> comptesBancaires = new ArrayList<CompteBancaire>();
		if (targetComptes != null) {
			for (Object o : targetComptes) {
				ch.vd.registre.pm.model.CompteBancaire c = (ch.vd.registre.pm.model.CompteBancaire) o;
				comptesBancaires.add(CompteBancaireImpl.get(c));
			}
		}
		return comptesBancaires;
	}

	@Override
	public long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	@Override
	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public String getRaisonSociale1() {
		return raisonSociale1;
	}

	@Override
	public String getRaisonSociale2() {
		return raisonSociale2;
	}

	@Override
	public String getRaisonSociale3() {
		return raisonSociale3;
	}

	@Override
	public String getTelecopieContact() {
		return telecopieContact;
	}

	@Override
	public String getDesignationAbregee() {
		return designationAbregee;
	}

	@Override
	public String getTelephoneContact() {
		return telephoneContact;
	}

	@Override
	public String getTitulaireCompte() {
		return titulaireCompte;
	}

	@Override
	public RegDate getDateBouclementFuture() {
		return dateBouclementFuture;
	}

	@Override
	public String getNumeroIPMRO() {
		return numeroIPMRO;
	}

	@Override
	public List<Capital> getCapitaux() {
		return capitaux;
	}

	private List<Capital> initCapitaux(Collection<?> targetCapitaux) {
		List<Capital> capitaux = new ArrayList<Capital>();
		if (targetCapitaux != null) {
			for (Object o : targetCapitaux) {
				ch.vd.registre.pm.model.Capital c = (ch.vd.registre.pm.model.Capital) o;
				capitaux.add(CapitalImpl.get(c));
			}
		}
		return capitaux;
	}

	@Override
	public List<EtatPM> getEtats() {
		return etats;
	}

	private List<EtatPM> initEtats(Collection<?> targetEtats) {
		List<EtatPM> etats = new ArrayList<EtatPM>();
		if (targetEtats != null) {
			for (Object o : targetEtats) {
				ch.vd.registre.pm.model.EtatPM e = (ch.vd.registre.pm.model.EtatPM) o;
				etats.add(EtatPMImpl.get(e));
			}
		}
		return etats;
	}

	@Override
	public List<RegimeFiscal> getRegimesVD() {
		return regimesVD;
	}

	private List<RegimeFiscal> initRegimesVD(Collection<?> targetRegimes) {
		List<RegimeFiscal> regimesVD = new ArrayList<RegimeFiscal>();
		if (targetRegimes != null) {
			for (Object o : targetRegimes) {
				ch.vd.registre.pm.model.RegimeFiscal r = (ch.vd.registre.pm.model.RegimeFiscal) o;
				regimesVD.add(RegimeFiscalImpl.get(r));
			}
		}
		return regimesVD;
	}

	@Override
	public List<RegimeFiscal> getRegimesCH() {
		return regimesCH;
	}

	private List<RegimeFiscal> initRegimesCH(Collection<?> targetRegimes) {
		List<RegimeFiscal> regimesCH = new ArrayList<RegimeFiscal>();
		if (targetRegimes != null) {
			for (Object o : targetRegimes) {
				ch.vd.registre.pm.model.RegimeFiscal r = (ch.vd.registre.pm.model.RegimeFiscal) o;
				regimesCH.add(RegimeFiscalImpl.get(r));
			}
		}
		return regimesCH;
	}

	@Override
	public List<Siege> getSieges() {
		return sieges;
	}

	private List<Siege> initSieges(Collection<?> targetSieges) {
		List<Siege> sieges = new ArrayList<Siege>();
		if (targetSieges != null) {
			for (Object o : targetSieges) {
				ch.vd.registre.pm.model.Siege s = (ch.vd.registre.pm.model.Siege) o;
				sieges.add(SiegeImpl.get(s));
			}
		}
		return sieges;
	}

	@Override
	public List<AssujettissementPM> getAssujettissementsLIC() {
		return assujettissementsLIC;
	}

	private List<AssujettissementPM> initAssujettissementsLIC(Collection<?> targetAssujettissements) {
		List<AssujettissementPM> assujettissementsLIC = new ArrayList<AssujettissementPM>();
		if (targetAssujettissements != null) {
			for (Object o : targetAssujettissements) {
				ch.vd.registre.fiscal.model.Assujettissement a = (ch.vd.registre.fiscal.model.Assujettissement) o;
				assujettissementsLIC.add(AssujettissementPMImpl.get(a));
			}
		}
		return assujettissementsLIC;
	}

	@Override
	public List<AssujettissementPM> getAssujettissementsLIFD() {
		return assujettissementsLIFD;
	}

	private List<AssujettissementPM> initAssujettissementsLIFD(Collection<?> targetAssujettissements) {
		List<AssujettissementPM> assujettissementsLIFD = new ArrayList<AssujettissementPM>();
		if (targetAssujettissements != null) {
			for (Object o : targetAssujettissements) {
				ch.vd.registre.fiscal.model.Assujettissement a = (ch.vd.registre.fiscal.model.Assujettissement) o;
				assujettissementsLIFD.add(AssujettissementPMImpl.get(a));
			}
		}
		return assujettissementsLIFD;
	}

	@Override
	public List<ForPM> getForsFiscauxPrincipaux() {
		return forsFiscauxPrincipaux;
	}

	private List<ForPM> initForsFiscauxPrincipaux(Collection<?> targetFors) {
		List<ForPM> forsFiscauxPrincipaux = new ArrayList<ForPM>();
		if (targetFors != null) {
			for (Object o : targetFors) {
				ch.vd.registre.pm.model.ForPM a = (ch.vd.registre.pm.model.ForPM) o;
				forsFiscauxPrincipaux.add(ForPMImpl.get(a));
			}
		}
		return forsFiscauxPrincipaux;
	}

	@Override
	public List<ForPM> getForsFiscauxSecondaires() {
		return forsFiscauxSecondaires;
	}

	private List<ForPM> initForsFiscauxSecondaires(Collection<?> targetFors) {
		List<ForPM> forsFiscauxSecondaires = new ArrayList<ForPM>();
		if (targetFors != null) {
			for (Object o : targetFors) {
				ch.vd.registre.pm.model.ForPM a = (ch.vd.registre.pm.model.ForPM) o;
				forsFiscauxSecondaires.add(ForPMImpl.get(a));
			}
		}
		return forsFiscauxSecondaires;
	}

	@Override
	public List<Mandat> getMandats() {
		return mandats;
	}

	private List<Mandat> initMandats(List<ch.vd.registre.pm.model.Mandat> targetMandats) {
		List<Mandat> mandats = new ArrayList<Mandat>();
		if (targetMandats != null) {
			for (Object o : targetMandats) {
				ch.vd.registre.pm.model.Mandat m = (ch.vd.registre.pm.model.Mandat) o;
				mandats.add(MandatImpl.get(m));
			}
		}
		return mandats;
	}

	@Override
	public void copyPartsFrom(PersonneMorale pm, Set<PartPM> parts) {

		if (parts != null && parts.contains(PartPM.ADRESSES)) {
			adresses = pm.getAdresses();
		}
		if (parts != null && parts.contains(PartPM.FORMES_JURIDIQUES)) {
			formesJuridiques = pm.getFormesJuridiques();
		}
		if (parts != null && parts.contains(PartPM.CAPITAUX)) {
			capitaux = pm.getCapitaux();
		}
		if (parts != null && parts.contains(PartPM.ETATS)) {
			etats = pm.getEtats();
		}
		if (parts != null && parts.contains(PartPM.REGIMES_FISCAUX)) {
			regimesVD = pm.getRegimesVD();
			regimesCH = pm.getRegimesCH();
		}
		if (parts != null && parts.contains(PartPM.SIEGES)) {
			sieges = pm.getSieges();
		}
		if (parts != null && parts.contains(PartPM.ASSUJETTISSEMENTS)) {
			assujettissementsLIC = pm.getAssujettissementsLIC();
			assujettissementsLIFD = pm.getAssujettissementsLIFD();
		}
		if (parts != null && parts.contains(PartPM.FORS_FISCAUX)) {
			forsFiscauxPrincipaux = pm.getForsFiscauxPrincipaux();
			forsFiscauxSecondaires = pm.getForsFiscauxSecondaires();
		}
		if (parts != null && parts.contains(PartPM.MANDATS)) {
			mandats = pm.getMandats();
		}
	}

	@Override
	public PersonneMorale clone(Set<PartPM> parts) {
		return new PersonneMoraleImpl(this, parts);
	}
}
