package ch.vd.uniregctb.webservices.tiers;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.webservices.tiers.impl.Context;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MenageCommunHisto", propOrder = {
		"contribuablePrincipal", "contribuableSecondaire"
})
public class MenageCommunHisto extends ContribuableHisto {

	/**
	 * information de base du contribuable principal composant le ménage (raccourci pour éviter une requête supplémentaire). La présence du
	 * contribuable principal ne garanti pas de l'activité du couple
	 */
	@XmlElement(required = false)
	public PersonnePhysiqueHisto contribuablePrincipal;

	/**
	 * information de base du contribuable secondaire composant le ménage (raccourci pour éviter une requête supplémentaire). Peut être null
	 * en cas de marié seul. La présence du contribuable secondaire ne garanti pas de l'activité du couple
	 */
	@XmlElement(required = false)
	public PersonnePhysiqueHisto contribuableSecondaire;

	public MenageCommunHisto() {
	}

	public MenageCommunHisto(ch.vd.uniregctb.tiers.MenageCommun menageCommun, Set<TiersPart> parts, Context context) throws Exception {
		super(menageCommun, parts, context);
		initParts(context, menageCommun, parts);
	}

	public MenageCommunHisto(ch.vd.uniregctb.tiers.MenageCommun menageCommun, int periode, Set<TiersPart> parts, Context context)
			throws Exception {
		super(menageCommun, periode, parts, context);
		initParts(context, menageCommun, parts);
	}

	public MenageCommunHisto(MenageCommunHisto menageCommun, Set<TiersPart> parts) {
		super(menageCommun, parts);
		copyParts(menageCommun, parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyPartsFrom(TiersHisto tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((MenageCommunHisto)tiers, parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TiersHisto clone(Set<TiersPart> parts) {
		return new MenageCommunHisto(this, parts);
	}

	private void copyParts(MenageCommunHisto menageCommun, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			contribuablePrincipal = menageCommun.contribuablePrincipal;
			contribuableSecondaire = menageCommun.contribuableSecondaire;
		}
	}

	private void initParts(Context context, ch.vd.uniregctb.tiers.MenageCommun menageCommun, Set<TiersPart> parts) throws Exception {
		if (parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			initComposants(menageCommun, context);
		}
	}

	private void initComposants(ch.vd.uniregctb.tiers.MenageCommun menageCommun, Context context) throws Exception {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			contribuablePrincipal = new PersonnePhysiqueHisto(principal, null, context);
		}

		final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			contribuableSecondaire = new PersonnePhysiqueHisto(conjoint, null, context);
		}
	}
}
