<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="listEvenementsEch" type="java.util.List<ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView>"--%>

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

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.ech" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" commandName="evenementEchCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

        <c:set var="sortable" value='${not evenementEchCriteria.modeLotEvenement}' scope="page" />
		<display:table 	name="listEvenementsEch" id="tableEvtsEch" pagesize="25" requestURI="/evenement/ech/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsEchSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="${sortable}" titleKey="label.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />
			<!-- NO Individu + Conjoint -->
			<display:column sortable ="${sortable}" titleKey="label.individu" sortProperty="numeroIndividu" sortName="numeroIndividu">
				${tableEvtsEch.numeroIndividu}
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvtsEch.numeroCTB != null}">
					<unireg:numCTB numero="${tableEvtsEch.numeroCTB}" />
				</c:if>
			</display:column>
			<!-- Nom  /Prénom -->
			<display:column titleKey="label.prenom.nom">
				<c:out value="${tableEvtsEch.nom}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.ech.${tableEvtsEch.type}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.action.evenement" sortName="action">
				<fmt:message key="option.action.evenement.ech.${tableEvtsEch.action}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="${sortable}" titleKey="label.date.evenement" sortName="dateEvenement">
				<unireg:regdate regdate="${tableEvtsEch.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="${sortable}" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="${sortable}" titleKey="label.etat.evenement" sortName="etat" >
				<fmt:message key="option.etat.evenement.${tableEvtsEch.etat}" />
			</display:column>
			<display:column titleKey="label.commentaire.traitement">
				<i><c:out value="${tableEvtsEch.commentaireTraitement}"/></i>
			</display:column>
			<display:column style="action">
				<a href="#" class="detail" title="Détails" onclick="open_details(<c:out value="${tableEvtsEch.id}"/>); return false;">&nbsp;</a>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvtsEch.id != null}">
					<unireg:consulterLog entityNature="EvenementEch" entityId="${tableEvtsEch.id}"/>
				</c:if>
		</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
<script language="JavaScript">

	EvtCivil.list = [
		<c:forEach var="evt" items="${listEvenementsEch}" varStatus="evtStatus">
		${evt.id}
		<c:if test="${!evtStatus.last}">
		,
		</c:if>
		</c:forEach>
	]

</script>
<script language="javascript">
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
		if (!EvtCivil.list || EvtCivil.list.length == 0) return null;
		var evtPrecedant = null;
		var evtSuivant = null;
		var indexEvt = EvtCivil.list.indexOf(evtId);
		if (indexEvt == -1) {
			if (EvtCivil.nextId) {
				evtSuivant = EvtCivil.nextId;
			}
			if (!evtSuivant) {
				evtSuivant = EvtCivil.list[0];
			}
		}
		else {
			if (indexEvt > 0) {
				evtPrecedant = EvtCivil.list[indexEvt - 1];
			}
			if (indexEvt < EvtCivil.list.length - 1) {
				evtSuivant = EvtCivil.list[indexEvt + 1];
			}
		}
		EvtCivil.open_details(evtId, evtPrecedant, evtSuivant)
	}

	/*
	 Ce qui suit permet de rouvrir la fenêtre de détail de l'événement concerné par une action (recyclage, forçage, etc...) grâce
	 au paramètres passé lors de l'appel à l'action.
	 */
	<c:if test="${nextEvtId != null}">
	EvtCivil.nextId = ${nextEvtId};
	</c:if>
	<c:if test="${selectedEvtId != null}">
	open_details(${selectedEvtId});
	</c:if>
</script>