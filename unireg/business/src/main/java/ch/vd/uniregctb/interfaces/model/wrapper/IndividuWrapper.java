package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.helper.IndividuHelper;

public class IndividuWrapper extends EntiteCivileWrapper implements Individu, Serializable {

	private static final long serialVersionUID = -2767453068069111885L;

	private final long noTechnique;
	private final String nouveauNoAVS;
	private final String numeroRCE;
	private final boolean isMasculin;
	private Collection<AdoptionReconnaissance> adoptions;
	private final RegDate deces;
	private final RegDate naissance;
	private final HistoriqueIndividu dernierHistorique;
	private final Collection<HistoriqueIndividu> historique;
	private Collection<Individu> enfants;
	private final EtatCivilList etatsCivils;
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
		this.noTechnique = target.getNoTechnique();
		this.nouveauNoAVS = initNouveauNoAVS(target.getNouveauNoAVS());
		this.numeroRCE = target.getNumeroRCE();
		this.isMasculin = target.isSexeMasculin();
		this.adoptions = initAdoptions(target.getAdoptionsReconnaissances());
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
		this.dernierHistorique = HistoriqueIndividuWrapper.get(target.getDernierHistoriqueIndividu());
		this.historique = initHistorique(target.getHistoriqueIndividu());
		this.enfants = initEnfants(target.getEnfants());
		this.etatsCivils = initEtatsCivils(target);
		this.mere = IndividuWrapper.get(target.getMere());
		this.nationalites = initNationalites(target.getNationalites());
		this.origine = OrigineWrapper.get(target.getOrigine());
		this.pere = IndividuWrapper.get(target.getPere());
		this.permis = initPermis(target.getPermis());
		this.tutelle = TutelleWrapper.get(target.getTutelle());
	}

	protected IndividuWrapper(IndividuWrapper individuWrapper, Set<AttributeIndividu> parts) {
		super(individuWrapper, parts);
		this.noTechnique = individuWrapper.noTechnique;
		this.nouveauNoAVS = individuWrapper.nouveauNoAVS;
		this.numeroRCE = individuWrapper.numeroRCE;
		this.isMasculin = individuWrapper.isMasculin;
		this.deces = individuWrapper.deces;
		this.naissance = individuWrapper.naissance;
		this.dernierHistorique = individuWrapper.dernierHistorique;
		this.historique = individuWrapper.historique;
		this.etatsCivils = individuWrapper.etatsCivils;

		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptions = individuWrapper.adoptions;
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = individuWrapper.enfants;
		}
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = individuWrapper.nationalites;
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origine = individuWrapper.origine;
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			pere = individuWrapper.pere;
			mere = individuWrapper.mere;
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = individuWrapper.permis;
		}
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = individuWrapper.tutelle;
		}
	}


	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptions;
	}

	private List<AdoptionReconnaissance> initAdoptions(Collection<?> targetAdoptions) {
		final List<AdoptionReconnaissance> list = new ArrayList<AdoptionReconnaissance>();
		if (targetAdoptions != null) {
			for (Object o : targetAdoptions) {
				ch.vd.registre.civil.model.AdoptionReconnaissance a = (ch.vd.registre.civil.model.AdoptionReconnaissance) o;
				list.add(AdoptionReconnaissanceWrapper.get(a));
			}
		}
		return list;
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
		return dernierHistorique;
	}

	public Collection<Individu> getEnfants() {
		return enfants;
	}

	private List<Individu> initEnfants(Collection<?> targetEnfants) {
		final List<Individu> list = new ArrayList<Individu>();
		if (targetEnfants != null) {
			for (Object o : targetEnfants) {
				ch.vd.registre.civil.model.Individu i = (ch.vd.registre.civil.model.Individu) o;
				list.add(IndividuWrapper.get(i));
			}
		}
		return list;
	}

	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	private static EtatCivilList initEtatsCivils(ch.vd.registre.civil.model.Individu individu) {
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
		return historique;
	}

	public HistoriqueIndividu getHistoriqueIndividuAt(RegDate date) {
		HistoriqueIndividu candidat = null;
		for (HistoriqueIndividu histo : historique) {
			final RegDate dateDebut = histo.getDateDebutValidite();
			if (dateDebut == null || date == null || dateDebut.isBeforeOrEqual(date)) {
				candidat = histo;
			}
		}
		return candidat;
	}

	private List<HistoriqueIndividu> initHistorique(Collection<?> targetHistorique) {
		final List<HistoriqueIndividu> list = new ArrayList<HistoriqueIndividu>();
		if (targetHistorique != null) {
			for (Object o : targetHistorique) {
				ch.vd.registre.civil.model.HistoriqueIndividu h = (ch.vd.registre.civil.model.HistoriqueIndividu) o;
				list.add(HistoriqueIndividuWrapper.get(h));
			}
		}
		return list;
	}

	public Individu getMere() {
		return mere;
	}

	public Collection<Nationalite> getNationalites() {
		return nationalites;
	}

	private List<Nationalite> initNationalites(Collection<?> targetNationalites) {
		final List<Nationalite> list = new ArrayList<Nationalite>();
		if (targetNationalites != null) {
			for (Object o : targetNationalites) {
				ch.vd.registre.civil.model.Nationalite n = (ch.vd.registre.civil.model.Nationalite) o;
				list.add(NationaliteWrapper.get(n));
			}
		}
		return list;
	}

	public long getNoTechnique() {
		return noTechnique;
	}

	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	private static String initNouveauNoAVS(String nouveauNoAVS) {
		String numero = nouveauNoAVS;
		// [UNIREG-1223] interprète la valeur "0" comme une valeur nulle
		if (numero == null || "".equals(numero) || "0".equals(numero)) {
			numero = null;
		}
		return numero;
	}

	public String getNumeroRCE() {
		return numeroRCE;
	}

	public Origine getOrigine() {
		return origine;
	}

	public Individu getPere() {
		return pere;
	}

	public Collection<Permis> getPermis() {
		return permis;
	}

	private List<Permis> initPermis(Collection<?> targetPermis) {
		final List<Permis> permis = new ArrayList<Permis>();
		if (targetPermis != null) {
			for (Object o : targetPermis) {
				ch.vd.registre.civil.model.Permis p = (ch.vd.registre.civil.model.Permis) o;
				permis.add(PermisWrapper.get(p));
			}
		}
		return permis;
	}

	public Tutelle getTutelle() {
		return tutelle;
	}

	public boolean isSexeMasculin() {
		return isMasculin;
	}

	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		super.copyPartsFrom(individu, parts);
		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptions = individu.getAdoptionsReconnaissances();
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINT)) {
			//conjoint = individu.getConjoint();
		}
		if (parts != null && parts.contains(AttributeIndividu.ENFANTS)) {
			enfants = individu.getEnfants();
		}
		if (parts != null && parts.contains(AttributeIndividu.NATIONALITE)) {
			nationalites = individu.getNationalites();
		}
		if (parts != null && parts.contains(AttributeIndividu.ORIGINE)) {
			origine = individu.getOrigine();
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			pere = individu.getPere();
			mere = individu.getMere();
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = individu.getPermis();
		}
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = individu.getTutelle();
		}
	}

	public Individu clone(Set<AttributeIndividu> parts) {
		return new IndividuWrapper(this, parts);
	}

	public Permis getPermisActif(RegDate date) {
		return IndividuHelper.getPermisActif(this, date);
	}
}
