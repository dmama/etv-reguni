<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.copie.transfert.recherche.utilisateurs" /></tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-copier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formSelectUtilisateur"  name="theForm">

		<fieldset class="information">		
			<table>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.utilisateur.reference" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateurs">
							<form:input path="utilisateurReference" id="utilisateurReference" />
							<form:errors path="utilisateurReference" cssClass="error"/>
							<form:hidden path="numeroUtilisateurReference" id="numeroUtilisateurReference"  />
							<script type="text/javascript">
									function utilisateurReference_onChange(row) {	
										var form = document.forms["formSelectUtilisateur"];
										form.numeroUtilisateurReference.value = ( row ? row.individuNoTechnique : "");
									}
							</script>
							<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
								<jsp:param name="inputId" value="utilisateurReference" />
								<jsp:param name="dataValueField" value="visaOperateur" />
								<jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
								<jsp:param name="dataSource" value="selectionnerUtilisateur" />
								<jsp:param name="onChange" value="utilisateurReference_onChange" />
								<jsp:param name="autoSynchrone" value="false"/>
							</jsp:include>
						</div>
					</td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.utilisateur.destination" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateurs">
							<form:input path="utilisateurDestination" id="utilisateurDestination" />
							<form:errors path="utilisateurDestination" cssClass="error"/>
							<form:hidden path="numeroUtilisateurDestination" id="numeroUtilisateurDestination"  />
							<script type="text/javascript">
									function utilisateurDestination_onChange(row) {	
										var form = document.forms["formSelectUtilisateur"];
										form.numeroUtilisateurDestination.value = ( row ? row.individuNoTechnique : "");
									}
							</script>
							<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
								<jsp:param name="inputId" value="utilisateurDestination" />
								<jsp:param name="dataValueField" value="visaOperateur" />
								<jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
								<jsp:param name="dataSource" value="selectionnerUtilisateur" />
								<jsp:param name="onChange" value="utilisateurDestination_onChange" />
								<jsp:param name="autoSynchrone" value="false"/>
							</jsp:include>
						</div>
					</td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.type.operation" />&nbsp;:</td>
					<td width="75%">
						<form:radiobuttons path="typeOperation" items="${typesOperation}" />
					</td>
				</tr>
			</table>
		</fieldset>
		<input type="submit" value="<fmt:message key="label.bouton.selectionner"/>" name="rechercher"/>
		
	</form:form>
	</tiles:put>
</tiles:insert>