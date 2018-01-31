<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="exonerations" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.ExonerationIFONCView>"--%>
<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.allegement.ResumeImmeubleView"--%>
<%--@elvariable id="droits" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.DroitView>"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.exonerations.ifonc.edition"/>
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

		<jsp:include page="../../../common/degrevement-exoneration/droits-fieldset.jsp"/>

		<fieldset>
			<legend><span><fmt:message key="label.exonerations.ifonc"/></span></legend>
			<table border="0">
				<tr>
					<td>
						<unireg:raccourciAjouter display="Ajouter" tooltip="Ajouter une nouvelle information d'exonération" link="add-exoneration.do?idContribuable=${idContribuable}&idImmeuble=${immeuble.idImmeuble}"/>
					</td>
				</tr>
			</table>
			<jsp:include page="../../../common/degrevement-exoneration/exonerations-table.jsp">
				<jsp:param name="mode" value="edit"/>
			</jsp:include>
		</fieldset>

		<!-- Debut Bouton -->
		<unireg:buttonTo name="Retour" action="/tiers/visu.do" params="{id:${idContribuable}}" method="GET"/>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>
