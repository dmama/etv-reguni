package ch.vd.uniregctb.evenement.degrevement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.degrevement.v1.DonneesMetier;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.event.degrevement.v1.TypDateAttr;
import ch.vd.unireg.xml.event.degrevement.v1.TypEntMax12Attr;
import ch.vd.unireg.xml.event.degrevement.v1.TypPctPosDecMax32Attr;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Handler métier des retours des données de dégrèvement
 */
public class EvenementDegrevementHandlerV1Impl implements EvenementDegrevementHandlerV1 {

	private static final BigInteger BI_MAXINT = BigInteger.valueOf(Integer.MAX_VALUE);
	private static final BigDecimal BD_HUNDRED = BigDecimal.valueOf(100L);

	private TiersService tiersService;
	private HibernateTemplate hibernateTemplate;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onRetourDegrevement(Message retour, Map<String, String> headers) throws EsbBusinessException {
		// que faut-il faire ?
		// 1. identifier la demande de dégrèvement correspondante
		// 2. quittancer le formulaire (= quittancement implicite)
		// 3. récupérer les données de dégrèvement et générer une nouvelle entrée en base
		// 4. TODO envoyer quelque chose au DPerm (tâche) ???

		final DemandeDegrevementICI formulaire = findFormulaireDegrevement(retour);
		quittancerFormulaire(formulaire, XmlUtils.xmlcal2regdate(retour.getSupervision().getHorodatageReception()));

		final Entreprise entreprise = formulaire.getEntreprise();
		final ImmeubleRF immeuble = formulaire.getImmeuble();
		final DateRange destinationRange = new DateRangeHelper.Range(RegDate.get(formulaire.getPeriodeFiscale(), 1, 1), null);

		// TODO faut-il faire un contrôle spécifique si aucune valeur déclarée ne change ?

		final DegrevementICI degrevement = new DegrevementICI();
		degrevement.setDateDebut(destinationRange.getDateDebut());
		degrevement.setDateFin(destinationRange.getDateFin());
		degrevement.setImmeuble(immeuble);
		degrevement.setContribuable(entreprise);

		// Attention, les trois appels extract... plus bas peuvent encore lancer des EsbBusinessException...
		// il ne faut donc pas commencer la manipulation d'entité persistantes avant que les trois
		// appels soient revenus sans heurt

		final DonneesMetier donneesMetier = retour.getDonneesMetier();
		degrevement.setLocation(extractDonneesLocation(donneesMetier));
		degrevement.setPropreUsage(extractDonneesPropreUsage(donneesMetier));
		degrevement.setLoiLogement(extractDonneesLoiLogement(donneesMetier));

		// maintenant, on peut commencer à modifier les entités persistantes

		// TODO hypothèse : tous les dégrèvements ultérieurs du début de la période fiscale de la demande sont tronqués ou annulés
		final RegDate dateFinMax = destinationRange.getDateDebut().getOneDayBefore();
		entreprise.getAllegementsFonciersNonAnnulesTries(DegrevementICI.class).stream()
				.filter(deg -> deg.getImmeuble() == immeuble)
				.filter(deg -> DateRangeHelper.intersect(deg, destinationRange))
				.forEach(deg -> {
					// si complètement dedans -> annulé
					if (DateRangeHelper.within(deg, destinationRange)) {
						deg.setAnnule(true);
					}
					else {
						// il intersecte juste... comme le range de destination est ouvert à droite,
						// une intersection non-complète signifie que c'est la date de début qui dépasse
						// il y a donc deux cas :
						// - sans date de fin existante -> on ferme
						// - avec date de fin existante -> on annule, recopie et on ferme la copie à la veille de la nouvelle date de début
						if (deg.getDateFin() == null) {
							deg.setDateFin(dateFinMax);
						}
						else {
							final DegrevementICI copie = deg.duplicate();
							deg.setAnnule(true);
							copie.setDateFin(dateFinMax);
							entreprise.addAllegementFoncier(hibernateTemplate.merge(copie));
						}
					}
				});

		entreprise.addAllegementFoncier(hibernateTemplate.merge(degrevement));
	}

	@Nullable
	private static DonneesUtilisation extractDonneesLocation(DonneesMetier data) throws EsbBusinessException {
		final Integer volume = extractInteger("volume locatif", data.getVolumeLocatif());
		final Integer surface = extractInteger("surface locative", data.getSurfaceLocatif());
		final Integer revenu = extractInteger("revenu locatif perçu", data.getRevenuLocatifEncaisse());
		final BigDecimal percent = extractPourcentage("pourcentage locatif", data.getPourcentageLocatif());
		if (volume != null || surface != null || revenu != null || percent != null) {
			return new DonneesUtilisation(revenu, volume, surface, percent, null);
		}
		return null;
	}

