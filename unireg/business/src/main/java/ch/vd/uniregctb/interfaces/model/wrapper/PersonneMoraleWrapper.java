package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.EtatPM;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.RegimeFiscal;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.AssujettissementPMWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.CapitalWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.CompteBancaireWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.EtatPMWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.ForPMWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.FormeJuridiqueWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.MandatWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.RegimeFiscalWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.SiegeWrapper;

public class PersonneMoraleWrapper implements PersonneMorale {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateBouclementFuture;
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

	private final ch.vd.registre.pm.model.PersonneMorale target;

	public static PersonneMoraleWrapper get(ch.vd.registre.pm.model.PersonneMorale target) {
		if (target == null) {
			return null;
		}
		return new PersonneMoraleWrapper(target);
	}

	private PersonneMoraleWrapper(ch.vd.registre.pm.model.PersonneMorale target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateConstitution());
		this.dateFin = RegDate.get(target.getDateFinActivite());
		this.dateBouclementFuture = RegDate.get(target.getDateBouclementFuture());
	}

	public Collection<AdresseEntreprise> getAdresses() {
		if (adresses == null) {
			initAdresses();
		}
		return adresses;
	}

	private void initAdresses() {
		synchronized (this) {
			if (adresses == null) {
				adresses = new ArrayList<AdresseEntreprise>();
				final Collection<?> targetAdresses = target.getAdresses();
				if (targetAdresses != null) {
					for (Object o : targetAdresses) {
						ch.vd.registre.pm.model.AdresseEntreprise a = (ch.vd.registre.pm.model.AdresseEntreprise) o;
						adresses.add(AdresseEntrepriseWrapper.get(a));
					}
				}
			}
		}
	}

	public RegDate getDateConstitution() {
		return dateDebut;
	}

	public RegDate getDateFinActivite() {
		return dateFin;
	}

	public List<FormeJuridique> getFormesJuridiques() {
		if (formesJuridiques == null) {
			initFormesJuridiques();
		}
		return formesJuridiques;
	}

	private void initFormesJuridiques() {
		synchronized (this) {
			if (formesJuridiques == null) {
				formesJuridiques = new ArrayList<FormeJuridique>();
				final List<?> targetFormes = target.getFormesJuridiques();
				if (targetFormes != null) {
					for (Object o : targetFormes) {
						ch.vd.registre.pm.model.FormeJuridique f = (ch.vd.registre.pm.model.FormeJuridique) o;
						formesJuridiques.add(FormeJuridiqueWrapper.get(f));
					}
				}
			}
		}
	}

	public String getNomContact() {
		return target.getNomContact();
	}

	public List<CompteBancaire> getComptesBancaires() {
		if (comptesBancaires == null) {
			initComptesBancaires();
		}
		return comptesBancaires;
	}

	private void initComptesBancaires() {
		synchronized (this) {
			if (comptesBancaires == null) {
				comptesBancaires = new ArrayList<CompteBancaire>();
				final Collection<?> targetComptes = target.getComptesBancaires();
				if (targetComptes != null) {
					for (Object o : targetComptes) {
						ch.vd.registre.pm.model.CompteBancaire c = (ch.vd.registre.pm.model.CompteBancaire) o;
						comptesBancaires.add(CompteBancaireWrapper.get(c));
					}
				}
			}
		}
	}

	public long getNumeroEntreprise() {
		return target.getNumeroEntreprise();
	}

	public String getRaisonSociale() {
		return target.getRaisonSociale();
	}

	public String getRaisonSociale1() {
		return target.getRaisonSociale1();
	}

	public String getRaisonSociale2() {
		return target.getRaisonSociale2();
	}

	public String getRaisonSociale3() {
		return target.getRaisonSociale3();
	}

	public String getTelecopieContact() {
		return target.getTelecopieContact();
	}

	public String getDesignationAbregee() {
		return target.getDesignationAbregee();
	}

	public String getTelephoneContact() {
		return target.getTelephoneContact();
	}

	public String getTitulaireCompte() {
		return target.getTitulaireCompte();
	}

	public RegDate getDateBouclementFuture() {
		return dateBouclementFuture;
	}

	public String getNumeroIPMRO() {
		return target.getNumeroIPMRO();
	}

	public List<Capital> getCapitaux() {
		if (capitaux == null) {
			initCapitaux();
		}
		return capitaux;
	}

	private void initCapitaux() {
		synchronized (this) {
			if (capitaux == null) {
				capitaux = new ArrayList<Capital>();
				final Collection<?> targetCapitaux = target.getCapitaux();
				if (targetCapitaux != null) {
					for (Object o : targetCapitaux) {
						ch.vd.registre.pm.model.Capital c = (ch.vd.registre.pm.model.Capital) o;
						capitaux.add(CapitalWrapper.get(c));
					}
				}
			}
		}
	}

	public List<EtatPM> getEtats() {
		if (etats == null) {
			initEtats();
		}
		return etats;
	}

	private void initEtats() {
		synchronized (this) {
			if (etats == null) {
				etats = new ArrayList<EtatPM>();
				final Collection<?> targetEtats = target.getEtats();
				if (targetEtats != null) {
					for (Object o : targetEtats) {
						ch.vd.registre.pm.model.EtatPM e = (ch.vd.registre.pm.model.EtatPM) o;
						etats.add(EtatPMWrapper.get(e));
					}
				}
			}
		}
	}

	public List<RegimeFiscal> getRegimesVD() {
		if (regimesVD == null) {
			initRegimesVD();
		}
		return regimesVD;
	}

	private void initRegimesVD() {
		synchronized (this) {
			if (regimesVD == null) {
				regimesVD = new ArrayList<RegimeFiscal>();
				final Collection<?> targetRegimes = target.getRegimesVD();
				if (targetRegimes != null) {
					for (Object o : targetRegimes) {
						ch.vd.registre.pm.model.RegimeFiscal r = (ch.vd.registre.pm.model.RegimeFiscal) o;
						regimesVD.add(RegimeFiscalWrapper.get(r));
					}
				}
			}
		}
	}

	public List<RegimeFiscal> getRegimesCH() {
		if (regimesCH == null) {
			initRegimesCH();
		}
		return regimesCH;
	}

	private void initRegimesCH() {
		synchronized (this) {
			if (regimesCH == null) {
				regimesCH = new ArrayList<RegimeFiscal>();
				final Collection<?> targetRegimes = target.getRegimesCH();
				if (targetRegimes != null) {
					for (Object o : targetRegimes) {
						ch.vd.registre.pm.model.RegimeFiscal r = (ch.vd.registre.pm.model.RegimeFiscal) o;
						regimesCH.add(RegimeFiscalWrapper.get(r));
					}
				}
			}
		}
	}

	public List<Siege> getSieges() {
		if (sieges == null) {
			initSieges();
		}
		return sieges;
	}

	private void initSieges() {
		synchronized (this) {
			if (sieges == null) {
				sieges = new ArrayList<Siege>();
				final Collection<?> targetSieges = target.getSieges();
				if (targetSieges != null) {
					for (Object o : targetSieges) {
						ch.vd.registre.pm.model.Siege s = (ch.vd.registre.pm.model.Siege) o;
						sieges.add(SiegeWrapper.get(s));
					}
				}
			}
		}
	}

	public List<AssujettissementPM> getAssujettissementsLIC() {
		if (assujettissementsLIC == null) {
			initAssujettissementsLIC();
		}
		return assujettissementsLIC;
	}

	private void initAssujettissementsLIC() {
		synchronized (this) {
			if (assujettissementsLIC == null) {
				assujettissementsLIC = new ArrayList<AssujettissementPM>();
				final Collection<?> targetAssujettissements = target.getAssujettissementsLIC();
				if (targetAssujettissements != null) {
					for (Object o : targetAssujettissements) {
						ch.vd.registre.fiscal.model.Assujettissement a = (ch.vd.registre.fiscal.model.Assujettissement) o;
						assujettissementsLIC.add(AssujettissementPMWrapper.get(a));
					}
				}
			}
		}
	}

	public List<AssujettissementPM> getAssujettissementsLIFD() {
		if (assujettissementsLIFD == null) {
			initAssujettissementsLIFD();
		}
		return assujettissementsLIFD;
	}

	private void initAssujettissementsLIFD() {
		synchronized (this) {
			if (assujettissementsLIFD == null) {
				assujettissementsLIFD = new ArrayList<AssujettissementPM>();
				final Collection<?> targetAssujettissements = target.getAssujettissementsLIFD();
				if (targetAssujettissements != null) {
					for (Object o : targetAssujettissements) {
						ch.vd.registre.fiscal.model.Assujettissement a = (ch.vd.registre.fiscal.model.Assujettissement) o;
						assujettissementsLIFD.add(AssujettissementPMWrapper.get(a));
					}
				}
			}
		}
	}

	public List<ForPM> getForsFiscauxPrincipaux() {
		if (forsFiscauxPrincipaux == null) {
			initForsFiscauxPrincipaux();
		}
		return forsFiscauxPrincipaux;
	}

	private void initForsFiscauxPrincipaux() {
		synchronized (this) {
			if (forsFiscauxPrincipaux == null) {
				forsFiscauxPrincipaux = new ArrayList<ForPM>();
				final Collection<?> targetFors = target.getForsFiscauxPrincipaux();
				if (targetFors != null) {
					for (Object o : targetFors) {
						ch.vd.registre.pm.model.ForPM a = (ch.vd.registre.pm.model.ForPM) o;
						forsFiscauxPrincipaux.add(ForPMWrapper.get(a));
					}
				}
			}
		}
	}

	public List<ForPM> getForsFiscauxSecondaires() {
		if (forsFiscauxSecondaires == null) {
			initForsFiscauxSecondaires();
		}
		return forsFiscauxSecondaires;
	}

	private void initForsFiscauxSecondaires() {
		synchronized (this) {
			if (forsFiscauxSecondaires == null) {
				forsFiscauxSecondaires = new ArrayList<ForPM>();
				final Collection<?> targetFors = target.getForsFiscauxSecondaires();
				if (targetFors != null) {
					for (Object o : targetFors) {
						ch.vd.registre.pm.model.ForPM a = (ch.vd.registre.pm.model.ForPM) o;
						forsFiscauxSecondaires.add(ForPMWrapper.get(a));
					}
				}
			}
		}
	}

	public List<Mandat> getMandats() {
		if (mandats == null) {
			initMandats();
		}
		return mandats;
	}

	private void initMandats() {
		synchronized (this) {
			if (mandats == null) {
				mandats = new ArrayList<Mandat>();
				final List<ch.vd.registre.pm.model.Mandat> targetMandats = target.getMandats();
				if (targetMandats != null) {
					for (Object o : targetMandats) {
						ch.vd.registre.pm.model.Mandat m = (ch.vd.registre.pm.model.Mandat) o;
						mandats.add(MandatWrapper.get(m));
					}
				}
			}
		}
	}
}
