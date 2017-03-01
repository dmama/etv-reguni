package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.admin.indexer.GestionIndexation;
import ch.vd.uniregctb.admin.indexer.IndexDocument;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.SearchCallback;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexableData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class GestionIndexationController extends AbstractSimpleFormController {

	private final Logger LOGGER = LoggerFactory.getLogger(GestionIndexationController.class);

	private GlobalIndexInterface globalIndex;
	private IndexationManager indexationManager;
	private ServiceCivilService serviceCivil;
	private DataEventService dataEventService;
	private SecurityProviderInterface securityProvider;

	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String ACTION_SEARCH_VALUE = "search";
	private static final String ACTION_REINDEX_TIERS = "reindexTiers";
	private static final String ACTION_RELOAD_INDIVIDU = "reloadIndividu";
	public static final String INDEX_LIST_ATTRIBUTE_NAME = "index";
	public static final String GESTION_INDEXATION_NAME = "gestionIndexation";
	private static final int maxHits = 100;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		GestionIndexation bean = (GestionIndexation) session.getAttribute(GESTION_INDEXATION_NAME);
		if (bean == null) {
			bean = (GestionIndexation) super.formBackingObject(request);
			session.setAttribute(GESTION_INDEXATION_NAME, bean);
		}
		bean.setChemin(globalIndex.getIndexPath());
		bean.setNombreDocumentsIndexes(globalIndex.getApproxDocCount());
		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
		throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		HttpSession session = request.getSession();
		GestionIndexation bean = (GestionIndexation) session.getAttribute(GESTION_INDEXATION_NAME);
		if ((bean != null) && (bean.getRequete() != null) && (!"".equals(bean.getRequete()))) {
			executeSearch(session, bean) ;
		}
		mav.addObject(INDEX_LIST_ATTRIBUTE_NAME, session.getAttribute(INDEX_LIST_ATTRIBUTE_NAME));

		// On flush l'indexer, c'est utile pour le debugging
		// Le fait d'accèder a la page de gestion de l'indexation
		// va provoquer un flush et l'index pourra ensuite etre copié sans problemes
		globalIndex.flush();
		LOGGER.debug("The Global index is flushed");

		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);

		// Validate seulement si on fait une recherche
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action.equals(ACTION_SEARCH_VALUE)) {
			GestionIndexation bean = (GestionIndexation)command;
			try {
				globalIndex.search(bean.getRequete(), maxHits, new SearchCallback() {
					@Override
					public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
						// on n'est pas intéressé par les résultats
					}
				});
			}
			catch (Exception e) {
				errors.rejectValue("requete", "error.criteres.invalide");
			}
			LOGGER.debug("La requete '"+bean.getRequete()+"' est valide");
		}
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		GestionIndexation bean = (GestionIndexation) command;
		HttpSession session = request.getSession();
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action.equals(ACTION_SEARCH_VALUE)) {
			if ((bean != null) && (bean.getRequete() != null) && (!"".equals(bean.getRequete()))) {
				executeSearch(session, bean) ;
			}
		}
		else if (action.equals(ACTION_REINDEX_TIERS)) {
			if (bean != null) {
				final String idAsString = FormatNumeroHelper.removeSpaceAndDash(bean.getId());
				final long id = Long.parseLong(idAsString);
				indexationManager.reindexTiers(id);
				flash("Le tiers a été réindexé.");
				return new ModelAndView(new RedirectView("/tiers/visu.do?id=" + id, true));
			}
		}
		else if (action.equals(ACTION_RELOAD_INDIVIDU)) {
			if (bean != null) {
				final String idAsString = FormatNumeroHelper.removeSpaceAndDash(bean.getIndNo());
				final long id = Long.parseLong(idAsString);
				serviceCivil.setIndividuLogging(bean.isLogIndividu());
				try {
					dataEventService.onIndividuChange(id);
					final Individu individu = serviceCivil.getIndividu(id, null);
					if (individu == null) {
						flash("L'individu n°" + id + " n'existe pas.");
					}
					else {
						flash("L'individu n°" + id + " a été rechargé.");
					}
				}
				finally {
					serviceCivil.setIndividuLogging(false);
				}
			}
		}
		mav.setView(new RedirectView(getSuccessView()));
		return mav;
	}

	/**
	 * Met a jour les listes en Session
	 *
	 * @param session la session http
	 * @param bean    les données du formulaire de recherche
	 */
	private void executeSearch(HttpSession session, GestionIndexation bean) {

		final List<IndexDocument> listIndexDocument = new ArrayList<>();

		globalIndex.search(bean.getRequete(), maxHits, new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				for (ScoreDoc h : hits.scoreDocs) {
					final Document doc;
					try {
						doc = docGetter.get(h.doc);
					}
					catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
						continue; // rien de mieux à faire
					}
					final IndexDocument indexDocument = new IndexDocument();
					indexDocument.setEntityId(doc.get(LuceneHelper.F_ENTITYID));
					indexDocument.setNomCourrier1(doc.get(TiersIndexableData.NOM1));
					indexDocument.setNomCourrier2(doc.get(TiersIndexableData.NOM2));
					indexDocument.setDateNaissance(doc.get(TiersIndexableData.D_DATE_NAISSANCE));
					indexDocument.setNumeroAvs(concat(doc.get(TiersIndexableData.NAVS13), doc.get(TiersIndexableData.NAVS11)));
					indexDocument.setNomFor(doc.get(TiersIndexableData.FOR_PRINCIPAL));
					indexDocument.setNpa(doc.get(TiersIndexableData.NPA_COURRIER));
					indexDocument.setLocalite(doc.get(TiersIndexableData.LOCALITE));
					listIndexDocument.add(indexDocument);
				}
			}
		});

		session.setAttribute(INDEX_LIST_ATTRIBUTE_NAME, listIndexDocument);
		session.setAttribute(GESTION_INDEXATION_NAME, bean);
	}

	private static String concat(String... strs) {
		if (strs == null || strs.length == 0) {
			return StringUtils.EMPTY;
		}
		else if (strs.length == 1) {
			return StringUtils.trimToEmpty(strs[0]);
		}
		else {
			final StringBuilder b = new StringBuilder();
			for (String str : strs) {
				if (b.length() > 0) {
					b.append(' ');
				}
				b.append(StringUtils.trimToEmpty(str));
			}
			return b.toString();
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexationManager(IndexationManager indexationManager) {
		this.indexationManager = indexationManager;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}

