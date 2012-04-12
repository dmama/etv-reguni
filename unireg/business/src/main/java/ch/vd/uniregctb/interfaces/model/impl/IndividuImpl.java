package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.civil.model.HistoriqueIndividu;
import ch.vd.uniregctb.common.StringHelper;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.EtatCivilListHost;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.Tutelle;

public class IndividuImpl extends EntiteCivileImpl implements Individu, Serializable {

	private static final long serialVersionUID = 6263014813568645977L;

	private final long noTechnique;
	private final String prenom;
	private final String autresPrenoms;
	private final String nom;
	private final String nomNaissance;
	private final String noAVS11;
	private final String nouveauNoAVS;
	private final String numeroRCE;
	private final boolean isMasculin;
	private Collection<AdoptionReconnaissance> adoptions;
	private final RegDate deces;
	private final RegDate naissance;
	private final RegDate dateArriveeVD;
	private List<RelationVersIndividu> parents;
	private List<RelationVersIndividu> conjoints;
	private Collection<RelationVersIndividu> enfants;
	private final EtatCivilListHost etatsCivils;
	private List<Nationalite> nationalites;
	private Collection<Origine> origines;
	private List<Permis> permis;
	private Tutelle tutelle;
	private final Set<AttributeIndividu> availableParts = new HashSet<AttributeIndividu>();

	public static IndividuImpl get(ch.vd.registre.civil.model.Individu target, RegDate upTo, AttributeIndividu... parts) {
		if (target == null) {
			return null;
		}
		return new IndividuImpl(target, upTo, parts);
	}

	protected IndividuImpl(ch.vd.registre.civil.model.Individu target, RegDate upTo, AttributeIndividu... parts) {
		super(target, upTo);
		final ch.vd.registre.civil.model.HistoriqueIndividu dhi = target.getDernierHistoriqueIndividu();
		if (dhi == null) {
			this.prenom = null;
			this.autresPrenoms = null;
			this.nom = null;
			this.nomNaissance = null;
			this.noAVS11 = null;
		}
		else {
			this.prenom = dhi.getPrenom();
			this.autresPrenoms = dhi.getAutresPrenoms();
			this.nom = dhi.getNom();
			this.nomNaissance = dhi.getNomNaissance();
			this.noAVS11 = dhi.getNoAVS();
		}
		this.noTechnique = target.getNoTechnique();
		this.nouveauNoAVS = initNouveauNoAVS(target.getNouveauNoAVS());
		this.numeroRCE = initNumeroRCE(target.getNumeroRCE());
		this.isMasculin = target.isSexeMasculin();
		this.adoptions = initAdoptions(target.getAdoptionsReconnaissances(), upTo);
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
		//noinspection unchecked
		this.dateArriveeVD = initDateArriveVD(target.getHistoriqueIndividu());
		this.parents = initParents(this.naissance, target.getPere(), target.getMere(), upTo);
		this.enfants = initEnfants(target.getEnfants(), upTo);
		this.etatsCivils = initEtatsCivils(target, upTo);
		this.conjoints = initConjoint(this.etatsCivils, upTo);
		this.nationalites = initNationalites(target.getNationalites(), upTo);
		this.origines = initOrigines(target.getOrigines());
		this.permis = initPermis(target.getPermis(), upTo);
		this.tutelle = TutelleImpl.get(target.getTutelle(), upTo);

		if (parts != null) {
			Collections.addAll(this.availableParts, parts);
			this.availableParts.add(AttributeIndividu.CONJOINTS); // les conjoints sont systématiquement renseignés par host-interfaces
		}
	}

	protected IndividuImpl(IndividuImpl individuWrapper, Set<AttributeIndividu> parts) {
		super(individuWrapper, parts);
		this.prenom = individuWrapper.getPrenom();
		this.autresPrenoms = individuWrapper.getAutresPrenoms();
		this.nom = individuWrapper.getNom();
		this.nomNaissance = individuWrapper.getNomNaissance();
		this.noTechnique = individuWrapper.noTechnique;
		this.noAVS11 = individuWrapper.getNoAVS11();
		this.nouveauNoAVS = individuWrapper.nouveauNoAVS;
		this.numeroRCE = individuWrapper.numeroRCE;
		this.isMasculin = individuWrapper.isMasculin;
		this.deces = individuWrapper.deces;
		this.naissance = individuWrapper.naissance;
		this.dateArriveeVD = individuWrapper.dateArriveeVD;
		this.etatsCivils = individuWrapper.etatsCivils;

		if (parts != null && parts.contains(AttributeIndividu.ADOPTIONS)) {
			adoptions = individuWrapper.adoptions;
		}
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = individuWrapper.conjoints;
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

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	@Override
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	@Override
	public String getNom() {
		return nom;
	}

	@Override
	public String getNomNaissance() {
		return nomNaissance;
	}

	@Override
	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptions;
	}

