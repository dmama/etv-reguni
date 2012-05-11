package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.ServiceCivilServiceWrapper;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.Tutelle;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;

public class ServiceCivilImpl implements ServiceCivilService, ServiceCivilServiceWrapper {

	//private static final Logger LOGGER = Logger.getLogger(ServiceCivilImpl.class);

	private ServiceInfrastructureService infraService;
	private ServiceCivilRaw target;

	public ServiceCivilImpl() {
	}

	public ServiceCivilImpl(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public ServiceCivilImpl(ServiceInfrastructureService infraService, ServiceCivilRaw target) {
		this.infraService = infraService;
		this.target = target;
	}

	public void setTarget(ServiceCivilRaw target) {
		this.target = target;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public final AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {

		AdressesCivilesActives resultat = new AdressesCivilesActives();

		try {
			// [SIFISC-4250] on demande systématiquement la date 'null' pour pouvoir reconstruire les dates de fin des adresses qui n'en auraient pas
			final Individu individu = getIndividu(noIndividu, null, AttributeIndividu.ADRESSES);
			if (individu != null) {
				final Collection<Adresse> adressesCiviles = individu.getAdresses();
				if (adressesCiviles != null) {
					for (Adresse adresse : adressesCiviles) {
						if (adresse != null && adresse.isValidAt(date)) {
							resultat.set(adresse, strict);
						}
					}
				}
			}
		}
		catch (DonneesCivilesException e) {
			throw new DonneesCivilesException(e.getMessage() + " sur l'individu n°" + noIndividu + " et pour la date " + date + '.');
		}

		return resultat;
	}

	@Override
	public final AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {

		final Individu individu = getIndividu(noIndividu, null, AttributeIndividu.ADRESSES);
		if (individu == null) {
			return null;
		}

		final AdressesCivilesHistoriques resultat = new AdressesCivilesHistoriques();
		final Collection<Adresse> adressesCiviles = individu.getAdresses();
		if (adressesCiviles != null) {
			for (Adresse adresse : adressesCiviles) {
				if (adresse != null) {
					resultat.add(adresse);
				}
			}
		}
		resultat.finish(strict);

		return resultat;
	}

	@Override
	public final Collection<Nationalite> getNationalites(long noIndividu, RegDate date) {

		final Individu individu = getIndividu(noIndividu, date, AttributeIndividu.NATIONALITE);
		if (individu == null) {
			return null;
		}

		return individu.getNationalites();
	}

	@Override
	public final Collection<Origine> getOrigines(long noIndividu, RegDate date) {

		final Individu individu = getIndividu(noIndividu, date, AttributeIndividu.ORIGINE);
		if (individu == null) {
			return null;
		}

		return individu.getOrigines();
	}

	@Override
	public Collection<Permis> getPermis(long noIndividu, @Nullable RegDate date) {
		final Individu individu = getIndividu(noIndividu, date, AttributeIndividu.PERMIS);
		if (individu == null) {
			return null;
		}

		return individu.getPermis();
	}

	@Override
	public final Tutelle getTutelle(long noIndividu, RegDate date) {

		final Individu individu = getIndividu(noIndividu, date, AttributeIndividu.TUTELLE);
		if (individu == null) {
			return null;
		}

		return individu.getTutelle();
	}

	@Override
	public final Individu getConjoint(Long noIndividuPrincipal, @Nullable RegDate date) {

		final Long noConjoint = getNumeroIndividuConjoint(noIndividuPrincipal, date);
		if (noConjoint == null) {
			return null;
		}

		return getIndividu(noConjoint, date);
	}

	/**
	 * @param noIndividu le numéro d'individu
	 * @param date
	 *            la date de référence, ou null pour obtenir l'état-civil actif
	 * @return l'état civil actif d'un individu à une date donnée.
	 */
	@Override
	public final EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {

		final Individu individu = getIndividu(noIndividu, date);
		if (individu == null) {
			return null;
		}

		return individu.getEtatCivil(date);
	}

	@Override
	public final Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		final Individu individu = getIndividu(noIndividuPrincipal, date, AttributeIndividu.CONJOINTS);
		if (individu == null) {
			return null;
		}
		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		if (conjoints != null) {
			for (RelationVersIndividu conjoint : conjoints) {
				if (conjoint.isValidAt(date)) {
					return conjoint.getNumeroAutreIndividu();
				}
			}
		}
		return null;
	}

	@Override
	public final Set<Long> getNumerosIndividusConjoint(Long noIndividuPrincipal) {
		final Individu individu = getIndividu(noIndividuPrincipal, null, AttributeIndividu.CONJOINTS);
		if (individu == null) {
			return null;
		}
		final Set<Long> numeros = new HashSet<Long>();
		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		if (conjoints != null) {
			for (RelationVersIndividu conjoint : conjoints) {
				numeros.add(conjoint.getNumeroAutreIndividu());
			}
		}
		return numeros;
	}

	@Override
	public String getNomPrenom(Individu individu) {
		final String resultat;
		final NomPrenom nomPrenom = getDecompositionNomPrenom(individu);
		if (nomPrenom != null) {
			resultat = nomPrenom.getNomPrenom();
		}
		else {
			resultat = "";
		}
		return resultat;
	}

	/**
	 * Retourne les nom et prénoms de l'individu spécifié, dans deux champs distincts
	 * @param individu un individu
	 * @return une pair composée du (ou des) prénom(s) (premier élément) et du nom (deuxième élément) de l'individu
	 */
	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu) {

		if (individu == null) {
			return null;
		}

		return new NomPrenom(individu.getNom(), individu.getPrenom());
	}

