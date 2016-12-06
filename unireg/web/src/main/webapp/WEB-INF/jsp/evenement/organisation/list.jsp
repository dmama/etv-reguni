<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="listEvenementsOrganisation" type="java.util.List<ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationElementListeRechercheView>"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="head">
        <script type="text/javascript">
             $(document).ready(function () {
                 $('#modeLotEvenement').change( function () {
                    if (this.checked) {
                        $('#tableForm tr.toggle').hide()
                    } else {
                        $('#tableForm tr.toggle').show()
                    }
                 }).change();

                 $('#rechercher').click( function () {
                 	$('#formRechercheEvenements').attr('action','rechercher.do');
                 });

                 $('#effacer').click( function () {
                 	window.location.href = 'effacer.do';
                 	return false;
                 });

             });
        </script>
    </tiles:put>

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.organisation" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	    <c:choose>
		    <c:when test="${capping == 'EN_ERREUR'}">
			    <div class="flash-error"><fmt:message key="label.traitement.systematique.erreur"/></div>
		    </c:when>
		    <c:when test="${capping == 'A_VERIFIER'}">
			    <div class="flash-warning"><fmt:message key="label.traitement.systematique.aVerifier"/></div>
		    </c:when>
		    <c:otherwise>
			    <!-- rien à afficher -->
		    </c:otherwise>
	    </c:choose>

		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" commandName="evenementOrganisationCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

        <c:set var="sortable" value='${not evenementOrganisationCriteria.modeLotEvenement}' scope="page" />
		<display:table 	name="listEvenementsOrganisation" id="tableEvtsOrganisation" pagesize="25" requestURI="/evenement/organisation/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsOrganisationSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<display:column >
				<a href="#" class="jTip-evtinfo infotraitement" title="<c:url value="/evenement/organisation/summary.do?id=${tableEvtsOrganisation.id}"/>" id="messages-${tableEvtsOrganisation.id}" ></a>
			</display:column>

			<!-- No Evenement -->
			<display:column property="noEvenement" sortable ="${sortable}" titleKey="label.no.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="noEvenement"/>
			<!-- NO Organisation -->
			<display:column titleKey="label.no.cantonal">
				<a href="#" class="staticTip" id="tt-${tableEvtsOrganisation.numeroOrganisation}">
					<div id="tt-${tableEvtsOrganisation.numeroOrganisation}-tooltip" style="display:none;">
						<h3>Organisation n°${tableEvtsOrganisation.numeroOrganisation}</h3>
						<fieldset>
							<unireg:nextRowClass reset="1"/>
							<table>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.numero.registre.entreprises"/> :</td>
									<td>${tableEvtsOrganisation.organisation.numeroOrganisation}</td>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.raison.sociale"/> :</td>
									<td>${tableEvtsOrganisation.organisation.nom}</td>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.forme.juridique"/>&nbsp;:</td>
									<td style="min-width: 300px"><c:out value="${tableEvtsOrganisation.organisation.formeJuridique}"/></td>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td width="130px"><fmt:message key="label.categorie"/>&nbsp;:</td>
									<td><c:out value="${tableEvtsOrganisation.organisation.categorie}"/> (<fmt:message key="option.categorie.entreprise.${tableEvtsOrganisation.organisation.categorie}"/>)</td>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.siege"/>&nbsp;:</td>
									<c:choose>
										<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_OU_FRACTION_VD'}">
											<td><unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/></td>
										</c:when>
										<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_HC'}">
											<td><unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS"/></td>
										</c:when>
										<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'PAYS_HS'}">
											<td><unireg:pays ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/></td>
										</c:when>
									</c:choose>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.numero.ide"/>&nbsp;:</td>
									<td><unireg:numIDE numeroIDE="${tableEvtsOrganisation.organisation.numeroIDE}"/></td>
								</tr>
							</table>
						</fieldset>
					</div>
					<span>${tableEvtsOrganisation.numeroOrganisation}&nbsp;</span>
				</a>
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvtsOrganisation.numeroCTB != null}">
					<a href="<c:url value="../../tiers/visu.do"/>?id=${tableEvtsOrganisation.numeroCTB}"><unireg:numCTB numero="${tableEvtsOrganisation.numeroCTB}" /></a>
				</c:if>
			</display:column>
			<!-- Raison sociale -->
			<display:column titleKey="label.raison.sociale">
				<c:out value="${tableEvtsOrganisation.nom}" />
			</display:column>
			<!-- Siège -->
			<display:column titleKey="label.siege">
				<c:choose>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_OU_FRACTION_VD'}">
						<unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/>
					</c:when>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_HC'}">
						<unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS"/>
					</c:when>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'PAYS_HS'}">
						<unireg:pays ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/>
					</c:when>
				</c:choose>
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.organisation.${tableEvtsOrganisation.type}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="${sortable}" titleKey="label.date" sortName="dateEvenement">
				<span><unireg:regdate regdate="${tableEvtsOrganisation.dateEvenement}" />&nbsp;</span>
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="${sortable}" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="${sortable}" titleKey="label.etat.evenement" sortName="etat">
				<fmt:message key="option.etat.evenement.${tableEvtsOrganisation.etat}" />
			</display:column>
			<display:column>
				<c:if test="${tableEvtsOrganisation.correctionDansLePasse == true}">
					<a href="#" class="alert" title="<fmt:message key="label.correction.passe"/>"></a>
				</c:if>
			</display:column>
			<display:column style="action">
				<a href="#" class="detail" title="Détails" onclick="open_details(<c:out value="${tableEvtsOrganisation.id}"/>); return false;">&nbsp;</a>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvtsOrganisation.id != null}">
					<unireg:consulterLog entityNature="EvenementOrganisation" entityId="${tableEvtsOrganisation.id}"/>
				</c:if>
		</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
