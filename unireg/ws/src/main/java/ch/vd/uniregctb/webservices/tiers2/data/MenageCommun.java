package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MenageCommun", propOrder = {
		"contribuablePrincipal", "contribuableSecondaire"
})
public class MenageCommun extends Contribuable {

	/**
	 * information de base du contribuable principal composant le ménage (raccourci pour éviter une requête supplémentaire). La présence du
	 * contribuable principal ne garanti pas de l'activité du couple
	 */
	@XmlElement(required = false)
	public PersonnePhysique contribuablePrincipal;

	/**
	 * information de base du contribuable secondaire composant le ménage (raccourci pour éviter une requête supplémentaire). Peut être null
	 * en cas de marié seul. La présence du contribuable secondaire ne garanti pas de l'activité du couple
	 */
	@XmlElement(required = false)
	public PersonnePhysique contribuableSecondaire;

	public MenageCommun() {
	}

	public MenageCommun(ch.vd.uniregctb.tiers.MenageCommun menageCommun, Set<TiersPart> setParts, ch.vd.registre.base.date.RegDate date,
			Context context) throws BusinessException {
		super(menageCommun, setParts, date, context);

		if (setParts != null && setParts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);

			final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				contribuablePrincipal = new PersonnePhysique(principal, null, date, context);
			}

			final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				contribuableSecondaire = new PersonnePhysique(conjoint, null, date, context);
			}
		}
	}

	public MenageCommun(MenageCommun menageCommun, Set<TiersPart> setParts) {
		super(menageCommun, setParts);
		copyParts(menageCommun, setParts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyPartsFrom(Tiers tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((MenageCommun)tiers, parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tiers clone(Set<TiersPart> parts) {
		return new MenageCommun(this, parts);
	}

	private void copyParts(MenageCommun menageCommun, Set<TiersPart> setParts) {
		if (setParts != null && setParts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			contribuablePrincipal = menageCommun.contribuablePrincipal;
			contribuableSecondaire = menageCommun.contribuableSecondaire;
		}
	}
}
