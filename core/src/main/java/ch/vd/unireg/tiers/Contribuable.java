package ch.vd.unireg.tiers;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationAvecNumeroSequence;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@Entity
public abstract class Contribuable extends Tiers {

	private Set<MouvementDossier> mouvementsDossier;
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

	/**
	 * @return la date jusqu'à laquelle les tâches d'envoi de déclarations d'impôts ne doivent pas être traitées par les <b>batches</b>; ou
	 *         <i>null</i> si les tâches peuvent être traitées sans restriction.
	 */
	@Column(name = "DATE_LIMITE_EXCLUSION", nullable = true)
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
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
			if (noOfs == null) {
				throw new IllegalArgumentException();
			}
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

	/**
	 * Trie les fors principaux par date, sans les annulés
	 *
	 * @return Renvoie les fors principaux
	 */
	@NotNull
	@Transient
	public List<? extends ForFiscalPrincipal> getForsFiscauxPrincipauxActifsSorted() {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.sorted(FOR_FISCAL_COMPARATOR)
				.collect(Collectors.toList());
	}

	/**
	 * Trie les fors secondaires par date, sans les annulés
	 *
	 * @return Renvoie les fors secondaires dans une map indexée par no ofs de la commune
	 */
	@NotNull
	@Transient
	public Map<Integer, List<ForFiscalSecondaire>> getForsFiscauxSecondairesActifsSortedMapped(MotifRattachement filtreMotifRattachement) {
		final Map<Integer, List<ForFiscalSecondaire>> map = getStreamForsFiscaux(ForFiscalSecondaire.class, false)
				.filter(ffs -> filtreMotifRattachement == null || ffs.getMotifRattachement() == filtreMotifRattachement)
				.collect(Collectors.toMap(ForFiscalSecondaire::getNumeroOfsAutoriteFiscale,
				                          Collections::singletonList,
				                          ListUtils::union));
		map.values().forEach(list -> list.sort(DateRangeComparator::compareRanges));
		return map;
	}

	/**
	 * Trie les fors secondaires par date, sans les annulés
	 *
	 * @return Renvoie les fors secondaires dans une map indexée par no ofs de la commune
	 */
	@Transient
	public Map<Integer, List<ForFiscalSecondaire>> getForsFiscauxSecondairesActifsSortedMapped() {
		return getForsFiscauxSecondairesActifsSortedMapped(null);
	}

	/**
	 * Retourne le for principal actif à une date donnée.
	 *
	 * @param date
	 *            la date à laquelle le for principal est actif, ou <b>null</b> pour obtenir le for courant.
	 *
	 * @return le for principal correspondant, ou nulle si aucun for ne correspond aux critères.
	 */
	@Transient
	public ForFiscalPrincipal getForFiscalPrincipalAt(@Nullable RegDate date) {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.filter(ff -> ff.isValidAt(date))
				.findFirst()
				.orElse(null);
	}

