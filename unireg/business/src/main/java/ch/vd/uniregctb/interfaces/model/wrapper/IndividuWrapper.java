package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.helper.IndividuHelper;

public class IndividuWrapper extends EntiteCivileWrapper implements Individu {

	private final ch.vd.registre.civil.model.Individu target;
	private Collection<AdoptionReconnaissance> adoptions;
	private Individu conjoint;
	private final RegDate deces;
	private final RegDate naissance;
	private HistoriqueIndividu dernierHistorique;
	private Collection<HistoriqueIndividu> historique;
	private Collection<Individu> enfants;
	private EtatCivilList etatsCivils;
	private Individu mere;
	private Collection<Nationalite> nationalites;
	private Origine origine;
	private Individu pere;
	private Collection<Permis> permis;
	private Tutelle tutelle;

	public static IndividuWrapper get(ch.vd.registre.civil.model.Individu target) {
		if (target == null) {
			return null;
		}
		return new IndividuWrapper(target);
	}

	protected IndividuWrapper(ch.vd.registre.civil.model.Individu target) {
		super(target);
		this.target = target;
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
	}

	protected IndividuWrapper(IndividuWrapper individuWrapper, Set<EnumAttributeIndividu> parts) {
		super(individuWrapper, parts);
		this.target = individuWrapper.target;
		this.deces = individuWrapper.deces;
		this.naissance = individuWrapper.naissance;
		this.dernierHistorique = individuWrapper.dernierHistorique;
		this.historique = individuWrapper.historique;
		this.etatsCivils = individuWrapper.etatsCivils;

		if (parts != null && parts.contains(EnumAttributeIndividu.ADOPTIONS)) {
			adoptions = individuWrapper.adoptions;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.CONJOINT)) {
			conjoint = individuWrapper.conjoint;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.ENFANTS)) {
			enfants = individuWrapper.enfants;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.NATIONALITE)) {
			nationalites = individuWrapper.nationalites;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.ORIGINE)) {
			origine = individuWrapper.origine;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.PARENTS)) {
			pere = individuWrapper.pere;
			mere = individuWrapper.mere;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.PERMIS)) {
			permis = individuWrapper.permis;
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.TUTELLE)) {
			tutelle = individuWrapper.tutelle;
		}
	}


	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		if (adoptions == null) {
			initAdoptions();
		}
		return adoptions;
	}

	private void initAdoptions() {
		synchronized (this) {
			if (adoptions == null) {
				adoptions = new ArrayList<AdoptionReconnaissance>();
				final Collection<?> targetAdoptions = target.getAdoptionsReconnaissances();
				if (targetAdoptions != null) {
					for (Object o : targetAdoptions) {
						ch.vd.registre.civil.model.AdoptionReconnaissance a = (ch.vd.registre.civil.model.AdoptionReconnaissance) o;
						adoptions.add(AdoptionReconnaissanceWrapper.get(a));
					}
				}
			}
		}
	}

	public RegDate getDateDeces() {
		return deces;
	}

	public RegDate getDateNaissance() {
		return naissance;
	}

	public boolean isMineur(RegDate date) {
		return naissance != null && naissance.addYears(18).compareTo(date) > 0;
	}

	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		if (dernierHistorique == null) {
			dernierHistorique = HistoriqueIndividuWrapper.get(target.getDernierHistoriqueIndividu());
		}
		return dernierHistorique;
	}

	public Collection<Individu> getEnfants() {
		if (enfants == null) {
			initEnfants();
		}
		return enfants;
	}

	private void initEnfants() {
		synchronized (this) {
			if (enfants == null) {
				enfants = new ArrayList<Individu>();
				final Collection<?> targetEnfants = target.getEnfants();
				if (targetEnfants != null) {
					for (Object o : targetEnfants) {
						ch.vd.registre.civil.model.Individu i = (ch.vd.registre.civil.model.Individu) o;
						enfants.add(IndividuWrapper.get(i));
					}
				}
			}
		}
	}

	public EtatCivilList getEtatsCivils() {
		if (etatsCivils == null) {
			initEtatsCivils();
		}
		return etatsCivils;
	}

	private void initEtatsCivils() {
		synchronized (this) {
			if (etatsCivils == null) {
				etatsCivils = extractEtatsCivils(target);
			}
		}
	}

	private static EtatCivilList extractEtatsCivils(ch.vd.registre.civil.model.Individu individu) {
		final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
		final Collection<?> targetEtatsCivils = individu.getEtatsCivils();
		if (targetEtatsCivils != null) {
			for (Object o : targetEtatsCivils) {
				ch.vd.registre.civil.model.EtatCivil e = (ch.vd.registre.civil.model.EtatCivil) o;
				if (e.getDateDebutValidite() == null && e.getNoSequence() == 0 && e.getTypeEtatCivil() == null) {
					// host-interface retourne un état-civil vide si l'individu n'en a pas du tout dans la base...
					continue;
				}
				etatsCivils.add(EtatCivilWrapper.get(e));
			}
		}
		return new EtatCivilList(individu.getNoTechnique(), etatsCivils);
	}

	public EtatCivil getEtatCivilCourant() {

		EtatCivil etatCivilCourant = null;

		int noSequence = -1;
		for (EtatCivil etatCivil : getEtatsCivils()) {
			if (etatCivil.getNoSequence() > noSequence) {
				etatCivilCourant = etatCivil;
				noSequence = etatCivil.getNoSequence();
			}
		}

		return etatCivilCourant;
	}

	public EtatCivil getEtatCivil(RegDate date) {
		return getEtatsCivils().getEtatCivilAt(date);
	}

	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		if (historique == null) {
			initHistorique();
		}
		return historique;
	}

	private void initHistorique() {
		synchronized (this) {
			if (historique == null) {
				historique = new ArrayList<HistoriqueIndividu>();
				final Collection<?> targetHistorique = target.getHistoriqueIndividu();
				if (targetHistorique != null) {
					for (Object o : targetHistorique) {
						ch.vd.registre.civil.model.HistoriqueIndividu h = (ch.vd.registre.civil.model.HistoriqueIndividu) o;
						historique.add(HistoriqueIndividuWrapper.get(h));
					}
				}
			}
		}
	}

	public Individu getMere() {
		if (mere == null) {
			mere = IndividuWrapper.get(target.getMere());
		}
		return mere;
	}

	public Collection<Nationalite> getNationalites() {
		if (nationalites == null) {
			initNationalites();
		}
		return nationalites;
	}

	private void initNationalites() {
		synchronized (this) {
			if (nationalites == null) {
				Collection<?> targetNationalites = target.getNationalites();
				if (targetNationalites != null) {
					nationalites = new ArrayList<Nationalite>();
					for (Object o : targetNationalites) {
						ch.vd.registre.civil.model.Nationalite n = (ch.vd.registre.civil.model.Nationalite) o;
						nationalites.add(NationaliteWrapper.get(n));
					}
				}
			}
		}
	}

	public long getNoTechnique() {
		return target.getNoTechnique();
	}

	public String getNouveauNoAVS() {
		final String numero = target.getNouveauNoAVS();
		// [UNIREG-1223] interprète la valeur "0" comme une valeur nulle
		if (numero == null || "".equals(numero) || "0".equals(numero)) {
			return null;
		}
		return numero;
	}

	public String getNumeroRCE() {
		return target.getNumeroRCE();
	}

	public Origine getOrigine() {
		if (origine == null) {
			origine = OrigineWrapper.get(target.getOrigine());
		}
		return origine;
	}

	public Individu getPere() {
		if (pere == null) {
			pere = IndividuWrapper.get(target.getPere());
		}
		return pere;
	}

	public Collection<Permis> getPermis() {
		if (permis == null) {
			initPermis();
		}
		return permis;
	}

	private void initPermis() {
		synchronized (this) {
			if (permis == null) {
				Collection<?> targetPermis = target.getPermis();
				if (targetPermis != null) {
					permis = new ArrayList<Permis>();
					for (Object o : targetPermis) {
						ch.vd.registre.civil.model.Permis p = (ch.vd.registre.civil.model.Permis) o;
						permis.add(PermisWrapper.get(p));
					}
				}
			}
		}
	}

	public Tutelle getTutelle() {
		if (tutelle == null) {
			tutelle = TutelleWrapper.get(target.getTutelle());
		}
		return tutelle;
	}

	public boolean isSexeMasculin() {
		return target.isSexeMasculin();
	}

	public void copyPartsFrom(Individu individu, Set<EnumAttributeIndividu> parts) {
		super.copyPartsFrom(individu, parts);
		if (parts != null && parts.contains(EnumAttributeIndividu.ADOPTIONS)) {
			adoptions = individu.getAdoptionsReconnaissances();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.CONJOINT)) {
			//conjoint = individu.getConjoint();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.ENFANTS)) {
			enfants = individu.getEnfants();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.NATIONALITE)) {
			nationalites = individu.getNationalites();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.ORIGINE)) {
			origine = individu.getOrigine();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.PARENTS)) {
			pere = individu.getPere();
			mere = individu.getMere();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.PERMIS)) {
			permis = individu.getPermis();
		}
		if (parts != null && parts.contains(EnumAttributeIndividu.TUTELLE)) {
			tutelle = individu.getTutelle();
		}
	}

	public Individu clone(Set<EnumAttributeIndividu> parts) {
		return new IndividuWrapper(this, parts);
	}

	public Permis getPermisActif(RegDate date) {
		return IndividuHelper.getPermisActif(this, date);
	}
}
