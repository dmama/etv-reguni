<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="label.recapitulatif.fusion" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/fusion.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
	  	<form:form method="post" id="formRecapFusion">
		<fieldset>
			<legend><span><fmt:message key="label.recapitulatif.fusion" /></span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%">&nbsp;</td>
					<td width="40%">
						<fmt:message key="title.inconnu.controle.habitants" />
					</td>
					<td width="40%">
						<fmt:message key="title.reference.controle.habitants" />
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
					<td width="40%">
						<unireg:numCTB numero="${command.nonHabitant.numero}"></unireg:numCTB>
					</td>
					<td width="40%">
						<unireg:numCTB numero="${command.habitant.numero}"></unireg:numCTB>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.adresse" />&nbsp;:</td>
					<td width="40%">
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne1 != null}">${command.nonHabitant.adresseEnvoi.ligne1}</c:if>
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne2 != null}"><br />${command.nonHabitant.adresseEnvoi.ligne2}</c:if>
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne3 != null}"><br />${command.nonHabitant.adresseEnvoi.ligne3}</c:if>
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne4 != null}"><br />${command.nonHabitant.adresseEnvoi.ligne4}</c:if>
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne5 != null}"><br />${command.nonHabitant.adresseEnvoi.ligne5}</c:if>
						<c:if test="${command.nonHabitant.adresseEnvoi.ligne6 != null}"><br />${command.nonHabitant.adresseEnvoi.ligne6}</c:if>
					</td>
					<td width="40%">
						<c:if test="${command.habitant.adresseEnvoi.ligne1 != null}">${command.habitant.adresseEnvoi.ligne1}</c:if>
						<c:if test="${command.habitant.adresseEnvoi.ligne2 != null}"><br />${command.habitant.adresseEnvoi.ligne2}</c:if>
						<c:if test="${command.habitant.adresseEnvoi.ligne3 != null}"><br />${command.habitant.adresseEnvoi.ligne3}</c:if>
						<c:if test="${command.habitant.adresseEnvoi.ligne4 != null}"><br />${command.habitant.adresseEnvoi.ligne4}</c:if>
						<c:if test="${command.habitant.adresseEnvoi.ligne5 != null}"><br />${command.habitant.adresseEnvoi.ligne5}</c:if>
						<c:if test="${command.habitant.adresseEnvoi.ligne6 != null}"><br />${command.habitant.adresseEnvoi.ligne6}</c:if>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.date.naissance" />&nbsp;:</td>
					<td width="40%">
						<unireg:date date="${command.nonHabitant.dateNaissance}"></unireg:date>
					</td>
					<td width="40%">
						<unireg:date date="${command.habitant.dateNaissance}"></unireg:date>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
					<td width="40%">
						<unireg:numAVS numeroAssureSocial="${command.nonHabitant.numeroAssureSocial}"></unireg:numAVS>	
					</td>
					<td width="40%">
						<unireg:numAVS numeroAssureSocial="${command.habitant.numeroAssureSocial}"></unireg:numAVS>	
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
					<td width="40%">
						<unireg:ancienNumeroAVS ancienNumeroAVS="${command.nonHabitant.ancienNumeroAVS}"></unireg:ancienNumeroAVS>	
					</td>
					<td width="40%">
						<unireg:ancienNumeroAVS ancienNumeroAVS="${command.habitant.ancienNumeroAVS}"></unireg:ancienNumeroAVS>	
					</td>
				</tr>
			</table>
		</fieldset>
		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:retourRecapFusion(${command.nonHabitant.numero});" />
		<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverFusion(event || window.event);" />	
		<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript1.3">
			function Page_retourRecapFusion(numeroNonHab) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='list-habitant.do?numeroNonHab=' + numeroNonHab ;
				}
			}
			function Page_sauverFusion(event) {
				if(!confirm('Voulez-vous vraiment fusionner ces deux personnes ?')) {
					return Event.stop(event);
			 	}
			 	return true;
			}
		</script>
	</tiles:put>
</tiles:insert>
<!-- Fin Caracteristiques non habitant -->