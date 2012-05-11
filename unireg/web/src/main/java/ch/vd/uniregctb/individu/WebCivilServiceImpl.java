package ch.vd.uniregctb.individu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Re-organisation des informations de l'individu
 *
 * @author Francois Dardare
 *
 */
public class WebCivilServiceImpl implements WebCivilService, MessageSourceAware {

	//private static final Logger LOGGER = Logger.getLogger(WebCivilServiceImpl.class);

	private ServiceCivilService serviceCivilService;

	private MessageSource messageSource;

	/**
	 * @return the serviceCivil
	 */
	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	/**
	 * @param serviceCivil
	 *            the serviceCivil to set
	 */
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu
	 *
	 * @return un objet IndividuView
	 */
	@Override
	public IndividuView getIndividu(Long numeroIndividu) {
		final Individu indSource = getServiceCivilService().getIndividu(numeroIndividu, null);
		if (indSource == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.individu.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		// Copie les données de l'individu
		return alimenteIndividuView(indSource);
	}

	/**
	 * Copie les propriétés d'un Individu de type hostinterface en Individu du registre (interface)
	 * @param indSource
	 * @return
	 */
	private IndividuView alimenteIndividuView(Individu indSource) {
		final IndividuView indCible = new IndividuView();
		indCible.setNumeroIndividu(indSource.getNoTechnique());
		indCible.setNom(indSource.getNom());
		indCible.setPrenom(indSource.getPrenom());
		indCible.setNomNaissance(indSource.getNomNaissance());
		indCible.setAutresPrenoms(indSource.getAutresPrenoms());
		indCible.setDateNaissance(RegDate.asJavaDate(indSource.getDateNaissance()));
		indCible.setAncienNumeroAVS(indSource.getNoAVS11());
		indCible.setNumeroAssureSocial(indSource.getNouveauNoAVS());
		indCible.setNumeroRCE( indSource.getNumeroRCE() );
		traiteSexe(indSource, indCible);
		traitePermis(indSource.getNoTechnique(), indCible);
		traiteEtatCivil(indSource, indCible);
		traiteOrigine(indSource.getNoTechnique(), indCible);
		traiteNationalite(indSource.getNoTechnique(), indCible);
		return indCible;
	}

	/**
	 * Traitement du sexe
	 *
	 * @param indSource
	 * @param indCible
	 */
	private void traiteSexe(Individu indSource, IndividuView indCible) {

		final Sexe sexe;
		if (indSource.isSexeMasculin()) {
			sexe = Sexe.MASCULIN;
		}
		else {
			sexe = Sexe.FEMININ;
		}
		indCible.setSexe(sexe);
	}

	/**
	 * Traitement du permis de travail
	 *
	 * @param numeroIndividu un numéro d'individu
	 * @param view           la vue à compléter
	 */
	private void traitePermis(Long numeroIndividu, IndividuView view) {
		final Collection<Permis> permis = getServiceCivilService().getPermis(numeroIndividu, null);
		if (permis != null && !permis.isEmpty()) {
			final List<PermisView> list = new ArrayList<PermisView>();
			for (Permis p : permis) {
				list.add(new PermisView(p));
			}
			Collections.sort(list, new PermisViewComparator());
			view.setPermisView(list);
		}
		else {
			view.setPermisView(Collections.<PermisView>emptyList());
		}
	}

	/**
	 * Traitement de l'etat Civil
	 *
	 * @param indSource
	 * @param indCible
	 */
	private void traiteEtatCivil(Individu indSource, IndividuView indCible) {
		if (indSource != null) {

			final EtatCivil etatCivil = indSource.getEtatCivilCourant();
			if (etatCivil != null) {
				indCible.setEtatCivil(EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil()).name());
				indCible.setDateDernierChgtEtatCivil(RegDate.asJavaDate(etatCivil.getDateDebut()));
			}

			/* Cas particulier du décès qui ne correspond pas à un état civil dans le host */
			if (indSource.getDateDeces() != null) {
				indCible.setEtatCivil("DECEDE");
				indCible.setDateDernierChgtEtatCivil(RegDate.asJavaDate(indSource.getDateDeces()));
			}

		}
	}

	/**
	 * Origine
	 */
	private void traiteOrigine(Long numeroIndividu, IndividuView indCible) {
		final Collection<Origine> origines = getServiceCivilService().getOrigines(numeroIndividu, null);
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
	private void traiteNationalite(Long numeroIndividu, IndividuView indCible) {

		final Collection<Nationalite> nationalites = getServiceCivilService().getNationalites(numeroIndividu, null);
		if (nationalites != null && !nationalites.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			for (Nationalite nationalite : nationalites) {
				if (nationalite.getDateFinValidite() == null) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append(nationalite.getPays().getNomMinuscule());
				}
			}
			indCible.setNationalite(b.toString());
		}
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;

	}
}