	@Transient
	public ForFiscalPrincipal getPremierForFiscalPrincipal() {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.min(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipal() {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalAvant(@Nullable RegDate date) {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.filter(ff -> RegDateHelper.isBeforeOrEqual(ff.getDateDebut(), date, NullDateBehavior.LATEST))
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalVaudois() {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForFiscalPrincipal getDernierForFiscalPrincipalVaudoisAvant(RegDate date) {
		return getStreamForsFiscaux(ForFiscalPrincipal.class, false)
				.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				.filter(ff -> RegDateHelper.isBeforeOrEqual(ff.getDateDebut(), date, NullDateBehavior.LATEST))
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	/**
	 * @return vrai s'il existe un for principal (ou une succession ininterrompue de fors principaux) durant la période spécifiée.
	 */
	public static boolean existForPrincipal(List<? extends ForFiscalPrincipal> principaux, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {

		int indexCandidat = -1;

		// Vérification de la date de début
		for (int i = 0; i < principaux.size(); ++i) {
			final ForFiscalPrincipal f = principaux.get(i);

			if (dateDebut != null && f.getDateFin() != null && f.getDateFin().isBefore(dateDebut)) {
				// on est pas encore arrivé à la date de début => on continue
				continue;
			}
			else if (f.getDateDebut() == null || (dateDebut != null && f.getDateDebut().isBeforeOrEqual(dateDebut))) {
				// on a trouvé un for qui contient la date de début => on saute à la vérification de la date de fin
				indexCandidat = i;
				break;
			}
			else if (dateDebut == null || (dateFin != null && f.getDateDebut().isAfter(dateFin))) {
				// on a dépassé la date de fin => rien trouvé
				return false;
			}
		}
		if (indexCandidat < 0) {
			// on a rien trouvé.
			return false;
		}

		// Vérification de la date de fin
		RegDate dateRaccord = null;
		for (int i = indexCandidat; i < principaux.size(); ++i) {
			final ForFiscalPrincipal f = principaux.get(i);

			if (dateRaccord != null && !dateRaccord.equals(f.getDateDebut())) {
				// il y a bien deux fors dans la plage spécifiée, mais ils ne se touchent pas => pas trouvé
				return false;
			}
			else if (f.getDateFin() == null || (dateFin != null && f.getDateFin().isAfterOrEqual(dateFin))) {
				// le for courant contient la date de fin => on a trouvé
				return true;
			}
			else {
				// le for ne s'étend pas sur toute la plage spécifiée => on continue avec le for suivant en spécifiant une date de raccord
				dateRaccord = f.getDateFin().getOneDayAfter();
			}
		}

		// on a pas trouvé de for s'étendant sur toute la plage demandée
		return false;
	}

	/**
	 * Renvoie la liste de fors fiscaux principaux débutant à ou après la date demandée (y compris les fors annulés).
	 * @param date date de référence
	 * @return liste des fors principaux demandés
	 */
	@NotNull
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxOuvertsApres(RegDate date) {
		return getForsFiscauxPrincipauxOuvertsApres(date,true);
	}

	/**
	 * Renvoie la liste de fors fiscaux principaux débutant à ou après la date demandée (y compris les fors annulés).
	 * @param date date de référence
	 * @param withAnnule indique si on veut les fors annulées
	 * @return liste des fors principaux demandés
	 */
	@NotNull
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxOuvertsApres(RegDate date, boolean withAnnule) {
		if (date == null) {
			throw new IllegalArgumentException();
		}
		return getStreamForsFiscaux(ForFiscalPrincipal.class, withAnnule)
				.filter(ff -> date.isBeforeOrEqual(ff.getDateDebut()))
				.sorted(FOR_FISCAL_COMPARATOR)
				.collect(Collectors.toList());
	}

	/**
	 * Renvoie la liste de fors fiscaux principaux débutant à ou avant la date demandée (y compris les fors annulés).
	 * @param date date de référence
	 * @param withAnnule indique si on veut les fors annulées
	 * @return liste des fors principaux demandés
	 */
	@NotNull
	@Transient
	public List<ForFiscalPrincipal> getForsFiscauxPrincipauxOuvertsAvant(RegDate date, boolean withAnnule) {
		if (date == null) {
			throw new IllegalArgumentException();
		}
		return getStreamForsFiscaux(ForFiscalPrincipal.class, withAnnule)
				.filter(ff -> date.isAfterOrEqual(ff.getDateDebut()))
				.sorted(FOR_FISCAL_COMPARATOR)
				.collect(Collectors.toList());
	}

	/**
	 * @param date date a laquelle on doit verifié que le tiers possède un for annulé.
	 * @param motif motif du for
	 *
	 * @return true si le tiers a un for fiscal principale annulé à la date précisée pour le motif précisé
	 */
	@Transient
	public boolean hasForFiscalPrincipalAnnule(RegDate date, @Nullable MotifFor motif) {
		if (date == null) {
			throw new IllegalArgumentException();
		}
		return getStreamForsFiscaux(ForFiscalPrincipal.class, true)
				.filter(ForFiscal::isAnnule)
				.filter(ff -> RegDateHelper.isBetween(date, ff.getDateDebut(), ff.getDateFin(), NullDateBehavior.EARLIEST))
				.anyMatch(ff -> motif == null || ff.getMotifOuverture() == motif);
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

	@NotNull
	@Transient
	public List<DecisionAci> getDecisionsSorted() {
		if (decisionsAci == null || decisionsAci.isEmpty()) {
			return Collections.emptyList();
		}
		return decisionsAci.stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(new DateRangeComparator<DecisionAci>().thenComparing(DecisionAci::getTypeAutoriteFiscale))
				.collect(Collectors.toList());
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
		if (decisionsAci == null || decisionsAci.isEmpty()) {
			return false;
		}
		return decisionsAci.stream().anyMatch(d -> d.isValidAt(date));
	}

	@Transient
	public boolean hasDecisionEnCours(){
		if (decisionsAci == null || decisionsAci.isEmpty()) {
			return false;
		}
		return decisionsAci.stream()
				.filter(AnnulableHelper::nonAnnule)
				.anyMatch(d -> RegDateHelper.isAfterOrEqual(d.getDateFin(), RegDate.get(), NullDateBehavior.LATEST));
	}


	@Transient
	public boolean hasDecisionsNonAnnulees() {
		if (decisionsAci == null || decisionsAci.isEmpty()) {
			return false;
		}
		return decisionsAci.stream().anyMatch(AnnulableHelper::nonAnnule);
	}


	@Transient
	public RegDate getDateFinDerniereDecisionAci() {
		final List<DecisionAci> decisionsAci = getDecisionsSorted();
		if (!decisionsAci.isEmpty()) {
			final Iterable<DecisionAci> reversed = CollectionsUtils.revertedOrder(decisionsAci);
			final DecisionAci last = reversed.iterator().next();
			return last.getDateFin();
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
		else {
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
	public synchronized void addDocumentFiscal(DocumentFiscal documentFiscal) {
		if (documentFiscal instanceof DeclarationAvecNumeroSequence) {
			DeclarationAvecNumeroSequence declaration = (DeclarationAvecNumeroSequence) documentFiscal;
			final DeclarationAvecNumeroSequence avecNumero = (DeclarationAvecNumeroSequence) declaration;
			if (avecNumero.getNumero() == null) {
				// assignation d'un nouveau numéro de séquence par période fiscale
				final Set<Declaration> declarations = getDeclarations();
				final int pf = declaration.getPeriode().getAnnee();
				int numero = 0;
				Integer maxFound = null;
				if (declarations != null) {
					for (Declaration existante : declarations) {
						if (existante.getPeriode().getAnnee() == pf && existante instanceof DeclarationAvecNumeroSequence) {
							++numero;

							final Integer numeroExistante = ((DeclarationAvecNumeroSequence) existante).getNumero();
							if (numeroExistante != null && (maxFound == null || numeroExistante > maxFound)) {
								maxFound = numeroExistante;
							}
						}
					}
				}

				avecNumero.setNumero(maxFound == null
						                     ? numero + 1
						                     : Math.max(numero, maxFound) + 1);
			}
		}
		super.addDocumentFiscal(documentFiscal);
	}

	@Override
	public synchronized void addDeclaration(Declaration declaration) {
		addDocumentFiscal(declaration);
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
