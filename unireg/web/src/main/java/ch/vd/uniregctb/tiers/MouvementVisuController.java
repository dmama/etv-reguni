package ch.vd.uniregctb.tiers;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.tiers.manager.MouvementVisuManager;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Controller pour l'overlay de visualisation du détail du mouvement
 *
 */
@Controller
public class MouvementVisuController implements MessageSourceAware {

	private MouvementVisuManager mouvementVisuManager;
	private MessageSource messageSource;

	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	//TODO Un check sur la securité peut-être ?
	@RequestMapping("/tiers/mouvement.do")
	@ResponseBody
	public MouvementForJSON mouvement(@RequestParam("idMvt") Long idMvt) {
		return new MouvementForJSON (mouvementVisuManager.get(idMvt)) {};
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@SuppressWarnings("UnusedDeclaration")
	public class MouvementForJSON {

		MouvementDetailView view;

		public MouvementForJSON (MouvementDetailView mouvementDetailView) {
			view = mouvementDetailView;
		}

		public String getId() {
			return view.getId() == null ? "" : view.getId().toString();
		}

		public String getCollAdmDestinataireEnvoi() {
			return view.getCollAdmDestinataireEnvoi() == null ? "" : view.getCollAdmDestinataireEnvoi();
		}

		public String getLocalisation() {

			String loc = "";

			if (view.getLocalisation() != null) {
				loc = messageSource.getMessage(
						"option.localisation." + view.getLocalisation().name(),  null,
						WebContextUtils.getDefaultLocale());
			}
			return loc;
		}

		public RegDate getDateMouvement() {
			return RegDate.get(view.getDateMouvement());
		}

		public String getTypeMouvement() {
			String type = "";

			if (view.getTypeMouvement() != null) {
				type = messageSource.getMessage(
						"option.type.mouvement." + view.getTypeMouvement().name(),  null,
						WebContextUtils.getDefaultLocale());
			}
			return type;
		}

		public String getNoCollAdmDestinataireEnvoi() {
			return view.getNoCollAdmDestinataireEnvoi() == null ? "" : view.getNoCollAdmDestinataireEnvoi().toString();
		}

		public String getIdTache() {
			return view.getIdTache() == null ? "" : view.getIdTache().toString();
		}

		public String getNumeroUtilisateurEnvoi() {
			return view.getNumeroUtilisateurEnvoi() == null ? "" : view.getNumeroUtilisateurEnvoi().toString();
		}

		public String getNumeroUtilisateurReception() {
			return view.getNumeroUtilisateurReception() == null ? "" : view.getNumeroUtilisateurReception().toString();
		}

		public String getEtatMouvement() {
			String etat = "";

			if (view.getEtatMouvement() != null) {
				etat = messageSource.getMessage(
						"option.type.mouvement." + view.getEtatMouvement().name(),  null,
						WebContextUtils.getDefaultLocale());
			}
			return etat;
		}

		public String getCollectiviteAdministrative() {
			return view.getCollectiviteAdministrative() == null ? "" : view.getCollectiviteAdministrative();
		}

		public String getDestinationUtilisateur() {
			return view.getDestinationUtilisateur() == null ? "" : view.getDestinationUtilisateur();
		}

		public String getNomPrenomUtilisateur() {
			return view.getNomPrenomUtilisateur() == null ? "" : view.getNomPrenomUtilisateur();
		}

		public String getNumeroTelephoneUtilisateur() {
			return view.getNumeroTelephoneUtilisateur() == null ? "" : view.getNumeroTelephoneUtilisateur();
		}

		public String getDestinationEnvoi() {
			return view.getDestinationEnvoi() == null ? "" : view.getDestinationEnvoi();
		}

		public String getDateExecution() {
			return DateHelper.dateTimeToDisplayString(view.getDateExecution());
		}

		public String getExecutant() {
			return view.getExecutant() == null ? "" : view.getExecutant();
		}

		public String getUtilisateurEnvoi() {
			return view.getUtilisateurEnvoi() == null ? "" : view.getUtilisateurEnvoi();
		}

		public String getUtilisateurReception() {
			return view.getUtilisateurReception() == null ? "" : view.getUtilisateurReception();
		}
	}
}
