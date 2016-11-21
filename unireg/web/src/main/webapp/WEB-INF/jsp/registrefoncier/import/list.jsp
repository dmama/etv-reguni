<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style>
			.checkboxes {
				margin: 5px;
				display: block;
			}
			.checkboxes input {
				margin-left: 10px;
				margin-right: 2px;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.suivi.imports.registre.foncier" />
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<form:form method="get" commandName="view" id="formSuiviImportRF" action="list.do">

			<script type="text/javascript">
				function submitForm(){
					$('#formSuiviImportRF').submit();
				}
			</script>

			<fieldset>
			<legend><span><fmt:message key="title.suivi.imports.criteria"/></span></legend>
				<span class="checkboxes">
			        Etats : <form:checkbox path="aTraiter" id="aTraiter" label="A traiter" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="traite" id="traite" label="Traité" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="enErreur" id="enErreur" label="En erreur" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="force" id="force" label="Forcé" onchange="submitForm()" onclick="submitForm()" />
			    </span>
			</fieldset>

			<%--@elvariable id="importEvent" type="ch.vd.uniregctb.registrefoncier.EvenementRFImportView"--%>
			<%--@elvariable id="count" type="java.lang.Long"--%>
			<display:table name="list" id="importEvent" class="display_table" pagesize="10" size="${count}" sort="external" partialList="true"
			               requestURI="/registrefoncier/import/list.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.import.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.un.import.trouve"/></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>

				<display:column titleKey="label.id.import" >
					${importEvent.id}
				</display:column>
				<display:column titleKey="label.date.valeur.import" >
					<unireg:regdate regdate="${importEvent.dateEvenement}"/>
				</display:column>
				<display:column titleKey="label.etat.import" >
					<fmt:message key="option.rf.etat.evenement.${importEvent.etat}" />
				</display:column>
				<display:column titleKey="label.message.erreur.import" >
					${importEvent.errorMessage}
				</display:column>
				<display:column style="action">
					<c:if test="${importEvent.etat == 'EN_ERREUR'}">
						<unireg:buttonTo name="Relancer" confirm="Voulez-vous vraiment relancer le traitement de l'import n°${importEvent.id} ?"
						                 action="/registrefoncier/import/restart.do" params="{id:${importEvent.id}}"/>
						<unireg:buttonTo name="Forcer" confirm="Voulez-vous vraiment forcer le traitement de l'import n°${importEvent.id} ? \n\nEn forçant cet import, les données non-traitées seront marquées comme 'forcées' et ne seront jamais intégrées dans Unireg."
						                 action="/registrefoncier/import/forcer.do" params="{id:${importEvent.id}}"/>
					</c:if>
					<unireg:consulterLog entityNature="EvenementRFImport" entityId="${importEvent.id}"/>
				</display:column>
			</display:table>

		</form:form>
	</tiles:put>
</tiles:insert>