	private static boolean isValidUpTo(Date dateDebut, Date upTo) {
		return upTo == null || dateDebut == null || !dateDebut.after(upTo);
	}

	private static boolean isValidUpTo(RegDate dateDebut, RegDate upTo) {
		return upTo == null || dateDebut == null || dateDebut.isBeforeOrEqual(upTo);
	}

	private static List<AdoptionReconnaissance> initAdoptions(Collection<?> targetAdoptions, RegDate upTo) {
		if (targetAdoptions == null || targetAdoptions.isEmpty()) {
			return null;
		}
		final List<AdoptionReconnaissance> list = new ArrayList<AdoptionReconnaissance>();
		final Date upToJava = upTo == null ? null : upTo.asJavaDate();
		for (Object o : targetAdoptions) {
			final ch.vd.registre.civil.model.AdoptionReconnaissance a = (ch.vd.registre.civil.model.AdoptionReconnaissance) o;
			final Date dateDebut = (a.getDateAdoption() == null ? a.getDateReconnaissance() : a.getDateAdoption());
			if (isValidUpTo(dateDebut, upToJava)) {
				list.add(AdoptionReconnaissanceImpl.get(a, upTo));
			}
		}
		return list;
	}

	private static List<Origine> initOrigines(Collection<ch.vd.registre.civil.model.Origine> targetOrigines) {
		if (targetOrigines == null || targetOrigines.isEmpty()) {
			return null;
		}
		final List<Origine> list;
		final Set<Origine> set = new LinkedHashSet<Origine>();
		for (ch.vd.registre.civil.model.Origine origine : targetOrigines) {
			set.add(OrigineImpl.get(origine));
		}
		list = new ArrayList<Origine>(set);     // pour éliminer les doublons...
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
	public RegDate getDateArriveeVD() {
		return dateArriveeVD;
	}

	private static RegDate initDateArriveVD(Collection<HistoriqueIndividu> historiqueIndividu) {
		if (historiqueIndividu == null || historiqueIndividu.isEmpty()) {
			return null;
		}
		return RegDate.get(historiqueIndividu.iterator().next().getDateDebutValidite());
	}

	@Override
	public boolean isMineur(RegDate date) {
		return naissance != null && naissance.addYears(18).compareTo(date) > 0;
	}

	@Override
	public List<RelationVersIndividu> getParents() {
		return parents;
	}

	private static List<RelationVersIndividu> initParents(RegDate dateNaissance, ch.vd.registre.civil.model.Individu pere, ch.vd.registre.civil.model.Individu mere, RegDate upTo) {

		if (pere == null && mere == null) {
			return null;
		}

		final Individu m = IndividuImpl.get(mere, upTo);
		final Individu p = IndividuImpl.get(pere, upTo);

		final List<RelationVersIndividu> parents = new ArrayList<RelationVersIndividu>(2);
		if (p != null) {
			parents.add(new RelationVersIndividuImpl(p.getNoTechnique(), dateNaissance, p.getDateDeces()));
		}
		if (m != null) {
			parents.add(new RelationVersIndividuImpl(m.getNoTechnique(), dateNaissance, m.getDateDeces()));
		}

		return parents;
	}

	@Override
	public Collection<RelationVersIndividu> getEnfants() {
		return enfants;
	}

	private static List<RelationVersIndividu> initEnfants(Collection<?> targetEnfants, RegDate upTo) {
		if (targetEnfants == null || targetEnfants.isEmpty()) {
			return null;
		}
		final List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		final Date upToJava = upTo == null ? null : upTo.asJavaDate();
		for (Object o : targetEnfants) {
			final ch.vd.registre.civil.model.Individu i = (ch.vd.registre.civil.model.Individu) o;
			if (isValidUpTo(i.getDateNaissance(), upToJava)) {
				list.add(new RelationVersIndividuImpl(i.getNoTechnique(), RegDate.get(i.getDateNaissance()), RegDate.get(i.getDateDeces())));
			}
		}
		return list;
	}

	@Override
	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	private static EtatCivilListHost initEtatsCivils(ch.vd.registre.civil.model.Individu individu, RegDate upTo) {
		final ArrayList<EtatCivilImpl> etatsCivils = new ArrayList<EtatCivilImpl>();
		final Collection<?> targetEtatsCivils = individu.getEtatsCivils();
		if (targetEtatsCivils != null) {
			final Date upToJava = upTo == null ? null : upTo.asJavaDate();
			for (Object o : targetEtatsCivils) {
				ch.vd.registre.civil.model.EtatCivil e = (ch.vd.registre.civil.model.EtatCivil) o;
				if (e.getDateDebutValidite() == null && e.getNoSequence() == 0 && e.getTypeEtatCivil() == null) {
					// host-interface retourne un état-civil vide si l'individu n'en a pas du tout dans la base...
					continue;
				}
				if (isValidUpTo(e.getDateDebutValidite(), upToJava)) {
					etatsCivils.add(EtatCivilImpl.get(e));
				}
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
	public List<RelationVersIndividu> getConjoints() {
		return conjoints;
	}

	private static List<RelationVersIndividu> initConjoint(EtatCivilListHost etatsCivils, RegDate upTo) {
		List<RelationVersIndividu> list = new ArrayList<RelationVersIndividu>();
		if (etatsCivils != null) {
			for (EtatCivil etatCivil : etatsCivils) {
				Long numeroConjoint = ((EtatCivilImpl) etatCivil).getNumeroConjoint();
				if (numeroConjoint != null && numeroConjoint > 0 && isValidUpTo(etatCivil.getDateDebut(), upTo)) {
					list.add(new RelationVersIndividuImpl(numeroConjoint, etatCivil.getDateDebut(), etatCivil.getDateFin()));
				}
			}
		}
		return list.isEmpty() ? null : list;
	}

	@Override
	public List<Nationalite> getNationalites() {
		return nationalites;
	}

	private static List<Nationalite> initNationalites(Collection<?> targetNationalites, RegDate upTo) {
		if (targetNationalites == null || targetNationalites.isEmpty()) {
			return null;
		}
		final List<Nationalite> list = new ArrayList<Nationalite>();
		final Date upToJava = upTo == null ? null : upTo.asJavaDate();
		for (Object o : targetNationalites) {
			ch.vd.registre.civil.model.Nationalite n = (ch.vd.registre.civil.model.Nationalite) o;
			if (isValidUpTo(n.getDateDebutValidite(), upToJava)) {
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
	public String getNoAVS11() {
		return noAVS11;
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

	private static String initNumeroRCE(String numeroRCE) {
		if (StringUtils.isBlank(numeroRCE) || "0".equals(numeroRCE)) {
			return null;
		}
		// supprime tous les caractères non-numérique et pad avec des zéros pour obtenir le même formatting que RcPers
		numeroRCE = numeroRCE.replaceAll("[^0-9]*", "");
		numeroRCE = StringHelper.lpad(numeroRCE, 9, '0');
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

	private List<Permis> initPermis(Collection<?> targetPermis, RegDate upTo) {
		if (targetPermis == null || targetPermis.isEmpty()) {
			return null;
		}

		final List<Permis> permis = new ArrayList<Permis>();
		final Date upToJava = upTo == null ? null : upTo.asJavaDate();
		for (Object o : targetPermis) {
			ch.vd.registre.civil.model.Permis p = (ch.vd.registre.civil.model.Permis) o;
			if (isValidUpTo(p.getDateDebutValidite(), upToJava)) {
				permis.add(PermisImpl.get(p));
			}
		}

		// on trie immédiatement la liste par ordre croissant d'obtention des permis
		Collections.sort(permis, new PermisComparator());

		return permis;
	}

	private static class PermisComparator implements Comparator<Permis> {
		@Override
		public int compare(Permis o1, Permis o2) {
			final PermisImpl p1 = (PermisImpl) o1;
			final PermisImpl p2 = (PermisImpl) o2;
			if (RegDateHelper.equals(p1.getDateDebut(), p2.getDateDebut())) {
				return p1.getNoSequence() - p2.getNoSequence();
			}
			else {
				return NullDateBehavior.EARLIEST.compare(p1.getDateDebut(), p2.getDateDebut());
			}
		}
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
		if (parts != null && parts.contains(AttributeIndividu.CONJOINTS)) {
			conjoints = individu.getConjoints();
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

		if (parts != null) {
			this.availableParts.addAll(parts);
		}
	}

	@Override
	public Individu clone(Set<AttributeIndividu> parts) {
		return new IndividuImpl(this, parts);
	}

	@Override
	public Set<AttributeIndividu> getAvailableParts() {
		return availableParts;
	}
}
