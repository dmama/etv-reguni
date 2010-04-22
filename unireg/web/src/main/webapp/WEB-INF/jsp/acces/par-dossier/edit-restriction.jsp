<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-dossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form name="formAddRestriction" id="formAddRestriction">
	<fieldset class="information">
		
		<table>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.utilisateur" />&nbsp;:</td>
				<td width="25%">
					<div id="utilisateurs">
						<form:input path="utilisateur" id="utilisateur" />
						<form:errors path="utilisateur" cssClass="error"/>
						<form:hidden path="numeroUtilisateur" id="numeroUtilisateur"  />
						<script type="text/javascript">
								function utilisateur_onChange(row) {	
									var form = document.forms["formAddRestriction"];
									form.numeroUtilisateur.value = ( row ? row.individuNoTechnique : "");
								}
						</script>
						<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
							<jsp:param name="inputId" value="utilisateur" />
							<jsp:param name="dataValueField" value="visaOperateur" />
							<jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
							<jsp:param name="dataSource" value="selectionnerUtilisateur" />
							<jsp:param name="onChange" value="utilisateur_onChange" />
							<jsp:param name="autoSynchrone" value="false"/>
						</jsp:include>
					</div>
				</td>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.type.restriction" />&nbsp;:</td>
				<td width="25%">
					<form:select path="type" items="${typesDroitAcces}" />
				</td>
				<td width="25%"><fmt:message key="label.lecture.seule" />&nbsp;:</td>
				<td width="25%">
					<form:checkbox path="lectureSeule" />
				</td>
			</tr>
		</table>
		
	</fieldset>
	
	<table>
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<input type="button" id="ajouter" value="<fmt:message key="label.bouton.ajouter" />" onclick="javascript:ajouterRestriction();">
			</td>
			<td width="25%">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()">
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
		function ajouterRestriction() {
			var formAddRestriction = document.getElementById('formAddRestriction');
			formAddRestriction.submit(); 	
		}
		
	</script>
	</tiles:put>
</tiles:insert>

