<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.annulation.deces.view.AnnulationDecesRecapView"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  	<c:choose>
  			<c:when test="${command.marieSeulAndVeuf}">
				<fmt:message key="title.recapitulatif.annulation.veuvage.marie.seul"/>
  			</c:when>
  			<c:otherwise>
  				<fmt:message key="title.recapitulatif.annulation.deces" />
  			</c:otherwise>

  	</c:choose>
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/annulation-deces.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapAnnulationDeces"  name="formRecapAnnulationDeces">
			<form:hidden path="personne.numero" />
		    <unireg:bandeauTiers numero="${command.personne.numero}" titre="label.caracteristiques.personne" cssClass="information"
		                         showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
			<fieldset class="information">
				<legend><span>
			<c:choose>
				<c:when test="${command.marieSeulAndVeuf}">
					<fmt:message key="title.caracteristiques.annulation.veuvage.marie.seul" />
				</c:when>
				<c:otherwise>
					<fmt:message key="title.caracteristiques.annulation.deces" />
				</c:otherwise>
		  	</c:choose>

				</span></legend>
				<table>
					 <c:choose>
						<c:when test="${command.marieSeulAndVeuf}">
							<tr class="<unireg:nextRowClass/>" >
								<td width="25%"><fmt:message key="label.date.veuvage" />&nbsp;:</td>
								<td width="75%"><unireg:regdate regdate="${command.dateVeuvage}" /></td>
							</tr>
						</c:when>
						<c:when test="${command.warningDateDecesModifie}">
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><em><fmt:message key="label.validation.warning" />&nbsp;:</em></td>
								<td width="75%"><em><fmt:message key="label.avertissement.date.deces.modifiee.1" /></em></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.avertissement.date.deces.modifiee.2" />:&nbsp;</td>
								<td width="75%"><unireg:regdate regdate="${command.dateDeces}" /></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.avertissement.date.deces.modifiee.3" />:&nbsp;</td>
								<td width="75%"><unireg:regdate regdate="${command.dateDecesDepuisDernierForPrincipal}" /></td>
							</tr>
						</c:when>
						<c:otherwise>
						<tr class="<unireg:nextRowClass/>">
							<td width="25%"><fmt:message key="label.date.deces" />&nbsp;:</td>
							<td width="75%"><unireg:regdate regdate="${command.dateDeces}" /></td>
						</tr>
						</c:otherwise>
					</c:choose>
				</table>
			</fieldset>
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<c:choose>
				<c:when test="${command.marieSeulAndVeuf}">
					<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return Page_sauverAnnulDeces(event || window.event,false);" />
				</c:when>
				<c:otherwise>
					<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return Page_sauverAnnulDeces(event || window.event,true);" />
				</c:otherwise>
			</c:choose>

			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_sauverAnnulDeces(event,isDeces) {
				var message;
				if(isDeces){
				   message ='Voulez-vous vraiment annuler le décès de cette personne ?';
				}
				else{
				   message ='Voulez-vous vraiment annuler le veuvage de cette personne ?';
				}
				if(!confirm(message)) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>