<script language="JavaScript">

	EvtOrg.list = [
		<c:forEach var="evt" items="${listEvenementsOrganisation}" varStatus="evtStatus">
		${evt.id}
		<c:if test="${!evtStatus.last}">
		,
		</c:if>
		</c:forEach>
	]

</script>
<script language="javascript">
	/**
	 * Variante de la version jTip globale avec un positionnement en dessous de la ligne de l'événement concerné.
	 */
	function activate_evtinfo_tooltips() {
		$(".jTip-evtinfo").tooltip({
			                   items: "[title]",
			                   position: { my: "right top", at: "right bottom" },
			                   content: function(response) {
				                   var url = $(this).attr("title");
				                   $.get(url, response);
				                   return "Chargement...";
			                   }
		                   });
	}
	function activate_static_evt_tooltips(obj) {
		$(".staticTip", obj).tooltip({
			                             items: "[id]",
			                             position: { my: "right top", at: "right bottom" },
			                             content: function() {
				                             // on détermine l'id de la div qui contient le tooltip à afficher
				                             var id = $(this).attr("id") + "-tooltip";
				                             id = id.replace(/\./g, '\\.'); // on escape les points

				                             // on récupère la div et on affiche son contenu
				                             var div = $("#" + id);
				                             return div.html();
			                             }
		                             });
	}

	/**
	 * Ouvrir le panneau de détail après avoir déterminé les ids de l'événement précédent et suivant.
	 *
	 * Selon la position de l'événement dans la liste, le précédent ou le suivant peut être null.
	 *
	 * Il faut gérer spécialement le cas où l'id de l'événement dont l'ouverture est demandé ne figure pas dans la liste, par exemple
	 * parce qu'il a été forcé. Dans ce cas, il faut prendre l'id conservé dans la variable nextId qui sert justement à accueillir
	 * cette valeur qui est passée lors de l'action de forçage. Lorsqu'on ne trouve pas de nextId, on prend le premier de la liste
	 * comme suivant.
	 *
	 * @param evtId l'id de l'événement dont on souhaite afficher le détail.
	 * @returns {null}
	 */
	function open_details(evtId) {
		if (!EvtOrg.list || EvtOrg.list.length == 0) return null;
		var evtPrecedant = null;
		var evtSuivant = null;
		var indexEvt = EvtOrg.list.indexOf(evtId);
		if (indexEvt == -1) {
			if (EvtOrg.nextId) {
				evtSuivant = EvtOrg.nextId;
			}
			if (!evtSuivant) {
				evtSuivant = EvtOrg.list[0];
			}
		}
		else {
			if (indexEvt > 0) {
				evtPrecedant = EvtOrg.list[indexEvt - 1];
			}
			if (indexEvt < EvtOrg.list.length - 1) {
				evtSuivant = EvtOrg.list[indexEvt + 1];
			}
		}
		EvtOrg.open_details(evtId, evtPrecedant, evtSuivant)
	}


	activate_evtinfo_tooltips();
	activate_static_evt_tooltips();

	/*
	Ce qui suit permet de rouvrir la fenêtre de détail de l'événement concerné par une action (recyclage, forçage, etc...) grâce
	au paramtères passé lors de l'appel à l'action.
	 */
	<c:if test="${nextEvtId != null}">
	EvtOrg.nextId = ${nextEvtId};
	</c:if>
	<c:if test="${selectedEvtId != null}">
	open_details(${selectedEvtId});
	</c:if>
</script>

