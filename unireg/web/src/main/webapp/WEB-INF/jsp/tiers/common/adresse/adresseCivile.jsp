<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="membre" value="${param.membre}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
	<c:choose>
		<c:when test="${membre == 'principal'}">
			<c:if test="${command.exceptionAdresseCiviles == null}">
				<display:table name="${command.historiqueAdressesCiviles}" id="adresseCivile" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableAdresseCivileDecorator">
					<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="usage">
						<fmt:message key="option.usage.civil.${adresseCivile.usageCivil}" />
					</display:column>
					<display:column sortable ="false" titleKey="label.provenance">
						<unireg:localisation localisation="${adresseCivile.localisationPrecedente}" showVD="false"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
						<unireg:regdate regdate="${adresseCivile.dateDebut}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
						<unireg:regdate regdate="${adresseCivile.dateFin}"/>
					</display:column>
					<display:column sortable ="false" titleKey="label.destination">
						<unireg:localisation localisation="${adresseCivile.localisationSuivante}" showVD="false"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement" property="complements"/>
					<display:column sortable ="true" titleKey="label.rueCasePostale" property="rue"/>
					<display:column sortable ="true" titleKey="label.localite" property="localite"/>
					<display:column sortable ="true" titleKey="label.pays">
						<c:if test="${adresseCivile.paysOFS != null }">
							<unireg:infra entityId="${adresseCivile.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
			<c:if test="${command.exceptionAdresseCiviles != null}">
				<p><fmt:message key="error.generic.adresse.civile"/><span class="error">&nbsp;<c:out value="${command.exceptionAdresseCiviles}"/></span></p>
			</c:if>

		</c:when>
		<c:when test="${membre == 'conjoint'}">
			<c:if test="${command.exceptionAdresseCivilesConjoint == null}">
				<display:table name="${command.historiqueAdressesCivilesConjoint}" id="adresseCivileConjoint" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableAdresseCivileDecorator">
					<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="usage">
						<fmt:message key="option.usage.civil.${adresseCivileConjoint.usageCivil}" />
					</display:column>
					<display:column sortable ="false" titleKey="label.provenance">
						<unireg:localisation localisation="${adresseCivileConjoint.localisationPrecedente}" showVD="false"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
						<unireg:regdate regdate="${adresseCivileConjoint.dateDebut}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
						<unireg:regdate regdate="${adresseCivileConjoint.dateFin}"/>
					</display:column>
					<display:column sortable ="false" titleKey="label.destination">
						<unireg:localisation localisation="${adresseCivileConjoint.localisationSuivante}" showVD="false"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement" property="complements"/>
					<display:column sortable ="true" titleKey="label.rueCasePostale" property="rue"/>
					<display:column sortable ="true" titleKey="label.localite" property="localite"/>
					<display:column sortable ="true" titleKey="label.pays">
						<c:if test="${adresseCivileConjoint.paysOFS != null }">
							<unireg:infra entityId="${adresseCivileConjoint.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
			<c:if test="${command.exceptionAdresseCivilesConjoint != null}">
				<p><fmt:message key="error.generic.adresse.civile"/><span class="error">&nbsp;<c:out value="${command.exceptionAdresseCivilesConjoint}"/></span></p>
			</c:if>
		</c:when>
	</c:choose>

	<script>
		$(function() {
			Tooltips.activate_static_tooltips($('#adresseCivile, #adresseCivileConjoint'));
		});
	</script>
</c:if>
