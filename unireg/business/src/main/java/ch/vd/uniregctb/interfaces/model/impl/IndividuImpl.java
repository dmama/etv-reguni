package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.EtatCivilListHost;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.helper.IndividuHelper;

public class IndividuImpl extends EntiteCivileImpl implements Individu, Serializable {

	private static final long serialVersionUID = -963453831766356538L;

	private final long noTechnique;
	private final String nouveauNoAVS;
	private final String numeroRCE;
	private final boolean isMasculin;
	private Collection<AdoptionReconnaissance> adoptions;
	private final RegDate deces;
	private final RegDate naissance;
	private final HistoriqueIndividu dernierHistorique;
	private final Collection<HistoriqueIndividu> historique;
	private List<RelationVersIndividu> parents;
	private Collection<RelationVersIndividu> enfants;
	private final EtatCivilListHost etatsCivils;
	private List<Nationalite> nationalites;
	private Collection<Origine> origines;
	private List<Permis> permis;
	private Tutelle tutelle;

	public static IndividuImpl get(ch.vd.registre.civil.model.Individu target) {
		if (target == null) {
			return null;
		}
		return new IndividuImpl(target);
	}

	protected IndividuImpl(ch.vd.registre.civil.model.Individu target) {
		super(target);
		this.noTechnique = target.getNoTechnique();
		this.nouveauNoAVS = initNouveauNoAVS(target.getNouveauNoAVS());
		this.numeroRCE = target.getNumeroRCE();
		this.isMasculin = target.isSexeMasculin();
		this.adoptions = initAdoptions(target.getAdoptionsReconnaissances());
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
		this.dernierHistorique = HistoriqueIndividuImpl.get(target.getDernierHistoriqueIndividu());
		this.historique = initHistorique(target.getHistoriqueIndividu());
		this.parents = initParents(target.getPere(), target.getMere());
		this.enfants = initEnfants(target.getEnfants());
		this.etatsCivils = initEtatsCivils(target);
		this.nationalites = initNationalites(target.getNationalites());
		this.origines = initOrigines(target.getOrigines());
		this.permis = initPermis(target.getPermis());
		this.tutelle = TutelleImpl.get(target.getTutelle());
	}

