package ch.vd.unireg.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.admin.indexer.GestionIndexation;
import ch.vd.unireg.admin.indexer.IndexDocument;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.lucene.LuceneHelper;
import ch.vd.unireg.indexer.tiers.TiersIndexableData;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;

/**
 * Controller d'administration de l'indexer Lucene
 */
@Controller
@RequestMapping(value = "/admin/indexation")
public class GestionIndexationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(GestionIndexationController.class);

	private GlobalIndexInterface globalIndex;
	private IndexationManager indexationManager;
	private ServiceCivilService serviceCivil;
	private DataEventService dataEventService;
	private SecurityProviderInterface securityProvider;

	private static final int maxHits = 100;

	@RequestMapping(value = "/show.do", method = RequestMethod.GET)
	public String show(@RequestParam(value = "requete", required = false) String requete, Model model) {

		final List<IndexDocument> docs;
		if (StringUtils.isNotBlank(requete)) {
			if (SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
				docs = executeSearch(requete);
			}
			else {
				Flash.warning("Vous n'avez le droit de faire des requêtes Lucene directes. Aucun résultat affiché.");
				docs = Collections.emptyList();
			}
		}
		else {
			docs = Collections.emptyList();
		}

		final GestionIndexation bean = new GestionIndexation();
		bean.setRequete(requete);

		model.addAttribute("docs", docs);
		model.addAttribute("command", bean);
		model.addAttribute("cheminIndex", globalIndex.getIndexPath());
		model.addAttribute("nombreDocumentsIndexes", globalIndex.getApproxDocCount());
		return "admin/indexation";
	}

	@NotNull
	private List<IndexDocument> executeSearch(String requete) {
		final List<IndexDocument> docs = new ArrayList<>();

		globalIndex.search(requete, maxHits, (hits, docGetter) -> {
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
				docs.add(indexDocument);
			}
		});
		return docs;
	}

	@RequestMapping(value = "/reindexTiers.do", method = RequestMethod.POST)
	public String reindexTiers(@ModelAttribute final GestionIndexation data) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final Long id = data.getId();
		if (id != null) {
			indexationManager.reindexTiers(id);
			Flash.message("Le tiers a été réindexé.");
		}
		else {
			Flash.warning("Veuillez renseigner un numéro de tiers.");
		}

		return "redirect:/tiers/visu.do?id=" + id;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/reloadIndividu.do", method = RequestMethod.POST)
	public String reloadIndividu(@ModelAttribute final GestionIndexation data) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final Long id = data.getIndNo();
		if (id != null) {
			dataEventService.onIndividuChange(id);
			final Individu individu = serviceCivil.getIndividu(id, null);
			if (individu == null) {
				Flash.warning("L'individu n°" + id + " n'existe pas.");
			}
			else {
				Flash.message("L'individu n°" + id + " a été rechargé.");
			}
		}
		else {
			Flash.warning("Veuillez renseigner un numéro d'individu.");
		}

		return "redirect:/admin/indexation/show.do";
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

