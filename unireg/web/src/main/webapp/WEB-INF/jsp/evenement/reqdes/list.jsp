<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.unites.traitement.reqdes" /></tiles:put>

  	    <tiles:put name="head">
            <script type="text/javascript">
                 $(document).ready(function () {

                     $('#rechercher').click(function () {
	                     $('#formRechercheUnitesTraitement').attr('action', 'rechercher.do');
                     });

                     $('#effacer').click( function () {
                     	window.location.href = 'effacer.do';
                     	return false;
                     });

                 });
            </script>
        </tiles:put>

  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheUnitesTraitement" commandName="reqdesCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table name="listUnites" id="tableUnitesTraitement" pagesize="25" requestURI="/evenement/reqdes/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="nbUnites">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="true" titleKey="label.reqdes.unite.traitement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />

			<!-- Etat -->
			<display:column titleKey="label.reqdes.unite.traitement.etat" sortable="true" sortName="etat">
				<fmt:message key="option.etat.traitement.reqdes.${tableUnitesTraitement.etat}"/>
			</display:column>

			<!-- Noms des parties prenantes -->
			<display:column titleKey="label.reqdes.unite.traitement.parties.prenantes" sortable="false">
				<c:if test="${tableUnitesTraitement.partiePrenante1 != null}">
					<c:out value="${tableUnitesTraitement.partiePrenante1.nomPrenom}"/>
					<c:if test="${tableUnitesTraitement.partiePrenante2 != null}">
						<br/><c:out value="${tableUnitesTraitement.partiePrenante2.nomPrenom}"/>
					</c:if>
				</c:if>
			</display:column>

			<!-- Date acte -->
			<display:column titleKey="label.reqdes.date.acte" sortable="true" sortName="evenement.dateActe">
				<unireg:regdate regdate="${tableUnitesTraitement.dateActe}"/>
			</display:column>

			<!-- Numéro de minute -->
			<display:column titleKey="label.reqdes.numero.minute" sortable="true" sortName="evenement.numeroMinute">
				<c:out value="${tableUnitesTraitement.numeroMinute}"/>
			</display:column>

			<!-- Notaire -->
			<display:column titleKey="label.reqdes.notaire" sortable="true" sortName="evenement.notaire.visa">
				<c:out value="${tableUnitesTraitement.notaire.nomPrenom} (${tableUnitesTraitement.visaNotaire})"/>
			</display:column>

			<!-- Logs -->
			<display:column style="action">
				<unireg:consulterLog entityNature="UniteTraitementReqDes" entityId="${tableUnitesTraitement.id}"/>
			</display:column>

		</display:table>

	</tiles:put>
</tiles:insert>
