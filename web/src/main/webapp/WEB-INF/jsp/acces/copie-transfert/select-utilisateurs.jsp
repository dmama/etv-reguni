<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.copie.transfert.recherche.utilisateurs" /></tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-copier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formSelectUtilisateur"  name="theForm">

		<fieldset class="information">		
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.utilisateur.reference" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateursReference">
							<form:input path="utilisateurReference" id="utilisateurReference" />
							<span class="mandatory">*</span>
							<form:errors path="utilisateurReference" cssClass="error"/>
							<form:hidden path="visaUtilisateurReference" id="visaUtilisateurReference"  />
							<script>
								$(function() {
									Autocomplete.security('user', '#utilisateurReference', false, function(item) {
										if (item) {
											$('#visaUtilisateurReference').val(item.id1); // le visa de l'opérateur
										}
										else {
											$('#utilisateurReference').val(null);
											$('#visaUtilisateurReference').val(null);
										}
									});
								});
							</script>
						</div>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.utilisateur.destination" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateursDestination">
							<form:input path="utilisateurDestination" id="utilisateurDestination" />
							<span class="mandatory">*</span>
							<form:errors path="utilisateurDestination" cssClass="error"/>
							<form:hidden path="visaUtilisateurDestination" id="visaUtilisateurDestination"  />
							<script>
								$(function() {
									Autocomplete.security('user', '#utilisateurDestination', false, function(item) {
										if (item) {
											$('#visaUtilisateurDestination').val(item.id1); // le visa de l'opérateur
										}
										else {
											$('#utilisateurDestination').val(null);
											$('#visaUtilisateurDestination').val(null);
										}
									});
								});
							</script>
						</div>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
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