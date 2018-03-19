package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.foncier.AllegementFoncier;

@Entity
public abstract class ContribuableImpositionPersonnesMorales extends Contribuable {

	private Set<AllegementFoncier> allegementsFonciers;

	public ContribuableImpositionPersonnesMorales() {
	}

	public ContribuableImpositionPersonnesMorales(long numero) {
		super(numero);
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Contribuable PM";
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "CTB_ID", nullable = false)
	@ForeignKey(name = "FK_AFONC_CTB_ID")
	public Set<AllegementFoncier> getAllegementsFonciers() {
		return allegementsFonciers;
	}

	public void setAllegementsFonciers(Set<AllegementFoncier> allegementsFonciers) {
		this.allegementsFonciers = allegementsFonciers;
	}

	public void addAllegementFoncier(AllegementFoncier af) {
		if (allegementsFonciers == null) {
			allegementsFonciers = new HashSet<>();
		}
		af.setContribuable(this);
		allegementsFonciers.add(af);
	}

	@NotNull
	@Transient
	public <T extends AllegementFoncier> List<T> getAllegementsFonciersNonAnnulesTries(Class<T> clazz) {
		if (allegementsFonciers == null || allegementsFonciers.isEmpty()) {
			return Collections.emptyList();
		}
		return allegementsFonciers.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	@Override
	public synchronized void addDeclaration(Declaration declaration) {
		if (declaration instanceof DeclarationImpotOrdinairePM) {
			final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
			if (shouldAssignCodeControle(di)) {
				di.setCodeControle(generateNewCodeControle(getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true)));
			}
		}
		super.addDeclaration(declaration);
	}

	/**
	 * @param di nouvelle déclaration en cours d'ajout
	 * @return <code>true</code> si un code de contrôle doit être généré pour la DI
	 */
	public boolean shouldAssignCodeControle(DeclarationImpotOrdinairePM di) {
		final int pf = di.getPeriode().getAnnee();
		return pf >= DeclarationImpotOrdinairePM.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE && di.getCodeControle() == null;
	}

	/**
	 * @param declarationsExistantes collection des déclarations existantes sur la PM
	 * @return un nouveau code de contrôle pour la déclaration (différent de tous les autres codes de contrôles des DI existantes, annulées ou pas)
	 */
	public static String generateNewCodeControle(Collection<DeclarationImpotOrdinairePM> declarationsExistantes) {
		// faisons le tour de tous les codes de contrôles existant afin de ne pas reprendre une deuxième fois le même
		final Set<String> codesExistants = new HashSet<>(declarationsExistantes.size());
		for (DeclarationImpotOrdinairePM di : declarationsExistantes) {
			if (di.getCodeControle() != null) {
				codesExistants.add(di.getCodeControle());
			}
		}

		// on boucle la génération tant qu'on n'a pas quelque chose de neuf...
		String codeControle;
		do {
			codeControle = DeclarationImpotOrdinairePM.generateCodeControle();
		}
		while (codesExistants.contains(codeControle));
		return codeControle;
	}

	@Override
	public void addForFiscal(ForFiscal nouveauForFiscal) {

		// les seuls fors fiscaux principaux autorisés sont de la classe "ForFiscalPrincipalPM"
		if (nouveauForFiscal.isPrincipal() && !ForFiscalPrincipalPM.class.isAssignableFrom(nouveauForFiscal.getClass())) {
			throw new IllegalArgumentException("Le for fiscal principal " + nouveauForFiscal + " n'est pas autorisé pour les contribuables dits 'PM'");
		}

		super.addForFiscal(nouveauForFiscal);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getPremierForFiscalPrincipal() {
		return (ForFiscalPrincipalPM) super.getPremierForFiscalPrincipal();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getDernierForFiscalPrincipal() {
		return (ForFiscalPrincipalPM) super.getDernierForFiscalPrincipal();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getDernierForFiscalPrincipalAvant(@Nullable RegDate date) {
		return (ForFiscalPrincipalPM) super.getDernierForFiscalPrincipalAvant(date);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getDernierForFiscalPrincipalVaudois() {
		return (ForFiscalPrincipalPM) super.getDernierForFiscalPrincipalVaudois();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getDernierForFiscalPrincipalVaudoisAvant(RegDate date) {
		return (ForFiscalPrincipalPM) super.getDernierForFiscalPrincipalVaudoisAvant(date);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPM getForFiscalPrincipalAt(@Nullable RegDate date) {
		return (ForFiscalPrincipalPM) super.getForFiscalPrincipalAt(date);
	}

	@NotNull
	@Transient
	@Override
	public List<ForFiscalPrincipalPM> getForsFiscauxPrincipauxActifsSorted() {
		return (List<ForFiscalPrincipalPM>) super.getForsFiscauxPrincipauxActifsSorted();
	}
}