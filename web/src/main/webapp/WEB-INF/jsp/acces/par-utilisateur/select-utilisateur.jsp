<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.acces.utilisateur.recherche.utilisateur" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formSelectUtilisateur"  name="theForm">

		<fieldset class="information">		
			<table>

				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.utilisateur" />&nbsp;:</td>
					<td width="75%">
						<div id="utilisateurs">
							<form:input path="utilisateur" id="utilisateur" />
							<span class="mandatory">*</span>
							<form:errors path="utilisateur" cssClass="error"/>
							<form:hidden path="visaOperateur" id="visaOperateur"  />
							<script>
								$(function() {
									Autocomplete.security('user', '#utilisateur', false, function(item) {
										if (item) {
											$('#visaOperateur').val(item.id1); // le visa de l'opérateur
										}
										else {
											$('#utilisateur').val(null);
											$('#visaOperateur').val(null);
										}
									});
								});
							</script>
						</div>
					</td>
				</tr>
			</table>
		</fieldset>
		<input type="submit" value="<fmt:message key="label.bouton.selectionner"/>" name="rechercher"/>
		
	</form:form>
	</tiles:put>
</tiles:insert>