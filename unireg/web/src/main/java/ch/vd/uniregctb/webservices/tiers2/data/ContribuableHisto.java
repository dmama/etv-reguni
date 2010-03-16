package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContribuableHisto", propOrder = {
		"situationsFamille", "assujettissementsLIC", "assujettissementsLIFD", "periodesImposition"
})
public abstract class ContribuableHisto extends TiersHisto {

	private static final Logger LOGGER = Logger.getLogger(ContribuableHisto.class);

	@XmlElement(required = false)
	public List<SituationFamille> situationsFamille = null;

	/**
	 * La liste des assujettissements LIC existant durant la période demandée. Cette liste peut être vide si le tiers n'est pas assujetti.
	 * <p/>
	 * <b>Note:</b> les dates de début et de fin des assujettissements ne sont pas limitées à l'année fiscale courante. Voir la classe {@link Assujettissement} pour plus d'information.
	 */
	@XmlElement(required = false)
	public List<Assujettissement> assujettissementsLIC = null;

	/**
	 * La liste des assujettissements LIFD existant durant la période demandée. Cette liste peut être vide si le tiers n'est pas assujetti.
	 * <p/>
	 * <b>Note:</b> les dates de début et de fin des assujettissements ne sont pas limitées à l'année fiscale courante. Voir la classe {@link Assujettissement} pour plus d'information.
	 */
	@XmlElement(required = false)
	public List<Assujettissement> assujettissementsLIFD = null;

	/**
	 * La liste des périodes d'imposition à la date demandée. Cette liste peut être vide si le tiers n'est pas assujetti.
	 * <p/>
	 * <b>Note:</b> les dates de début et de fin sont garanties appartenir à une période fiscale. Voir la classe {@link PeriodeImposition} pour plus d'information.
	 */
	@XmlElement(required = false)
	public List<PeriodeImposition> periodesImposition = null;

	public ContribuableHisto() {
	}

	public ContribuableHisto(ch.vd.uniregctb.tiers.Contribuable contribuable, Set<TiersPart> parts, Context context)
			throws BusinessException {
		super(contribuable, parts, context);
		initParts(context, contribuable, parts, null);
	}

	public ContribuableHisto(ch.vd.uniregctb.tiers.Contribuable contribuable, int periode, Set<TiersPart> parts, Context context)
			throws BusinessException {
		super(contribuable, periode, parts, context);
		final Range range = new Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));
		initParts(context, contribuable, parts, range);
	}

	public ContribuableHisto(ContribuableHisto contribuable, Set<TiersPart> parts) {
		super(contribuable, parts);
		copyParts(contribuable, parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyPartsFrom(TiersHisto tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((ContribuableHisto) tiers, parts);
	}

	private final void copyParts(ContribuableHisto contribuable, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			this.situationsFamille = contribuable.situationsFamille;
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			this.assujettissementsLIC = contribuable.assujettissementsLIC;
			this.assujettissementsLIFD = contribuable.assujettissementsLIFD;
		}

		if (parts != null && parts.contains(TiersPart.PERIODE_IMPOSITION)) {
			this.periodesImposition = contribuable.periodesImposition;
		}
	}

	private void initParts(Context context, ch.vd.uniregctb.tiers.Contribuable contribuable, Set<TiersPart> parts, final Range range)
			throws BusinessException {
		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			initSituationsFamille(context, contribuable, range);
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			initAssujettissements(contribuable, range);
		}

		if (parts != null && parts.contains(TiersPart.PERIODE_IMPOSITION)) {
			initPeriodeImposition(context, contribuable, range);
		}
	}

	private void initAssujettissements(ch.vd.uniregctb.tiers.Contribuable contribuable, final Range range)
			throws BusinessException {

		this.assujettissementsLIC = new ArrayList<Assujettissement>();
		this.assujettissementsLIFD = new ArrayList<Assujettissement>();

		/*
		 * Note: il est nécessaire de calculer l'assujettissement sur TOUTE la période de validité du contribuable pour obtenir un résultat
		 * correct avec le collate.
		 */
		final List<ch.vd.uniregctb.metier.assujettissement.Assujettissement> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.Assujettissement.determine(contribuable, null, true /* collate */);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw new BusinessException(e);
		}
		if (list != null) {
			for (ch.vd.uniregctb.metier.assujettissement.Assujettissement a : list) {
				if (range == null || DateRangeHelper.intersect(a, range)) {
					this.assujettissementsLIC.add(Assujettissement.coreToLIC(a));
					this.assujettissementsLIFD.add(Assujettissement.coreToLIFD(a));
				}
			}
		}

		if (this.assujettissementsLIC.isEmpty()) {
			this.assujettissementsLIC = null;
		}
		if (this.assujettissementsLIFD.isEmpty()) {
			this.assujettissementsLIFD = null;
		}
	}

	private void initPeriodeImposition(Context context, ch.vd.uniregctb.tiers.Contribuable contribuable, Range range)
			throws BusinessException {

		// [UNIREG-913] On n'expose pas les périodes fiscales avant la première période définie dans les paramètres
		final int premierePeriodeFiscale = context.parametreService.getPremierePeriodeFiscale();
		if (range != null && premierePeriodeFiscale > range.getDateDebut().year()) {
			range = new Range(RegDate.get(premierePeriodeFiscale, 1, 1), range.getDateFin());
		}

		this.periodesImposition = new ArrayList<PeriodeImposition>();

		final List<ch.vd.uniregctb.metier.assujettissement.PeriodeImposition> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.PeriodeImposition.determine(contribuable, range);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw new BusinessException(e);
		}
		if (list != null) {
			PeriodeImposition derniere = null;
			for (ch.vd.uniregctb.metier.assujettissement.PeriodeImposition p : list) {
				final PeriodeImposition periode = new PeriodeImposition(p, DataHelper.getAssociatedDi(p));
				this.periodesImposition.add(periode);
				derniere = periode;
			}
			// [UNIREG-910] la période d'imposition courante est laissée ouverte
			if (derniere != null && derniere.dateFin != null) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateFin = RegDate.get(derniere.dateFin.asJavaDate());
				if (dateFin.isAfter(aujourdhui)) {
					derniere.dateFin = null;
				}
			}
		}

		if (this.periodesImposition.isEmpty()) {
			this.periodesImposition = null;
		}
	}

	private void initSituationsFamille(Context context, ch.vd.uniregctb.tiers.Contribuable contribuable, final Range range) {

		this.situationsFamille = new ArrayList<SituationFamille>();
		final List<ch.vd.uniregctb.situationfamille.VueSituationFamille> situations = context.situationService.getVueHisto(contribuable);

		for (ch.vd.uniregctb.situationfamille.VueSituationFamille situation : situations) {
			if (range != null && !DateRangeHelper.intersect(situation, range)) {
				continue;
			}
			this.situationsFamille.add(new SituationFamille(situation));
		}
		if (this.situationsFamille.isEmpty()) {
			this.situationsFamille = null;
		}
	}
}
