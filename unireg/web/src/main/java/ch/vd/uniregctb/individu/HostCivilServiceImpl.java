package ch.vd.uniregctb.individu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Re-organisation des informations de l'individu
 *
 * @author Francois Dardare
 *
 */
public class HostCivilServiceImpl implements HostCivilService, MessageSourceAware {

	//private static final Logger LOGGER = Logger.getLogger(HostCivilServiceImpl.class);

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
	public IndividuView getIndividu(Long numeroIndividu) {
		IndividuView indCible = null;

		Individu indSource = getServiceCivilService().getIndividu(numeroIndividu.longValue(), DateHelper.getCurrentYear());

		if (indSource == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.individu.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (indSource != null) {
			// Copie les données de l'individu
			indCible = alimenteIndividuView(indSource);
		}
		return indCible;
	}

	/**
	 * Copie les propriétés d'un Individu de type hostinterface en Individu du registre (interface)
	 * @param indSource
	 * @return
	 */
	private IndividuView alimenteIndividuView(Individu indSource) {
		IndividuView indCible = null;
		indCible = new IndividuView();
		indCible.setNumeroIndividu(indSource.getNoTechnique());
		indCible.setNom(indSource.getDernierHistoriqueIndividu().getNom());
		indCible.setPrenom(indSource.getDernierHistoriqueIndividu().getPrenom());
		indCible.setNomNaissance(indSource.getDernierHistoriqueIndividu().getNomNaissance());
		indCible.setAutresPrenoms(indSource.getDernierHistoriqueIndividu().getAutresPrenoms());
		indCible.setDateNaissance(RegDate.asJavaDate(indSource.getDateNaissance()));
		indCible.setAncienNumeroAVS(indSource.getDernierHistoriqueIndividu().getNoAVS());
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

		Sexe sexe;
		if (indSource.isSexeMasculin())
			sexe = Sexe.MASCULIN;
		else
			sexe = Sexe.FEMININ;
		indCible.setSexe(sexe);
	}

	/**
	 * Traitement du permis de travail
	 *
	 * @param numeroIndividu
	 * @param indSource
	 * @param indCible
	 */
	private void traitePermis(Long numeroIndividu, IndividuView indCible) {
		List<PermisView> lPermisView = new ArrayList<PermisView>();
		Collection<Permis> colPermis = getServiceCivilService().getPermis(numeroIndividu.longValue(), DateHelper.getCurrentYear());
		for (Permis permis : colPermis) {
			PermisView permisView = new PermisView();
			if (permis.getTypePermis() != null) {
				permisView.setTypePermis(permis.getTypePermis().getName());
			}
			permisView.setDateDebutValidite(permis.getDateDebutValidite());
			permisView.setDateFinValidite(permis.getDateFinValidite());
			if (permis.getDateAnnulation() != null) {
				permisView.setAnnule(true);
			}
			else {
				permisView.setAnnule(false);
			}
			lPermisView.add(permisView);
		}
		Collections.sort(lPermisView, new PermisViewComparator());
		indCible.setPermisView(lPermisView);
	}

	/**
	 * Traitement de l'etat Civil
	 *
	 * @param indSource
	 * @param indCible
	 */
	private void traiteEtatCivil(Individu indSource, IndividuView indCible) {
		if (indSource != null) {

			Collection<EtatCivil> colEtatCivil = indSource.getEtatsCivils();
			int numSeq = 0;
			for(EtatCivil etatCivil : colEtatCivil) {
				if (etatCivil.getNoSequence() > numSeq) {
					indCible.setEtatCivil(EtatCivilHelper.getString(etatCivil.getTypeEtatCivil()));
					indCible.setDateDernierChgtEtatCivil(RegDate.asJavaDate(etatCivil.getDateDebutValidite()));
				}
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
		Origine origine = getServiceCivilService().getOrigine(numeroIndividu.longValue(), DateHelper.getCurrentYear());
		if (origine != null) {
			Commune commune = origine.getCommune();
			if (commune != null) {
				indCible.setOrigine(String.format("%s (%s)", commune.getNomMinuscule(), commune.getSigleCanton()));
			} else if (origine.getPays() != null) {
				//TODO [xcifde] A supprimer quand le rapprochement pays origine nationalite sera fini sur le Host
				indCible.setOrigine(origine.getPays().getNomMinuscule());
			}
		}
	}

	/**
	 * Nationalite
	 */
	private void traiteNationalite(Long numeroIndividu, IndividuView indCible) {

		final Collection<Nationalite> nationalites = getServiceCivilService().getNationalites(numeroIndividu.longValue(), DateHelper.getCurrentYear());
		if (nationalites == null) {
			return;
		}

		for (Nationalite nationalite : nationalites) {
			if (nationalite.getDateFinValidite() == null) {
				indCible.setNationalite(nationalite.getPays().getNomMinuscule());
			}
		}
	}

	/**
	 * Retour le nom (et prénom) de l'utilisateur en fonction du numero d'individu
	 * @param numeroIndividu
	 * @return
	 */
	public String getNomUtilisateur(Long numeroIndividu) {

		final String nomUtilisateur;
		if (numeroIndividu != null) {
			final Individu indSource = getServiceCivilService().getIndividu(numeroIndividu.longValue(), DateHelper.getCurrentYear());
			final String nom = indSource.getDernierHistoriqueIndividu().getNom();
			final String prenom = indSource.getDernierHistoriqueIndividu().getPrenom();
			final boolean blankNom = StringUtils.isBlank(nom);
			final boolean blankPrenom = StringUtils.isBlank(prenom);
			if (!blankNom && !blankPrenom) {
				nomUtilisateur = String.format("%s %s", prenom.trim(), nom.trim());
			}
			else if (!blankNom) {
				nomUtilisateur = nom.trim();
			}
			else if (!blankPrenom) {
				nomUtilisateur = prenom.trim();
			}
			else {
				nomUtilisateur = "";
			}
		}
		else {
			nomUtilisateur = "";
		}
		return nomUtilisateur;
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;

	}
}