	protected IndividuImpl(IndividuImpl individuWrapper, Set<AttributeIndividu> parts) {
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
			origines = individuWrapper.origines;
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			parents = individuWrapper.parents;
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = individuWrapper.permis;
		}
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = individuWrapper.tutelle;
		}
	}


	@Override
	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptions;
	}

	private static List<AdoptionReconnaissance> initAdoptions(Collection<?> targetAdoptions) {
		final List<AdoptionReconnaissance> list = new ArrayList<AdoptionReconnaissance>();
		if (targetAdoptions != null) {
			for (Object o : targetAdoptions) {
				ch.vd.registre.civil.model.AdoptionReconnaissance a = (ch.vd.registre.civil.model.AdoptionReconnaissance) o;
				list.add(AdoptionReconnaissanceImpl.get(a));
			}
		}
		return list;
	}

	private static List<Origine> initOrigines(Collection<ch.vd.registre.civil.model.Origine> targetOrigines) {
		final List<Origine> list;
		if (targetOrigines != null) {
			final Set<Origine> set = new LinkedHashSet<Origine>();
			for (ch.vd.registre.civil.model.Origine origine : targetOrigines) {
				set.add(OrigineImpl.get(origine));
			}
			list = new ArrayList<Origine>(set);     // pour éliminer les doublons...
		}
		else {
			list = Collections.emptyList();
		}
		return list;
	}

	@Override
	public RegDate getDateDeces() {
		return deces;
	}

	@Override
	public RegDate getDateNaissance() {
		return naissance;
	}

	@Override
	public boolean isMineur(RegDate date) {
		return naissance != null && naissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		return dernierHistorique;
	}

	@Override
	public List<RelationVersIndividu> getParents() {
		return parents;
	}

	private static List<RelationVersIndividu> initParents(ch.vd.registre.civil.model.Individu pere, ch.vd.registre.civil.model.Individu mere) {
		final Individu m = IndividuImpl.get(mere);
		final Individu p = IndividuImpl.get(pere);
		final List<RelationVersIndividu> parents = new ArrayList<RelationVersIndividu>(2);
		if (p != null) {
			parents.add(new RelationVersIndividuImpl(p.getNoTechnique(), p.getDateNaissance(), p.getDateDeces()));
		}
		if (m != null) {
			parents.add(new RelationVersIndividuImpl(m.getNoTechnique(), m.getDateNaissance(), m.getDateDeces()));
		}
		return parents;
	}

	@Override
	public Collection<RelationVersIndividu> getEnfants() {
		return enfants;
	}

	private static List<RelationVersIndividu> initEnfants(Collection<?> targetEnfants) {
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		if (targetEnfants != null) {
			for (Object o : targetEnfants) {
				final ch.vd.registre.civil.model.Individu i = (ch.vd.registre.civil.model.Individu) o;
				list.add(new RelationVersIndividuImpl(i.getNoTechnique(), RegDate.get(i.getDateNaissance()), RegDate.get(i.getDateDeces())));
			}
		}
		return list;
	}

	@Override
	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	private static EtatCivilListHost initEtatsCivils(ch.vd.registre.civil.model.Individu individu) {
		final ArrayList<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
		final Collection<?> targetEtatsCivils = individu.getEtatsCivils();
		if (targetEtatsCivils != null) {
			for (Object o : targetEtatsCivils) {
				ch.vd.registre.civil.model.EtatCivil e = (ch.vd.registre.civil.model.EtatCivil) o;
				if (e.getDateDebutValidite() == null && e.getNoSequence() == 0 && e.getTypeEtatCivil() == null) {
					// host-interface retourne un état-civil vide si l'individu n'en a pas du tout dans la base...
					continue;
				}
				etatsCivils.add(EtatCivilImpl.get(e));
			}
		}
		return new EtatCivilListHost(individu.getNoTechnique(), etatsCivils);
	}

	@Override
	public EtatCivil getEtatCivilCourant() {
		if (etatsCivils == null || etatsCivils.isEmpty()) {
			return null;
		}
		return etatsCivils.get(etatsCivils.size() - 1);
	}

	@Override
	public EtatCivil getEtatCivil(RegDate date) {
		return etatsCivils.getEtatCivilAt(date);
	}

	@Override
	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		return historique;
	}

	@Override
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
				list.add(HistoriqueIndividuImpl.get(h));
			}
		}
		return list;
	}

	@Override
	public List<Nationalite> getNationalites() {
		return nationalites;
	}

	private static List<Nationalite> initNationalites(Collection<?> targetNationalites) {
		final List<Nationalite> list = new ArrayList<Nationalite>();
		if (targetNationalites != null) {
			for (Object o : targetNationalites) {
				ch.vd.registre.civil.model.Nationalite n = (ch.vd.registre.civil.model.Nationalite) o;
				list.add(NationaliteImpl.get(n));
			}
		}
		return list;
	}

	@Override
	public long getNoTechnique() {
		return noTechnique;
	}

	@Override
	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	private static String initNouveauNoAVS(String nouveauNoAVS) {
		String numero = StringUtils.trimToNull(nouveauNoAVS);
		// [UNIREG-1223] interprète la valeur "0" comme une valeur nulle
		if (numero == null || "0".equals(numero)) {
			numero = null;
		}
		return numero;
	}

	@Override
	public String getNumeroRCE() {
		return numeroRCE;
	}

	@Override
	public Collection<Origine> getOrigines() {
		return origines;
	}

	@Override
	public List<Permis> getPermis() {
		return permis;
	}

	private List<Permis> initPermis(Collection<?> targetPermis) {
		final List<Permis> permis = new ArrayList<Permis>();
		if (targetPermis != null) {
			for (Object o : targetPermis) {
				ch.vd.registre.civil.model.Permis p = (ch.vd.registre.civil.model.Permis) o;
				permis.add(PermisImpl.get(p));
			}
		}

		// on trie immédiatement la liste par ordre croissant d'obtention des permis
		Collections.sort(permis, new PermisComparator());

		return permis;
	}

	@Override
	public Tutelle getTutelle() {
		return tutelle;
	}

	@Override
	public boolean isSexeMasculin() {
		return isMasculin;
	}

	@Override
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
			origines = individu.getOrigines();
		}
		if (parts != null && parts.contains(AttributeIndividu.PARENTS)) {
			parents = individu.getParents();
		}
		if (parts != null && parts.contains(AttributeIndividu.PERMIS)) {
			permis = individu.getPermis();
		}
		if (parts != null && parts.contains(AttributeIndividu.TUTELLE)) {
			tutelle = individu.getTutelle();
		}
	}

	@Override
	public Individu clone(Set<AttributeIndividu> parts) {
		return new IndividuImpl(this, parts);
	}

	@Override
	public Permis getPermisActif(RegDate date) {
		return IndividuHelper.getPermisActif(this, date);
	}

	private static class PermisComparator implements Comparator<Permis> {
		@Override
		public int compare(Permis o1, Permis o2) {
			final PermisImpl p1 = (PermisImpl) o1;
			final PermisImpl p2 = (PermisImpl) o2;
			if (RegDateHelper.equals(p1.getDateDebutValidite(), p2.getDateDebutValidite())) {
				return p1.getNoSequence() - p2.getNoSequence();
			}
			else {
				return RegDateHelper.isBeforeOrEqual(p1.getDateDebutValidite(), p2.getDateDebutValidite(), NullDateBehavior.EARLIEST) ? -1 : 1;
			}
		}
	}
}
