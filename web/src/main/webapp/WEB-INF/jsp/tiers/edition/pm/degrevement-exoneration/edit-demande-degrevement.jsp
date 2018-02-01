<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="immeuble" type="ch.vd.unireg.registrefoncier.allegement.ResumeImmeubleView"--%>
<%--@elvariable id="editDemandeDegrevementCommand" type="ch.vd.unireg.registrefoncier.allegement.EditDemandeDegrevementView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.demande.degrevement.ici.edition"/>
	</tiles:put>
	<tiles:put name="body">
		<table border="0">
			<tr style="vertical-align: top;">
				<td style="width: 50%; padding-right: 10px;">
					<unireg:bandeauTiers numero="${idContribuable}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>
				</td>
				<td>
					<jsp:include page="../../../common/degrevement-exoneration/resume-immeuble-fieldset.jsp"/>
				</td>
			</tr>
		</table>

		<form:form commandName="editDemandeDegrevementCommand" action="edit-demande-degrevement.do" id="editDemandeDegrevementForm">

			<form:hidden path="idDemandeDegrevement"/>

			<fieldset>
				<legend><span><fmt:message key="title.demande.degrevement.ici.edition"/></span></legend>
				<jsp:include page="../../../common/degrevement-exoneration/edit-demande-degrevement-table.jsp">
					<jsp:param name="commandName" value="editDemandeDegrevementCommand"/>
					<jsp:param name="allowPeriodeEdit" value="false"/>
					<jsp:param name="allowDelaiEdit" value="false"/>
					<jsp:param name="allowRetourEdit" value="false"/>
				</jsp:include>
			</fieldset>

			<!-- Debut Delais -->
			<jsp:include page="delai/lister.jsp"/>
			<!-- Fin Delais -->

			<!-- Debut Etats -->
			<jsp:include page="etat/lister.jsp"/>
			<!-- Fin Etats -->

			<!-- Debut Bouton -->
			<div style="margin-top:1em;">
				<unireg:buttonTo name="Retour" action="/degrevement-exoneration/edit-demandes-degrevement.do" params="{idContribuable:${idContribuable},idImmeuble:${immeuble.idImmeuble}}" method="GET"/>
				<!-- Duplicata demande de dégrèvement -->
				<unireg:buttonTo name="Imprimer duplicata" action="/degrevement-exoneration/duplicata.do" id="bouton_duplicata" method="post" params="{id:${editDemandeDegrevementCommand.idDemandeDegrevement}}"
				                 confirm="Voulez-vous imprimer un duplicata pour la demande de dégrèvement (impression locale)?"/>
				<!-- Annulation demande de dégrèvement -->
				<unireg:buttonTo name="Annuler document fiscal" confirm="Voulez-vous vraiment annuler cette demande de dégrèvement?"
				                 action="/degrevement-exoneration/annuler.do" method="post" params='{id:${editDemandeDegrevementCommand.idDemandeDegrevement}}'/>
			</div>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>
