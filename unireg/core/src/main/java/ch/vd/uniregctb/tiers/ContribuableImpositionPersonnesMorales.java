package ch.vd.uniregctb.tiers;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

@Entity
public abstract class ContribuableImpositionPersonnesMorales extends Contribuable {

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

	@Transient
	@Override
	public List<ForFiscalPrincipalPM> getForsFiscauxPrincipauxActifsSorted() {
		return (List<ForFiscalPrincipalPM>) super.getForsFiscauxPrincipauxActifsSorted();
	}
}
