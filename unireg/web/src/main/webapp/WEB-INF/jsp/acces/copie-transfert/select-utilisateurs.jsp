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
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.utilisateur.reference" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateurs">
							<form:input path="utilisateurReference" id="utilisateurReference" />
							<form:errors path="utilisateurReference" cssClass="error"/>
							<form:hidden path="numeroUtilisateurReference" id="numeroUtilisateurReference"  />
							<script>
								$(function() {
									autocomplete_security('user', '#utilisateurReference', function(item) {
										if (item) {
											$('#numeroUtilisateurReference').val(item.id2); // le numéro technique
										}
										else {
											$('#utilisateurReference').val(null);
											$('#numeroUtilisateurReference').val(null);
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
						<div id="utilisateurs">
							<form:input path="utilisateurDestination" id="utilisateurDestination" />
							<form:errors path="utilisateurDestination" cssClass="error"/>
							<form:hidden path="numeroUtilisateurDestination" id="numeroUtilisateurDestination"  />
							<script>
								$(function() {
									autocomplete_security('user', '#utilisateurDestination', function(item) {
										if (item) {
											$('#numeroUtilisateurDestination').val(item.id2); // le numéro technique
										}
										else {
											$('#utilisateurDestination').val(null);
											$('#numeroUtilisateurDestination').val(null);
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