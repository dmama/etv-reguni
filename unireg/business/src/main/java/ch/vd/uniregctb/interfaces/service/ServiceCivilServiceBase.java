package ch.vd.uniregctb.interfaces.service;

import java.util.*;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Permis;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class ServiceCivilServiceBase implements ServiceCivilService {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceBase.class);

	private final List<CivilListener> listeners = new ArrayList<CivilListener>();

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public final AdressesCiviles getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {

		final int year = (date == null ? 2400 : date.year());
		final Collection<Adresse> adressesCiviles = getAdresses(noIndividu, year);

		AdressesCiviles resultat = new AdressesCiviles();

		try {
			if (adressesCiviles != null) {
				for (Adresse adresse : adressesCiviles) {
					if (adresse != null && adresse.isValidAt(date)) {
						resultat.set(adresse, strict);
					}
				}
			}
		}
		catch (DonneesCivilesException e) {
			throw new DonneesCivilesException(e.getMessage() + " sur l'individu n°" + noIndividu + " et pour l'année " + year + ".");
		}

		return resultat;
	}

	public final AdressesCivilesHisto getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {

		final int all = 2400;
		final Collection<Adresse> adressesCiviles = getAdresses(noIndividu, all);

		AdressesCivilesHisto resultat = new AdressesCivilesHisto();

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

	public final Collection<Adresse> getAdresses(long noIndividu, int annee) {
		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.ADRESSES);
		if (individu == null) {
			return null;
		}
		return individu.getAdresses();
	}

	public final Individu getIndividu(long noIndividu, int annee) {
		return getIndividu(noIndividu, annee, (EnumAttributeIndividu[])null); // -> va charger implicitement l'état-civil et l'historique
	}

	public final Individu getIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		final Long numeroIndividuConjoint = getNumeroIndividuConjoint(noIndividuPrincipal, date);
		 Individu individuConjoint=null;		
		if(numeroIndividuConjoint!=null){
			individuConjoint = getIndividu(numeroIndividuConjoint,date.year());
		}

		return individuConjoint;
	}

	/**
	 * @param noIndividu le numéro d'individu
	 * @param date
	 *            la date de référence, ou null pour obtenir l'état-civil actif
	 * @return l'état civil actif d'un individu à une date donnée.
	 */
	public final EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {

		final int year = (date == null ? 2400 : date.year());
		final Individu individu = getIndividu(noIndividu, year);

		return individu.getEtatCivil(date);
	}

	/**
	 * @param noIndividu le numéro d'individu
	 * @param date la date de validité du permis, ou <b>null</b> pour obtenir le dernis permis valide.
	 * @return le permis actif d'un individu à une date donnée.
	 */
	public final Permis getPermisActif(long noIndividu, RegDate date) {

		final int year = (date == null ? 2400 : date.year());
		final Individu individu = getIndividu(noIndividu, year, EnumAttributeIndividu.PERMIS);

		Permis permis = null;

		final Collection<Permis> coll = individu.getPermis();
		if (coll != null) {

			// tri des permis par leur date de début et numéro de séquence (utile si les dates de début sont nulles)
			final List<Permis> liste = new ArrayList<Permis>(coll);
			Collections.sort(liste, new Comparator<Permis>() {
				public int compare(Permis o1, Permis o2) {
					if (RegDateHelper.equals(o1.getDateDebutValidite(), o2.getDateDebutValidite())) {
						return o1.getNoSequence() - o2.getNoSequence();
					}
					else {
						return RegDateHelper.isBeforeOrEqual(o1.getDateDebutValidite(), o2.getDateDebutValidite(), NullDateBehavior.EARLIEST) ? -1 : 1;
					}
				}
			});

			// itération sur la liste des permis, dans l'ordre inverse de l'obtention
			// (on s'arrête sur le premier pour lequel les dates sont bonnes - et on ne prends pas en compte les permis annulés)
			for (int i = liste.size() - 1 ; i >= 0 ; --i) {
				final Permis e = liste.get(i);
				if (e.getDateAnnulation() == null
						&& RegDateHelper.isBetween(date, e.getDateDebutValidite(), e.getDateFinValidite(), NullDateBehavior.LATEST)) {
					permis = e;
					break;
				}
			}
		}

		return permis;
	}


	public void onIndividuChange(long numero) {
		for (CivilListener l : listeners) {
			try {
				l.onIndividuChange(numero);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void register(CivilListener listener) {
		listeners.add(listener);
	}


	/**
	 * Construit la liste des communes de domiciles connues pour la personne physique donnée, et ce depuis une date de référence
	 * @param date limite (incluse) dans le passé à la recherche des communes
	 * @param pp personne physique concernée
	 * @param seulementVaud <code>true</code> si on ne s'intéresse qu'aux communes vaudoises (i.e. commune <code>null</code> pour HC/HS)
	 * @return une liste des communes de domiciles fréquentées
	 */
	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate date, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, InfrastructureException {
		final AdressesCivilesHisto histo = getAdressesHisto(noIndividu, strict);
		final List<HistoriqueCommune> result = new ArrayList<HistoriqueCommune>();
		for (Adresse adresse : histo.principales) {
			if (RegDateHelper.isAfterOrEqual(adresse.getDateFin(), date, NullDateBehavior.LATEST)) {
				final CommuneSimple commune = infraService.getCommuneByAdresse(adresse);
				final CommuneSimple communeFiltree = (commune == null || !seulementVaud || commune.isVaudoise() ? commune : null);
				result.add(new HistoriqueCommune(adresse.getDateDebut(), adresse.getDateFin(), communeFiltree));
			}
		}
		if (result.size() == 0) {
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
}
