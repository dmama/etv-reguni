package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationAvecNumeroSequence;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.type.MotifFor;

@Entity
public abstract class Contribuable extends Tiers {

	private Set<MouvementDossier> mouvementsDossier;
	private Set<Immeuble> immeubles;
	private Set<IdentificationEntreprise> identificationsEntreprise;
	private Set<AdresseMandataire> adressesMandataires;

	private RegDate dateLimiteExclusionEnvoiDeclarationImpot;
	private Set<DecisionAci> decisionsAci;
	private Set<DroitAcces> droitsAccesAppliques;
	private Set<RapprochementRF> rapprochementsRF;

	public Contribuable() {
	}

	public Contribuable(long numero) {
		super(numero);
	}

	@OneToMany(mappedBy = "contribuable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_MOV_DOS_CTB_ID")
	public Set<MouvementDossier> getMouvementsDossier() {
		return mouvementsDossier;
	}

	public void setMouvementsDossier(Set<MouvementDossier> theMouvementDossier) {
		mouvementsDossier = theMouvementDossier;
	}

	@OneToMany(mappedBy = "contribuable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_IMM_CTB_ID")
	public Set<Immeuble> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(Set<Immeuble> immeubles) {
		this.immeubles = immeubles;
	}

	/**
	 * @return la date jusqu'à laquelle les tâches d'envoi de déclarations d'impôts ne doivent pas être traitées par les <b>batches</b>; ou
	 *         <i>null</i> si les tâches peuvent être traitées sans restriction.
	 */
	@Column(name = "DATE_LIMITE_EXCLUSION", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateLimiteExclusionEnvoiDeclarationImpot() {
		return dateLimiteExclusionEnvoiDeclarationImpot;
	}

	public void setDateLimiteExclusionEnvoiDeclarationImpot(RegDate dateLimiteExclusionEnvoiDeclarationImpot) {
		this.dateLimiteExclusionEnvoiDeclarationImpot = dateLimiteExclusionEnvoiDeclarationImpot;
	}

	@OneToMany(mappedBy = "tiers", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_DA_TRS_ID")
	public Set<DroitAcces> getDroitsAccesAppliques() {
		return droitsAccesAppliques;
	}

	public void setDroitsAccesAppliques(Set<DroitAcces> droitsAccesAppliques) {
		this.droitsAccesAppliques = droitsAccesAppliques;
	}

	// ***********************************************
	/**
	 * Ajoute un mouvement de dossier
	 *
	 * @param nouveauMouvementDossier
	 *            le mouvement de dossier à ajouter
	 */
	public void addMouvementDossier(MouvementDossier nouveauMouvementDossier) {
		if (this.mouvementsDossier == null) {
			this.mouvementsDossier = new HashSet<>();
		}
		nouveauMouvementDossier.setContribuable(this);
		this.mouvementsDossier.add(nouveauMouvementDossier);
	}

	/**
	 * Ajoute un immeuble au contribuable.
	 *
	 * @param immeuble l'immeuble à ajouter
	 */
	public void addImmeuble(Immeuble immeuble) {
		if (immeubles == null) {
			this.immeubles = new HashSet<>();
		}
		immeuble.setContribuable(this);
		this.immeubles.add(immeuble);
	}

	/**
	 * Liste de fors fiscaux spécialisée de manière à ne stocker que les fors débutant le plus tôt (= date de début).
	 * <p>
	 * Dans la majorité des cas, cette liste ne contiendra qu'un seul for. Ce n'est que dans le cas où plusieurs fors débutent en même temps
	 * qu'elle en contiendra plusieurs.
	 */
	protected static class FirstForsList extends ArrayList<ForFiscal> {

		private static final long serialVersionUID = 5767109372815011124L;

		/**
		 * Teste un for comme candidat à l'insertion dans la liste des fors. Un candidat est accepté dans la liste si sa date de début est
		 * plus petite ou égale à celle des fors existants dans la liste. Dans le cas où sa date de début est plus petite, les fors
		 * existants sont enlevés de la liste.
		 *
		 * @return <b>true</b> si le for a été inséré dans la liste, <b>false</b> autrement.
		 */
		@SuppressWarnings("SimplifiableIfStatement")
        public boolean checkFor(ForFiscal candidat) {
			final int s = size();
			if (s == 0) {
				// pas de fors dans la liste -> on ajoute le nouveau
				return add(candidat);
			}
			else {
				final RegDate dateDebut = get(0).getDateDebut(); // tous les fors dans la liste sont sensés posséder la même date de début
				final RegDate candidatDateDebut = candidat.getDateDebut();

				if (dateDebut == null) {
					if (candidatDateDebut == null) {
						// le nouveau for débute en même temps que les fors stockés dans la liste -> on l'ajoute
						return add(candidat);
					}
					else {
						// le nouveau for est après les fors stockés dans la liste -> on l'ignore
						return false;
					}
				}
				else {
					if (candidatDateDebut == null || candidatDateDebut.isBefore(dateDebut)) {
						// le nouveau for est avant les fors stockés dans la liste -> on vide la liste et ajoute le for
						clear();
						return add(candidat);
					}
					else if (candidatDateDebut.isAfter(dateDebut)) {
						// le nouveau for est après les fors stockés dans la liste -> on l'ignore
						return false;
					}
					else {
						// le nouveau for débute en même temps que les fors stockés dans la liste -> on l'ajoute
						return add(candidat);
					}
				}
			}
		}

		/**
		 * @return le premier for fiscal dont le numéro Ofs est celui spécifié en paramètre.
		 */
		public ForFiscal findForWithNumeroOfs(Integer noOfs) {
			Assert.notNull(noOfs);
			for (ForFiscal f : this) {
				if (noOfs.equals(f.getNumeroOfsAutoriteFiscale())) {
					return f;
				}
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final Contribuable other = (Contribuable) obj;
		return ComparisonHelper.areEqual(identificationsEntreprise, other.identificationsEntreprise);
	}

	@Override
	@Transient
	protected boolean isDesactiveSelonFors(@Nullable RegDate date) {
		// pour un contribuable, on dira qu'il est désactivé à une date donnée s'il n'y a pas de for
		// principal actif à la date donnée et que le dernier for fiscal principal a été fermé
		// pour un motif "ANNULATION"

		final boolean desactive;
		final ForFiscalPrincipal ffpCourant = getForFiscalPrincipalAt(date);
		if (ffpCourant == null) {
			final ForFiscalPrincipal ffpPrecedent = getDernierForFiscalPrincipalAvant(date);
			desactive = ffpPrecedent != null && ffpPrecedent.getMotifFermeture() == MotifFor.ANNULATION;
		}
		else {
			desactive = false;
		}
		return desactive;
	}

	@Override
	@Transient
	public RegDate getDateDesactivation() {
		final RegDate date;
		final ForFiscalPrincipal ffpCourant = getForFiscalPrincipalAt(null);
		if (ffpCourant == null) {
			final ForFiscalPrincipal dernier = getDernierForFiscalPrincipal();
			date = dernier != null && dernier.getMotifFermeture() == MotifFor.ANNULATION ? dernier.getDateFin() : null;
		}
		else {
			date = null;
		}
		return date;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_IDE_TIERS_ID")
	public Set<IdentificationEntreprise> getIdentificationsEntreprise() {
		return identificationsEntreprise;
	}

	/**
	 * @see PersonnePhysique#setIdentificationsPersonnes(java.util.Set)
	 */
	public void setIdentificationsEntreprise(@Nullable Set<IdentificationEntreprise> identificationsEntreprise) {
		if (this.identificationsEntreprise == null || this.identificationsEntreprise instanceof HashSet) {
			this.identificationsEntreprise = identificationsEntreprise;
		}
		else if (this.identificationsEntreprise == identificationsEntreprise) {
			// pas de changement -> rien à faire
		}
		else {
			this.identificationsEntreprise.clear();
			if (identificationsEntreprise != null) {
				this.identificationsEntreprise.addAll(identificationsEntreprise);
			}
		}
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "CTB_ID", nullable = false)
	@ForeignKey(name = "FK_ADR_MAND_CTB_ID")
	public Set<AdresseMandataire> getAdressesMandataires() {
		return adressesMandataires;
	}

	public void setAdressesMandataires(Set<AdresseMandataire> adressesMandataires) {
		this.adressesMandataires = adressesMandataires;
	}

	public void addAdresseMandataire(AdresseMandataire adresse) {
		if (this.adressesMandataires == null) {
			this.adressesMandataires = new HashSet<>();
		}
		adresse.setMandant(this);
		this.adressesMandataires.add(adresse);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_DECISION_ACI_TRS_ID")
	public Set<DecisionAci> getDecisionsAci() {
		return decisionsAci;
	}

	@Transient
	public List<DecisionAci> getDecisionsSorted() {
		List<DecisionAci> decisions = null;
		if (decisionsAci != null) {
			decisions = new ArrayList<>();
			for (DecisionAci decisionAci : decisionsAci) {
				if (!decisionAci.isAnnule()) {
					decisions.add(decisionAci);
				}
			}
			Collections.sort(decisions, new DateRangeComparator<DecisionAci>() {
				@Override
				public int compare(DecisionAci o1, DecisionAci o2) {
					int comparisonDates = super.compare(o1, o2);
					if (comparisonDates == 0) {
						// à dates égales, il faut comparer selon le type d'autorité fiscale
						return o1.getTypeAutoriteFiscale().ordinal() - o2.getTypeAutoriteFiscale().ordinal();
					}
					else {
						return comparisonDates;
					}
				}
			});
		}
		return decisions;
	}
	public void setDecisionsAci(Set<DecisionAci> decisionsAci) {
		this.decisionsAci = decisionsAci;
	}

	/**
	 * Version simple du setter pour la méthode getBatch() du TiersDAO.
	 * @param set un ensemble
	 */
	public void setIdentificationsEntrepriseForGetBatch(@Nullable Set<IdentificationEntreprise> set) {
		this.identificationsEntreprise = set;
	}

	public void addIdentificationEntreprise(IdentificationEntreprise ident) {
		if (this.identificationsEntreprise == null) {
			this.identificationsEntreprise = new HashSet<>();
		}
		ident.setCtb(this);
		this.identificationsEntreprise.add(ident);
	}


	public void addDecisionAci(DecisionAci d) {
		if (this.decisionsAci == null) {
			this.decisionsAci = new HashSet<>();
		}
		d.setContribuable(this);
		this.decisionsAci.add(d);
	}

	@Transient
	public boolean hasDecisionAciValidAt(@Nullable RegDate date) {

		if (decisionsAci == null) {
			return false;
		}

		for (DecisionAci d : decisionsAci) {
			if (d.isValidAt(date)) {
				return true;
			}
		}

		return false;
	}

	@Transient
	public boolean hasDecisionEnCours(){
		if (decisionsAci == null) {
			return false;
		}

		for (DecisionAci d : decisionsAci) {
			final boolean ouverte =  d.getDateFin()==null || d.getDateFin().isAfterOrEqual(RegDate.get());
			if (ouverte && !d.isAnnule()) {
				return true;
			}
		}

		return false;
	}


	@Transient
	public boolean hasDecisionsNonAnnulees(){
		if (decisionsAci == null) {
			return false;
		}
		for (DecisionAci d : decisionsAci) {
			if (!d.isAnnule()) {
				return true;
			}
		}
		return false;
	}


	@Transient
	public RegDate getDateFinDerniereDecisionAci() {

		final List<DecisionAci> decisionsAci = getDecisionsSorted();
		if (decisionsAci != null && !decisionsAci.isEmpty()) {
			final List<RegDate> dates = DateRangeHelper.extractBoundaries(decisionsAci);
			return dates.get(dates.size()-1);
		}

		return null;
	}

	/**
	 * Permet de savoir si il existe  une decision qui a une date de fin qui se situe après une date de référence
	 * @param date la date de réference
	 * @return false s'il n'existe aucune décision ou si la dernière décision est férmée avant ou au plus tard à la date, true si la derniere décision
	 * est fermée après la date de passée en paramètre ou si elle est encore ouverte.
	 */
	@Transient
	public boolean existDecisionAciOuverteApres(RegDate date){
		if (hasDecisionsNonAnnulees()) {
			final RegDate dateFinDerniereDecisionAci = getDateFinDerniereDecisionAci();
			return RegDateHelper.isBefore(date, dateFinDerniereDecisionAci, NullDateBehavior.LATEST);
		}
		else{
			return false;
		}

	}

	/**
	 * Permet de savoir s'il existe une decision encore ouverte ou si une décision a une date de fin récente par rapport à une date
	 * @param date la date de réference
	 * @return <b>vrai</b> si il existe une décision récente <b>false</b> sinon
	 */
	public boolean hasDecisionRecenteFor(RegDate date){
		return hasDecisionEnCours() || existDecisionAciOuverteApres(date);
	}

	@Override
	public synchronized void addDeclaration(Declaration declaration) {
		if (declaration instanceof DeclarationAvecNumeroSequence) {
			final DeclarationAvecNumeroSequence avecNumero = (DeclarationAvecNumeroSequence) declaration;
			if (avecNumero.getNumero() == null) {
				// assignation d'un nouveau numéro de séquence par période fiscale
				final Set<Declaration> declarations = getOrCreateDeclarationSet();
				final int pf = declaration.getPeriode().getAnnee();
				int numero = 0;
				Integer maxFound = null;
				for (Declaration existante : declarations) {
					if (existante.getPeriode().getAnnee() == pf && existante instanceof DeclarationAvecNumeroSequence) {
						++ numero;

						final Integer numeroExistante = ((DeclarationAvecNumeroSequence) existante).getNumero();
						if (numeroExistante != null && (maxFound == null || numeroExistante > maxFound)) {
							maxFound = numeroExistante;
						}
					}
				}

				avecNumero.setNumero(maxFound == null
						                     ? numero + 1
						                     : Math.max(numero, maxFound) + 1);
			}
		}
		super.addDeclaration(declaration);
	}

	@OneToMany(mappedBy = "contribuable", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	public Set<RapprochementRF> getRapprochementsRF() {
		return rapprochementsRF;
	}

	public void setRapprochementsRF(Set<RapprochementRF> rapprochementsRF) {
		this.rapprochementsRF = rapprochementsRF;
	}

	public void addRapprochementRF(RapprochementRF rapprochement) {
		if (rapprochementsRF == null) {
			rapprochementsRF = new HashSet<>();
		}
		rapprochement.setContribuable(this);
		rapprochementsRF.add(rapprochement);
	}

	@NotNull
	@Transient
	public List<RapprochementRF> getRapprochementsRFNonAnnulesTries() {
		if (rapprochementsRF == null || rapprochementsRF.isEmpty()) {
			return Collections.emptyList();
		}
		return rapprochementsRF.stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}
}
