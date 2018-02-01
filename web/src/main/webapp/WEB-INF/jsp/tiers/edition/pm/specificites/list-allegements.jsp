<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.allegements.fiscaux"/>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<fieldset>
			<legend><span><fmt:message key="label.allegements.fiscaux"/></span></legend>

			<table border="0">
				<tr><td>
					<unireg:linkTo name="Ajouter" action="/allegement/add.do" method="get" params="{pmId:${pmId}}" title="Ajouter un allègement fiscal" link_class="add noprint"/>
				</td></tr>
			</table>

			<c:if test="${not empty allegements}">
				<display:table name="allegements" id="af" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" requestURI="/allegement/edit-list.do" sort="list">
					<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
						<unireg:regdate regdate="${af.dateDebut}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
						<unireg:regdate regdate="${af.dateFin}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.type.impot">
						<fmt:message key="option.allegement.fiscal.type.impot.${af.typeImpot}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.type.collectivite">
						<fmt:message key="option.allegement.fiscal.type.collectivite.${af.typeCollectivite}"/>
						<c:if test="${af.typeCollectivite == 'COMMUNE' && af.noOfsCommune != null}">
							&nbsp;(<unireg:commune ofs="${af.noOfsCommune}" displayProperty="nomOfficiel" titleProperty="noOFS"/>)
						</c:if>
					</display:column>
					<display:column titleKey="label.type">
						<c:choose>
							<c:when test="${af.typeCollectivite == 'COMMUNE' || af.typeCollectivite == 'CANTON'}">
								<c:if test="${af.typeICC != null}">
									<fmt:message key="option.allegement.icc.type.${af.typeICC}"/>
								</c:if>
							</c:when>
							<c:when test="${af.typeCollectivite == 'CONFEDERATION'}">
								<c:if test="${af.typeIFD != null}">
									<fmt:message key="option.allegement.ifd.type.${af.typeIFD}"/>
								</c:if>
							</c:when>
							<c:otherwise>
								&nbsp;
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column sortable="true" titleKey="label.allegements.fiscaux.pourcentage" sortProperty="pourcentage">
						<c:choose>
							<c:when test="${af.pourcentage != null}">
								<fmt:formatNumber maxIntegerDigits="3" minIntegerDigits="1" maxFractionDigits="2" minFractionDigits="0" value="${af.pourcentage}"/>&nbsp;%
							</c:when>
							<c:otherwise>
								<span>Allègement en montant</span>
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column class="action" style="width: 10%;">
						<c:if test="${!af.annule}">
							<c:if test="${af.dateFin == null}">
								<unireg:linkTo name="" action="/allegement/edit.do" method="GET" params="{afId:${af.id}}" link_class="edit" title="Fermeture d'allègement fiscal" />
							</c:if>
							<unireg:linkTo name="" action="/allegement/cancel.do" method="POST" params="{afId:${af.id}}" link_class="delete"
							               title="Annulation de régime fiscal" confirm="Voulez-vous vraiment annuler cet allègement fiscal ?"/>
						</c:if>
					</display:column>
				</display:table>
			</c:if>

		</fieldset>

		<!-- Scripts -->

		<!-- Debut Bouton -->
		<table>
			<tr>
				<td>
					<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${pmId}}"/>
				</td>
			</tr>
		</table>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>