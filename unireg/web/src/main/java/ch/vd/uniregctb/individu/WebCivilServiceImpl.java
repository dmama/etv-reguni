package ch.vd.uniregctb.individu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.StatutIndividu;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.NationaliteHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Re-organisation des informations de l'individu
 *
 * @author Francois Dardare
 *
 */
public class WebCivilServiceImpl implements WebCivilService, MessageSourceAware {

	private ServiceCivilService serviceCivilService;
	private MessageSource messageSource;

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * {@inheritDoc}
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException si on ne retrouve pas d'individu correspondant
	 */
	@Override
	public IndividuView getIndividu(Long numeroIndividu) {
		final Individu indSource = serviceCivilService.getIndividu(numeroIndividu, null, AttributeIndividu.NATIONALITES, AttributeIndividu.PERMIS, AttributeIndividu.ORIGINE);
		if (indSource == null) {
			throw new ObjectNotFoundException(this.messageSource.getMessage("error.individu.inexistant", new Object[] {Long.toString(numeroIndividu)},  WebContextUtils.getDefaultLocale()));
		}

		// Copie les données de l'individu
		return alimenteIndividuView(indSource);
	}

	/**
	 * {@inheritDoc}
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException Si on ne trouve l'individu ni pas son numero ou par un numero d'evt
	 */
	@Override
	public IndividuView getIndividu(Long numeroIndividu, Long numeroEvenement) {
		IndividuView individuView = null;
		try {
			individuView = getIndividu(numeroIndividu);
		}
		catch (ObjectNotFoundException e) {
			final IndividuApresEvenement indiv = serviceCivilService.getIndividuAfterEvent(numeroEvenement);
			if (indiv == null) {
				throw new ObjectNotFoundException(this.messageSource.getMessage("error.individu.inexistant", new Object[] {Long.toString(numeroIndividu)},  WebContextUtils.getDefaultLocale()));
			}
			individuView = alimenteIndividuView(indiv.getIndividu());
		}
		return individuView;
	}

	/**
	 * Copie les propriétés d'un Individu du registre civil dans une vue IndividuView du registre
	 * @param indSource l'individu source
	 * @return la vue alimentée
	 */
	private IndividuView alimenteIndividuView(final Individu indSource) {
		final IndividuView indCible = new IndividuView();
		indCible.setNumeroIndividu(indSource.getNoTechnique());
		indCible.setNom(indSource.getNom());
		indCible.setPrenom(indSource.getPrenom());
		indCible.setNomNaissance(indSource.getNomNaissance());
		indCible.setAutresPrenoms(indSource.getAutresPrenoms());
		indCible.setDateNaissance(indSource.getDateNaissance());
		indCible.setAncienNumeroAVS(indSource.getNoAVS11());
		indCible.setNumeroAssureSocial(indSource.getNouveauNoAVS());
		indCible.setNumeroRCE(indSource.getNumeroRCE());
		traiteStatut(indSource.getStatut(), indCible);
		traiteSexe(indSource, indCible);
		traitePermis(indSource.getPermis(), indCible);
		traiteEtatCivil(indSource, indCible);
		traiteOrigine(indSource.getOrigines(), indCible);
		traiteNationalite(indSource, indCible);
		return indCible;
	}

	/**
	 * Traitement du sexe
	 *
	 * @param indSource individu source
	 * @param indCible vue destination
	 */
	private void traiteSexe(Individu indSource, IndividuView indCible) {
		indCible.setSexe(indSource.getSexe());
	}

	/**
	 * Traitement du statut RCPers
	 * @param statut statut connu dans le civil
	 * @param indCible vue de destination pour affichage dans Unireg
	 */
	private void traiteStatut(StatutIndividu statut, IndividuView indCible) {
		if (statut != null) {
			indCible.setCanceled(!statut.isActive());
			indCible.setNumeroIndividuRemplacant(statut.getNoIndividuRemplacant());
		}
		else {
			indCible.setCanceled(false);
			indCible.setNumeroIndividuRemplacant(null);
		}
	}

	/**
	 * Traitement du permis de travail
	 *
	 * @param permis    la liste des permis de l'individu
	 * @param indCible  la vue à compléter
	 */
	private void traitePermis(PermisList permis, IndividuView indCible) {
		if (permis != null && !permis.isEmpty()) {
			final List<PermisView> list = new ArrayList<>();
			for (Permis p : permis) {
				list.add(new PermisView(p));
			}
			Collections.sort(list, new PermisViewComparator());
			indCible.setPermisView(list);
		}
		else {
			indCible.setPermisView(Collections.<PermisView>emptyList());
		}
	}

	/**
	 * Traitement de l'etat Civil
	 *
	 * @param indSource individu source
	 * @param indCible vue destination
	 */
	private void traiteEtatCivil(Individu indSource, IndividuView indCible) {
		if (indSource != null) {

			final EtatCivil etatCivil = indSource.getEtatCivilCourant();
			if (etatCivil != null) {
				indCible.setEtatCivil(EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil()).name());
				indCible.setDateDernierChgtEtatCivil(etatCivil.getDateDebut());
			}

			/* Cas particulier du décès qui ne correspond pas à un état civil dans le host */
			if (indSource.getDateDeces() != null) {
				indCible.setEtatCivil("DECEDE");
				indCible.setDateDernierChgtEtatCivil(indSource.getDateDeces());
			}

		}
	}

	/**
	 * Origine
	 */
	private void traiteOrigine(Collection<Origine> origines, IndividuView indCible) {
		if (origines != null && !origines.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			for (Origine origine : origines) {
				if (b.length() > 0) {
					b.append(", ");
				}
				b.append(origine.getNomLieu());
			}
			indCible.setOrigine(b.toString());
		}
	}

	/**
	 * Nationalite
	 */
	private void traiteNationalite(Individu individu, IndividuView indCible) {
		final List<Nationalite> nationalites = NationaliteHelper.validAt(individu.getNationalites(), null);
		if (nationalites.size() > 0) {
			final StringBuilder b = new StringBuilder();
			for (Nationalite nat : nationalites) {
				if (b.length() > 0) {
					b.append(", ");
				}
				b.append(nat.getPays().getNomCourt());
			}
			indCible.setNationalite(b.toString());
		}
	}
}
