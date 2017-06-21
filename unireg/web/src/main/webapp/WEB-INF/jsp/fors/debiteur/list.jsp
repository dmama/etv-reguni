<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.fors.debiteur" /></tiles:put>

	<tiles:put name="body">
		<form:form method="post" id="formEditTiers" name="theForm">

			<%--@elvariable id="id" type="java.lang.Long"--%>
			<unireg:bandeauTiers numero="${id}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="true"/>

			<unireg:setAuth var="autorisations" tiersId="${id}"/>
			<c:if test="${autorisations.donneesFiscales}">
				<fieldset>
					<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>

					<authz:authorize ifAnyGranted="ROLE_CREATE_DPI,ROLE_MODIF_FISCAL_DPI">
						<table border="0">
							<tr class="<unireg:nextRowClass/>" >
								<td>
									<unireg:linkTo name="Ajouter" title="Ajouter for" action="/fors/debiteur/add.do" params="{tiersId:${id}}" link_class="add"/>
								</td>
							</tr>
						</table>
					</authz:authorize>

					<display:table name="fors" id="forFiscal" pagesize="10" requestURI="list.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

						<%-- Commune du for --%>
						<display:column sortable="true" titleKey="label.commune">
							<c:choose>
								<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
									<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
								</c:when>
								<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
									<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
								</c:when>
							</c:choose>
						</display:column>

						<%-- Date d'ouverture --%>
						<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
							<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
						</display:column>

						<%-- Motif d'ouverture --%>
						<display:column sortable ="true" titleKey="label.motif.ouv">
							<c:if test="${forFiscal.motifOuverture != null}">
								<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
							</c:if>
						</display:column>

						<%-- Date de fermeture --%>
						<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
							<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
							<authz:authorize ifAnyGranted="ROLE_CREATE_DPI,ROLE_MODIF_FISCAL_DPI">
								<c:if test="${forFiscal.dateFermeture != null && forFiscal.dernierForPrincipalOuDebiteur}">
									<unireg:linkTo name="" action="/fors/debiteur/reopen.do" method="POST" params="{forId:${forFiscal.id}}" link_class="reOpen"
									               title="Ré-ouvrir le for" confirm="Voulez-vous vraiment ré-ouvrir ce for fiscal ?" />
								</c:if>
							</authz:authorize>
						</display:column>

						<%-- Motif de fermeture --%>
						<display:column sortable ="true" titleKey="label.motif.fer">
							<c:if test="${forFiscal.motifFermeture != null}">
								<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
							</c:if>
						</display:column>

						<%-- Actions --%>
						<display:column style="action">
							<authz:authorize ifAnyGranted="ROLE_CREATE_DPI,ROLE_MODIF_FISCAL_DPI">
								<c:if test="${!forFiscal.annule}">
									<c:if test="${forFiscal.dateFermeture == null}">
										<unireg:linkTo name="" action="/fors/debiteur/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
									</c:if>
									<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
										<unireg:linkTo name="" action="/fors/debiteur/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
										               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
									</c:if>
								</c:if>
							</authz:authorize>
						</display:column>

						<display:setProperty name="paging.banner.all_items_found" value=""/>
						<display:setProperty name="paging.banner.one_item_found" value=""/>
					</display:table>

				</fieldset>
			</c:if>

			<c:choose>
				<c:when test="${id != null}">
					<unireg:RetourButton link="../../tiers/visu.do?id=${id}" checkIfModified="true"/>
				</c:when>
				<c:otherwise>
					<unireg:RetourButton link="../../tiers/list.do" checkIfModified="true"/>
				</c:otherwise>
			</c:choose>

		</form:form>
	</tiles:put>
</tiles:insert>
