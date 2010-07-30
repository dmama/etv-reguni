<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.droits.acces.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-dossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditRestriction"  name="theForm">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/pp.jsp">
			<jsp:param name="page" value="acces" />
			<jsp:param name="path" value="dossier" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste des restrictions -->
		<fieldset>
		<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
			<table border="0">
			<tr>
				<td>
					<a href="edit-acces-pp.do?numero=${command.dossier.numero}&height=120&width=650&index=&TB_iframe=true&modal=true" 
					class="add thickbox" title="Ajouter">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
				</td>
			</tr>
			</table>
		</authz:authorize>
		<c:if test="${not empty command.restrictions}">
			<display:table
					name="command.restrictions" id="restriction" pagesize="10" 
					requestURI="${url}"
					class="display">
			
				<display:column sortable ="true" titleKey="label.type.restriction">
					<c:if test="${restriction.annule}"><strike></c:if>
						<fmt:message key="option.type.droit.acces.${restriction.type}"  />
					<c:if test="${restriction.annule}"></strike></c:if>
				</display:column>
				
				<display:column sortable ="true" titleKey="label.visa.operateur" >
					<c:if test="${restriction.annule}"><strike></c:if>
						${restriction.visaOperateur}
					<c:if test="${restriction.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.prenom.nom">
					<c:if test="${restriction.annule}"><strike></c:if>
						<c:out value="${restriction.prenomNom}" />
					<c:if test="${restriction.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.office.impot">
					<c:if test="${restriction.annule}"><strike></c:if>
						<c:out value="${restriction.officeImpot}" />
					<c:if test="${restriction.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.lecture.seule">
					<input type="checkbox" name="lectureSeule" value="True"   
							<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column style="action">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=DroitAcces&id=${restriction.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
					<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
						<c:if test="${!restriction.annule}">
							<unireg:raccourciAnnuler onClick="javascript:Page_AnnulerRestriction(${restriction.id});"/>
						</c:if>
					</authz:authorize>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
				
			</display:table>
			
				<script type="text/javascript">
					function Page_AnnulerRestriction(id) {
							if(confirm('Voulez-vous vraiment annuler ce droit d\'acces ?')) {
								var form = F$("theForm");
								form.doPostBack("annulerRestriction", id);
						 	}
				 	} 	
				</script>
			
			</c:if>
			</fieldset>
		<!-- Fin Liste des restrictions -->
		<!-- Debut Bouton -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:document.location.href='list-pp.do';" />
		<!-- Fin Bouton -->
		&nbsp;
	</form:form>
	</tiles:put>
</tiles:insert>