	@Nullable
	private static DonneesUtilisation extractDonneesPropreUsage(DonneesMetier data) throws EsbBusinessException {
		final Integer volume = extractInteger("volume propre usage", data.getVolumePropreUsage());
		final Integer surface = extractInteger("surface propre usage", data.getSurfacePropreUsage());
		final Integer revenu = extractInteger("revenu estimé", data.getRevenuLocatifEstime());
		final BigDecimal percent = extractPourcentage("pourcentage propre usage", data.getPourcentagePropreUsage());
		if (volume != null || surface != null || revenu != null || percent != null) {
			return new DonneesUtilisation(revenu, volume, surface, percent, null);
		}
		return null;
	}

	@Nullable
	private static DonneesLoiLogement extractDonneesLoiLogement(DonneesMetier data) throws EsbBusinessException {
		if (data.isControleOfficeLogement()) {
			final RegDate dateOctroi = extractDate(data.getDateOctroi());
			final RegDate dateEcheanceOctroi = extractDate(data.getDateEcheanceOctroi());
			if (dateOctroi != null || dateEcheanceOctroi != null) {
				return new DonneesLoiLogement(dateOctroi, dateEcheanceOctroi, null);
			}
		}
		return null;
	}

	@Nullable
	private static Integer extractInteger(String description, TypEntMax12Attr value) throws EsbBusinessException {
		if (value != null && value.isValide()) {
			final BigInteger numericalValue = value.getValue();
			if (numericalValue.compareTo(BigInteger.ZERO) < 0 || numericalValue.compareTo(BI_MAXINT) > 0) {
				// valeur clairement hors domaine de validité...
				throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "L'attribut '" + description + "' est hors de son domaine de validité [0 - " + Integer.MAX_VALUE + "]", null);
			}
			return numericalValue.intValue();
		}
		return null;
	}

	@Nullable
	private static BigDecimal extractPourcentage(String description, TypPctPosDecMax32Attr value) throws EsbBusinessException {
		if (value != null && value.isValide()) {
			final BigDecimal percent = value.getValue();
			if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BD_HUNDRED) > 0) {
				// valeur hors du domaine de validité...
				throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "L'attribut '" + description + "' est hors de son domaine de validité [0.00 - 100.00]", null);
			}
			return percent;
		}
		return null;
	}

	@Nullable
	private static RegDate extractDate(TypDateAttr value) throws EsbBusinessException {
		if (value != null && value.isValide()) {
			return XmlUtils.xmlcal2regdate(value.getValue());
		}
		return null;
	}

	/**
	 * Classe interne qui contient les informations nécessaire pour retrouver un formulaire de demande de dégrèvement ICI
	 */
	private static final class IdFormulaire {
		final int noContribuable;
		final int numeroSequence;
		final int pf;

		public IdFormulaire(int noContribuable, int numeroSequence, int pf) {
			this.noContribuable = noContribuable;
			this.numeroSequence = numeroSequence;
			this.pf = pf;
		}
	}

	/**
	 * Retrouve le formulaire de dégrèvement ICI correspondant aux données qui reviennent
	 * @param retour message entrant
	 * @return le formulaire de demande
	 * @throws EsbBusinessException si le formulaire est introuvable
	 */
	@NotNull
	private DemandeDegrevementICI findFormulaireDegrevement(Message retour) throws EsbBusinessException {

		// récupération des données de récupération de la demande de dégrèvement ICI
		final IdFormulaire id = extractIdFormulaire(retour.getDonneesMetier());

		final Tiers tiers = tiersService.getTiers(id.noContribuable);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Entreprise inconnue avec ce numéro de contribuable.", null);
		}

		final Entreprise entreprise = (Entreprise) tiers;
		final Optional<DemandeDegrevementICI> formulaire = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
				.filter(dd -> dd.getPeriodeFiscale() == id.pf)
				.filter(dd -> dd.getNumeroSequence() == id.numeroSequence)
				.findFirst();
		if (formulaire.isPresent()) {
			return formulaire.get();
		}

		throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Formulaire de demande de dégrèvement introuvable pour la PF " + id.pf + " et le numéro de séquence " + id.numeroSequence, null);
	}

	/**
	 * Quittance le formulaire de demande de dégrèvement s'il n'est pas déjà quittancé
	 * @param formulaire formulaire
	 * @param dateQuittance nouvelle date de quittance
	 */
	private void quittancerFormulaire(DemandeDegrevementICI formulaire, RegDate dateQuittance) {
		if (formulaire.getDateRetour() == null) {
			formulaire.setDateRetour(dateQuittance);
		}
	}

	@NotNull
	private IdFormulaire extractIdFormulaire(DonneesMetier data) throws EsbBusinessException {
		if (data == null) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Données métier absentes.", null);
		}

		final BigInteger noSeqBrut = data.getNumeroSequenceDemande();
		if (noSeqBrut == null) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Numéro de séquence du formulaire originel absent.", null);
		}
		else if (noSeqBrut.compareTo(BigInteger.ZERO) <= 0 || noSeqBrut.compareTo(BI_MAXINT) > 0) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Numéro de séquence invalide (" + noSeqBrut + ").", null);
		}

		return new IdFormulaire(data.getNumeroContribuable(), noSeqBrut.intValue(), data.getPeriodeFiscale());
	}
}
