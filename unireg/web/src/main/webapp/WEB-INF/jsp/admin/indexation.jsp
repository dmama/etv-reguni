<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="label.gestion.indexation" /></tiles:put>
  	
  	<tiles:put name="body">
  	<form:form method="post" id="formGestionIndexation" name="formGestionIndexation">

		<fieldset>
			<legend><span><fmt:message key="label.gestion.performance" /></span></legend>
			<table>
		    	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.gestion.performance.actif" />&nbsp;:</td>
					<td width="50%"><form:checkbox path="gestionPerfActif" onclick="submitIndex('performance');" /></td>
				</tr>
			</table>
		</fieldset>

		<fieldset>
			<legend><span><fmt:message key="label.force.reindexation" /></span></legend>
			<table>
		    	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td><fmt:message key="label.numero.tiers"/>&nbsp;:</td>
					<td>
						<form:input path="id" size ="15" cssErrorClass="input-with-errors"/>
						<form:errors path="id" cssClass="error"/>
					</td>
					<td>
						<input type="button" value="<fmt:message key="label.bouton.forcer.reindexation"/>" onclick="submitIndex('reindexTiers');"/>
					</td>
				</tr>
			</table>
		</fieldset>

		<fieldset>
			<legend><span><fmt:message key="label.effacement.index" /></span></legend>
			<table>
		    	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.chemin.index" />&nbsp;:</td>
					<td width="50%">${command.chemin}</td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.nombre.documents.indexes" />&nbsp;:</td>
					<td width="50%">${command.nombreDocumentsIndexes}</td>
				</tr>
			</table>
		</fieldset>
	
		<fieldset>
			<legend><span><fmt:message key="label.gestion.indexation" /></span></legend>
		    <c:set var="ligneTableau" value="${1}" scope="request" />

		    <table>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.requete.lucene" />&nbsp;:</td>
					<td>
						<form:input path="requete" id="requete"  size ="65" cssErrorClass="input-with-errors"/>
						<form:errors path="requete" cssClass="error"/>
					</td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td colspan ="2">
						<input type="button" value="<fmt:message key="label.bouton.retour"/>" onclick="document.location='index.do';"/>
						<input type="button" value="<fmt:message key="label.bouton.rechercher"/>" onclick="submitIndex('search');"/>
					</td>
				</tr>
			</table>	

			<display:table 	name="index" id="index" pagesize="30"
							defaultsort="1" requestURI="/admin/indexation.do" sort="list">
				<display:column property="entityId" titleKey="label.index.entityId" />
				<display:column property="nomCourrier1" titleKey="label.index.nomCourrier1" />
				<display:column property="nomCourrier2" titleKey="label.index.nomCourrier2" />
				<display:column property="nomFor" titleKey="label.index.nomFor" />
				<display:column property="npa" titleKey="label.index.npa" />
				<display:column property="localite" titleKey="label.index.localite" />
				<display:column property="dateNaissance" titleKey="label.index.dateNaissance" />
				<display:column property="numeroAvs" titleKey="label.index.numeroAvs" />
			</display:table>
			
		</fieldset>
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
		/**
		* Submit du formulaire de gestion de l'indexation
		*/
		function submitIndex(action) {
			var formGestionIndexation = F$('formGestionIndexation');
			if (action == 'clean' || action == 'hostClean') {
				if(confirm('Voulez-vous vraiment supprimer cet index ?')) {
					formGestionIndexation.action = 'indexation.do?action=' + action;
					formGestionIndexation.submit();
				}
			} else {
				formGestionIndexation.action = 'indexation.do?action=' + action;
				formGestionIndexation.submit();
			}
		}
	</script>
  	</tiles:put>
</tiles:insert>
