package ch.vd.unireg.rt.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.RapportsPrestationView;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class RapportPrestationEditManagerImpl implements RapportPrestationEditManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapportPrestationEditManagerImpl.class);

	private HibernateTemplate hibernateTemplate;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceCivilService serviceCivilService;
	private TiersGeneralManager tiersGeneralManager;
	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private MessageHelper messageHelper;
	private SecurityProviderInterface securityProvider;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RapportPrestationView get(Long numeroSrc, Long numeroDpi, String provenance) {
		RapportPrestationView rapportView = new RapportPrestationView();

		rapportView.setProvenance(provenance);

		final Tiers tiers = tiersService.getTiers(numeroSrc);

		if (tiers == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.sourcier.inexistant"));
		}

		if (!(tiers instanceof PersonnePhysique)) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.personne.physique.attendu",FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero())));
		}
		final PersonnePhysique sourcier = (PersonnePhysique) tiers;

		TiersGeneralView sourcierView = tiersGeneralManager.getPersonnePhysique(sourcier, true);
		rapportView.setSourcier(sourcierView);

		final Tiers dpi = tiersService.getTiers(numeroDpi);

		if (dpi == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.debiteur.inexistant"));
		}

		if (!(dpi instanceof DebiteurPrestationImposable)) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.debiteur.prestation.impot.source.attendu", FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero())));
		}

		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur((DebiteurPrestationImposable) dpi, true);
		rapportView.setDebiteur(dpiView);

		return rapportView;
	}

	/**
	 * Alimente la vue RapportView
	 */
	public RapportPrestationView get(Long idRapport, SensRapportEntreTiers sensRapportEntreTiers) throws AdresseException {
		RapportPrestationView rapportView = new RapportPrestationView();

		RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.rapport.inexistant"));
		}

		rapportView.setSensRapportEntreTiers(sensRapportEntreTiers);
		rapportView.setTypeRapportEntreTiers(rapportEntreTiers.getType());
		Long numero = null;
		if (sensRapportEntreTiers == SensRapportEntreTiers.OBJET) {
			numero = rapportEntreTiers.getSujetId();
		}
		if (sensRapportEntreTiers == SensRapportEntreTiers.SUJET) {
			numero = rapportEntreTiers.getObjetId();
		}
		setNomCourrier(rapportView, numero);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}

		return rapportView;
	}

	/**
	 * Mise à jour de numero, nomCourrier1 et nomCourrier2 en fonction du numero
	 */
	private void setNomCourrier(RapportPrestationView rapportView, Long numero) throws AdresseException {
		rapportView.setNumero(numero);

		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		List<String> nomCourrier = adresseService.getNomCourrier(tiers, null, false);
		rapportView.setNomCourrier(nomCourrier);

	}


	/**
	 * Persiste le rapport de travail
	 */
	@Override
	public void save(RapportPrestationView rapportView) {
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(rapportView.getSourcier().getNumero());
		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(rapportView.getDebiteur().getNumero());

		final List<RapportPrestationImposable> existants = tiersService.getAllRapportPrestationImposable(debiteur, sourcier, true, true);
		if (existants != null && !existants.isEmpty()) {
			final DateRange newRange = new DateRangeHelper.Range(rapportView.getDateDebut(), null);
			if (DateRangeHelper.intersect(newRange, existants)) {
				throw new ActionException("Un rapport de travail existe déjà entre ces mêmes débiteur et sourcier sur une période d'au moins un jour après le " + RegDateHelper.dateToDisplayString(newRange.getDateDebut()));
			}
		}

		tiersService.addRapportPrestationImposable(sourcier, debiteur, rapportView.getDateDebut(), null);
	}

	@Override
	public boolean isExistingTiers(long tiersId) {
		final Tiers tiers = tiersService.getTiers(tiersId);
		return tiers != null;
	}

	@Override
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto) {
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur, !rapportsPrestationHisto);
	}

	@Override
	public void fillRapportsPrestationView(long noDebiteur, RapportsPrestationView view) {
		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(noDebiteur);
		if (debiteur == null) {
			throw new TiersNotFoundException(noDebiteur);
		}

		final List<RapportsPrestationView.Rapport> rapports = new ArrayList<>();
		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumero = new HashMap<>();

		final long startRapports = System.nanoTime();

		final Set<RapportEntreTiers> list = debiteur.getRapportsObjet();

		// Rempli les informations de base

		for (RapportEntreTiers r : list) {
			if (r.getType() != TypeRapportEntreTiers.PRESTATION_IMPOSABLE) {
				continue;
			}

			final RapportPrestationImposable rpi = (RapportPrestationImposable) r;

			final RapportsPrestationView.Rapport rapport = new RapportsPrestationView.Rapport();
			rapport.id = r.getId();
			rapport.annule = r.isAnnule();
			rapport.noCTB = r.getSujetId();
			rapport.dateDebut = r.getDateDebut();
			rapport.dateFin = r.getDateFin();
			rapport.noCTB = rpi.getSujetId();
			rapports.add(rapport);

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumero.get(rapport.noCTB);
			if (rl == null) {
				rl = new ArrayList<>();
				rapportsByNumero.put(rapport.noCTB, rl);
			}

			rl.add(rapport);
		}

		final long endRapports = System.nanoTime();
		LOGGER.debug("- chargement des rapports en " + ((endRapports - startRapports) / 1000000) + " ms");

		// Complète les noms, prénoms et nouveaux numéros AVS des non-habitants

		final long startNH = System.nanoTime();

		final Set<Long> pasDeNouveauNosAvs = new HashSet<>();

		// TODO (msi) déplacer ce HQL dans un DAO
		final List infoNonHabitants = hibernateTemplate.find("select pp.numero, pp.prenomUsuel, pp.nom, pp.numeroAssureSocial from PersonnePhysique pp, RapportPrestationImposable rpi "
				                                                     + "where pp.habitant = false and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur, null);
		for (Object o : infoNonHabitants) {
			final Object line[] = (Object[]) o;
			final Long numero = (Long) line[0];
			final String prenom = (String) line[1];
			final String nom = (String) line[2];
			final String noAVS = (String) line[3];

			if (StringUtils.isBlank(noAVS)) {
				pasDeNouveauNosAvs.add(numero);
			}

			final List<RapportsPrestationView.Rapport> rl = rapportsByNumero.get(numero);
			if (rl == null) {
				throw new IllegalArgumentException();
			}

			for (RapportsPrestationView.Rapport r : rl) {
				r.nomCourrier = Collections.singletonList(getNomPrenom(prenom, nom));
				r.noAVS = FormatNumeroHelper.formatNumAVS(noAVS);
			}
		}

		// Complète les anciens numéros AVS des non-habitants qui n'en possède pas des nouveaux

		if (!pasDeNouveauNosAvs.isEmpty()) {
			final StandardBatchIterator<Long> it = new StandardBatchIterator<>(pasDeNouveauNosAvs, 500);
			while (it.hasNext()) {
				final List<Long> ids = it.next();

				// TODO (msi) déplacer ce HQL dans un DAO
				final List<Object[]> ancienNosAvs = hibernateTemplate.execute(session -> {
					final Query query = session.createQuery(
							"select ip.personnePhysique.id, ip.identifiant from IdentificationPersonne ip where ip.categorieIdentifiant = 'CH_AHV_AVS' and ip.personnePhysique.id in (:ids)");
					query.setParameterList("ids", ids);
					//noinspection unchecked
					return (List<Object[]>) query.list();
				});

				for (Object[] line : ancienNosAvs) {
					final Long numero = (Long) line[0];
					final String noAVS = (String) line[1];

					final List<RapportsPrestationView.Rapport> rl = rapportsByNumero.get(numero);
					if (rl == null) {
						throw new IllegalArgumentException();
					}

					for (RapportsPrestationView.Rapport r : rl) {
						r.noAVS = FormatNumeroHelper.formatAncienNumAVS(noAVS);
					}
				}
			}
		}

		final long endNH = System.nanoTime();
		LOGGER.debug("- chargement des non-habitants en " + ((endNH - startNH) / 1000000) + " ms");

		// Complète les noms, prénoms et numéros AVS des habitants

		final long startH = System.nanoTime();

		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumeroIndividu = new HashMap<>();

		// TODO (msi) déplacer ce HQL dans un DAO
		final List infoHabitants = hibernateTemplate.find("select pp.numero, pp.numeroIndividu from PersonnePhysique pp, RapportPrestationImposable rpi "
				                                                  + "where pp.habitant = true and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur, null);
		for (Object o : infoHabitants) {
			final Object line[] = (Object[]) o;
			final Long numero = (Long) line[0];
			final Long numeroIndividu = (Long) line[1];

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumeroIndividu.get(numeroIndividu);
			if (rl == null) {
				rl = new ArrayList<>();
				rapportsByNumeroIndividu.put(numeroIndividu, rl);
			}

			rl.addAll(rapportsByNumero.get(numero));
		}

		final Set<Long> numerosIndividus = rapportsByNumeroIndividu.keySet();
		final StandardBatchIterator<Long> iterator = new StandardBatchIterator<>(numerosIndividus, 500);
		while (iterator.hasNext()) {
			final List<Long> batch = iterator.next();
			try {
				final List<Individu> individus = serviceCivilService.getIndividus(batch, null);
				for (Individu ind : individus) {
					final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(ind.getNoTechnique());
					if (rl == null) {
						throw new IllegalArgumentException();
					}
					for (RapportsPrestationView.Rapport rapport : rl) {
						rapport.nomCourrier = Collections.singletonList(serviceCivilService.getNomPrenom(ind));
						rapport.noAVS = getNumeroAvs(ind);
					}
				}
			}
			catch (ServiceCivilException e) {
				LOGGER.debug("Impossible de charger le lot d'individus [" + batch + "], on continue un-par-un. L'erreur est : " + e.getMessage());
				// on recommence, un-par-un
				for (Long numero : batch) {
					try {
						Individu ind = serviceCivilService.getIndividu(numero, null);
						if (ind != null) {
							final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(ind.getNoTechnique());
							if (rl == null) {
								throw new IllegalArgumentException();
							}
							for (RapportsPrestationView.Rapport rapport : rl) {
								rapport.nomCourrier = Collections.singletonList(serviceCivilService.getNomPrenom(ind));
								rapport.noAVS = getNumeroAvs(ind);
							}
						}
					}
					catch (ServiceCivilException ex) {
						LOGGER.warn("Impossible de charger l'individu [" + numero + "]. L'erreur est : " + ex.getMessage(), ex);
						// on affiche le message d'erreur directement dans la page, pour éviter qu'il soit perdu
						final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(numero);
						if (rl == null) {
							throw new IllegalArgumentException();
						}
						for (RapportsPrestationView.Rapport rapport : rl) {
							rapport.nomCourrier = Collections.singletonList("##erreur## : " + ex.getMessage());
							rapport.noAVS = "##erreur##";
						}
					}
				}
			}
		}

		final long endH = System.nanoTime();
		LOGGER.debug("- chargement des habitants en " + ((endH - startH) / 1000000) + " ms");

		view.idDpi = noDebiteur;
		view.tiersGeneral = tiersGeneralManager.getDebiteur(debiteur, true);
		view.editionAllowed = SecurityHelper.isAnyGranted(securityProvider, Role.RT, Role.CREATE_MODIF_DPI);
		view.rapports = rapports;
	}

	private static String getNumeroAvs(Individu ind) {
		final String noAVS;
		if (StringUtils.isBlank(ind.getNouveauNoAVS())) {
			noAVS = FormatNumeroHelper.formatAncienNumAVS(ind.getNoAVS11());
		}
		else {
			noAVS = FormatNumeroHelper.formatNumAVS(ind.getNouveauNoAVS());
		}
		return noAVS;
	}

	private static String getNomPrenom(String prenom, String nom) {
		final String nomPrenom;
		if (nom != null && prenom != null) {
			nomPrenom = String.format("%s %s", prenom, nom);
		}
		else if (nom != null) {
			nomPrenom = nom;
		}
		else if (prenom != null) {
			nomPrenom = prenom;
		}
		else {
			nomPrenom = "";
		}
		return nomPrenom;
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}
}