	/**
	 * Construit la liste des communes de domiciles connues pour la personne physique donnée, et ce depuis une date de référence
	 * @param date limite (incluse) dans le passé à la recherche des communes
	 * @param noIndividu l'individu concerné
	 * @param seulementVaud <code>true</code> si on ne s'intéresse qu'aux communes vaudoises (i.e. commune <code>null</code> pour HC/HS)
	 * @return une liste des communes de domiciles fréquentées
	 */
	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate date, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		final AdressesCivilesHistoriques histo = getAdressesHisto(noIndividu, strict);
		if (histo == null) {
			throw new IndividuNotFoundException(noIndividu);
		}
		final List<HistoriqueCommune> result = new ArrayList<HistoriqueCommune>();
		for (Adresse adresse : histo.principales) {
			if (RegDateHelper.isAfterOrEqual(adresse.getDateFin(), date, NullDateBehavior.LATEST)) {
				final Commune commune = infraService.getCommuneByAdresse(adresse, date);
				final Commune communeFiltree = (commune == null || !seulementVaud || commune.isVaudoise() ? commune : null);
				result.add(new HistoriqueCommune(adresse.getDateDebut(), adresse.getDateFin(), communeFiltree));
			}
		}
		if (result.isEmpty()) {
			result.add(new HistoriqueCommune(date.getOneDayBefore(), null, null));
		}
		else if (!RegDateHelper.isBeforeOrEqual(result.get(0).getDateDebut(), date, NullDateBehavior.EARLIEST)) {
			result.add(0, new HistoriqueCommune(date.getOneDayBefore(), result.get(0).getDateDebut().getOneDayBefore(), null));
		}

		final HistoriqueCommune dernier = result.get(result.size() - 1);
		if (dernier.getDateFin() != null) {
			result.add(new HistoriqueCommune(dernier.getDateFin().getOneDayAfter(), null, null));
		}

		// maintenant on remplit les trous pour les périodes où aucune adresse n'est connue
		for (int i = result.size() - 1 ; i > 0 ; -- i) {
			final RegDate debut = result.get(i).getDateDebut();
			final RegDate finPrecedent = result.get(i - 1).getDateFin();
			if (debut != null && NullDateBehavior.LATEST.compare(debut.getOneDayBefore(), finPrecedent) > 0) {
				result.add(i, new HistoriqueCommune(finPrecedent.getOneDayAfter(), debut.getOneDayBefore(), null));
			}
		}

		return DateRangeHelper.collate(result);
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
		return target.getIndividu(noIndividu, date, parties);
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) {
		return target.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		return target.getIndividuFromEvent(eventId);
	}

	@Override
	public boolean isWarmable() {
		return target.isWarmable();
	}

	@Override
	public void setIndividuLogger(boolean value) {
		target.setIndividuLogger(value);
	}

	@Override
	public ServiceCivilRaw getTarget() {
		return target;
	}

	@Override
	public ServiceCivilRaw getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
