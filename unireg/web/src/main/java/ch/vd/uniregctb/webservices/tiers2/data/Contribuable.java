package ch.vd.uniregctb.webservices.tiers2.data;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Set;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Contribuable", propOrder = {
		"situationFamille", "assujettissementLIC", "assujettissementLIFD", "periodeImposition"
})
public abstract class Contribuable extends Tiers {

	private static final Logger LOGGER = Logger.getLogger(Contribuable.class);

	@XmlElement(required = false)
	public SituationFamille situationFamille;

	/**
	 * L'assujettissement LIC à la date demandée, ou <b>null</b> si le tiers n'est pas assujetti.
	 * <p>
	 * <b>Note:</b> les dates de début et de fin ne sont pas limitée à l'année fiscale courante. Voir la classe {@link Assujettissement}
	 * pour plus d'information.
	 */
	@XmlElement(required = false)
	public Assujettissement assujettissementLIC = null;

	/**
	 * L'assujettissement LIFD à la date demandée, ou <b>null</b> si le tiers n'est pas assujetti.
	 * <p>
	 * <b>Note:</b> les dates de début et de fin ne sont pas limitée à l'année fiscale courante. Voir la classe {@link Assujettissement}
	 * pour plus d'information.
	 */
	@XmlElement(required = false)
	public Assujettissement assujettissementLIFD = null;

	/**
	 * La période d'imposition à la date demandée, ou <b>null</b> si le tiers n'est pas assujetti.
	 * <p>
	 * <b>Note:</b> les dates de début et de fin sont garanties appartenir à la période fiscale courante. Voir la classe
	 * {@link PeriodeImposition} pour plus d'information.
	 */
	@XmlElement(required = false)
	public PeriodeImposition periodeImposition = null;

	public Contribuable() {
	}

	public Contribuable(ch.vd.uniregctb.tiers.Contribuable contribuable, Set<TiersPart> parts, ch.vd.registre.base.date.RegDate date,
			Context context) throws BusinessException {
		super(contribuable, parts, date, context);

		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			ch.vd.uniregctb.situationfamille.VueSituationFamille situation = context.situationService.getVue(contribuable, date);
			if (situation != null) {
				this.situationFamille = new SituationFamille(situation);
			}
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			/*
			 * Note: on doit calculer le range complet (range=null) des assujettissement pour être en mesure de retourner les dates de début
			 * et de fin correctes de l'assujettissement. Autrement, on va limiter obligatoirement le range à l'année fiscale courante, et
			 * il n'y a plus de différence avec la période d'imposition.
			 */
			final List<ch.vd.uniregctb.metier.assujettissement.Assujettissement> list;
			try {
				list = ch.vd.uniregctb.metier.assujettissement.Assujettissement.determine(contribuable, null, true);
			}
			catch (AssujettissementException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}
			if (list != null) {
				for (ch.vd.uniregctb.metier.assujettissement.Assujettissement a : list) {
					if (a.isValidAt(date)) {
						this.assujettissementLIC = Assujettissement.coreToLIC(a);
						this.assujettissementLIFD = Assujettissement.coreToLIFD(a);
					}
				}
			}
		}

		if (parts != null && parts.contains(TiersPart.PERIODE_IMPOSITION)) {
			// [UNIREG-913] On n'expose pas les périodes fiscales avant la première période définie dans les paramètres
			final int premierePeriodeFiscale = context.parametreService.getPremierePeriodeFiscale();
			final int year = date != null ? date.year() : RegDate.get().year();
			if (year >= premierePeriodeFiscale) {
				final List<ch.vd.uniregctb.metier.assujettissement.PeriodeImposition> list;
				try {
					list = ch.vd.uniregctb.metier.assujettissement.PeriodeImposition.determine(contribuable, year);
				}
				catch (AssujettissementException e) {
					LOGGER.error(e, e);
					throw new BusinessException(e);
				}
				if (list != null) {
					for (ch.vd.uniregctb.metier.assujettissement.PeriodeImposition p : list) {
						if (p.isValidAt(date)) {
							this.periodeImposition = new PeriodeImposition(p, DataHelper.getAssociatedDi(p));
							// [UNIREG-910] la période d'imposition courante est laissée ouverte
							if (this.periodeImposition.dateFin != null) {
								final RegDate aujourdhui = RegDate.get();
								final RegDate dateFin = RegDate.get(this.periodeImposition.dateFin.asJavaDate());
								if (dateFin.isAfter(aujourdhui)) {
									this.periodeImposition.dateFin = null;
								}
							}
							break;
						}
					}
				}
			}
		}
	}

	public Contribuable(Contribuable contribuable, Set<TiersPart> parts) {
		super(contribuable, parts);
		copyParts(contribuable, parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyPartsFrom(Tiers tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((Contribuable) tiers, parts);
	}

	private void copyParts(Contribuable contribuable, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			this.situationFamille = contribuable.situationFamille;
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			this.assujettissementLIC = contribuable.assujettissementLIC;
			this.assujettissementLIFD = contribuable.assujettissementLIFD;
		}

		if (parts != null && parts.contains(TiersPart.PERIODE_IMPOSITION)) {
			this.periodeImposition = contribuable.periodeImposition;
		}
	}
}
