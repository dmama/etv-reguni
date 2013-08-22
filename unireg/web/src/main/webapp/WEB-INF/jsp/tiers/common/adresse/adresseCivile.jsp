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
						<unireg:localisation localisation="${adresseCivile.localisationPrecedente}" showVD="true"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
						<unireg:regdate regdate="${adresseCivile.dateDebut}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
						<unireg:regdate regdate="${adresseCivile.dateFin}"/>
					</display:column>
					<display:column sortable ="false" titleKey="label.destination">
						<unireg:localisation localisation="${adresseCivile.localisationSuivante}" showVD="true"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement" property="complements"/>
					<display:column sortable ="true" titleKey="label.rueCasePostale">
						<c:choose>
							<c:when test="${adresseCivile.egid != null || adresseCivile.ewid != null}">
								<span id="prn-${adresseCivile.usageCivil}-<unireg:regdate regdate="${adresseCivile.dateDebut}" format="yyyyMMdd"/>-<unireg:regdate regdate="${adresseCivile.dateFin}" format="yyyyMMdd"/>" class="staticTip">
									<c:out value="${adresseCivile.rue}"/>
								</span>
								<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
									<div id="prn-${adresseCivile.usageCivil}-<unireg:regdate regdate="${adresseCivile.dateDebut}" format="yyyyMMdd"/>-<unireg:regdate regdate="${adresseCivile.dateFin}" format="yyyyMMdd"/>-tooltip" style="display: none;">
										<b>EGID&nbsp;</b>: <c:choose><c:when test="${adresseCivile.egid != null}"><c:out value="${adresseCivile.egid}"/></c:when><c:otherwise>-</c:otherwise></c:choose><br/>
										<b>EWID&nbsp;</b>: <c:choose><c:when test="${adresseCivile.ewid != null}"><c:out value="${adresseCivile.ewid}"/></c:when><c:otherwise>-</c:otherwise></c:choose><br/>
									</div>
								</authz:authorize>
							</c:when>
							<c:otherwise>
								<c:out value="${adresseCivile.rue}"/>
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column sortable ="true" titleKey="label.localite" property="localite"/>
					<display:column sortable ="true" titleKey="label.pays">
						<c:if test="${adresseCivile.paysOFS != null }">
							<unireg:pays ofs="${adresseCivile.paysOFS}" displayProperty="nomCourt" date="${adresseCivile.dateDebut}"/>
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
						<unireg:localisation localisation="${adresseCivileConjoint.localisationPrecedente}" showVD="true"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
						<unireg:regdate regdate="${adresseCivileConjoint.dateDebut}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
						<unireg:regdate regdate="${adresseCivileConjoint.dateFin}"/>
					</display:column>
					<display:column sortable ="false" titleKey="label.destination">
						<unireg:localisation localisation="${adresseCivileConjoint.localisationSuivante}" showVD="true"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement" property="complements"/>
					<display:column sortable ="true" titleKey="label.rueCasePostale">
						<c:choose>
							<c:when test="${adresseCivileConjoint.egid != null || adresseCivileConjoint.ewid != null}">
								<span id="cnj-${adresseCivileConjoint.usageCivil}-<unireg:regdate regdate="${adresseCivileConjoint.dateDebut}" format="yyyyMMdd"/>-<unireg:regdate regdate="${adresseCivileConjoint.dateFin}" format="yyyyMMdd"/>" class="staticTip">
									<c:out value="${adresseCivileConjoint.rue}"/>
								</span>
								<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
									<div id="cnj-${adresseCivileConjoint.usageCivil}-<unireg:regdate regdate="${adresseCivileConjoint.dateDebut}" format="yyyyMMdd"/>-<unireg:regdate regdate="${adresseCivileConjoint.dateFin}" format="yyyyMMdd"/>-tooltip" style="display: none;">
										<b>EGID&nbsp;</b>: <c:choose><c:when test="${adresseCivileConjoint.egid != null}"><c:out value="${adresseCivileConjoint.egid}"/></c:when><c:otherwise>-</c:otherwise></c:choose><br/>
										<b>EWID&nbsp;</b>: <c:choose><c:when test="${adresseCivileConjoint.ewid != null}"><c:out value="${adresseCivileConjoint.ewid}"/></c:when><c:otherwise>-</c:otherwise></c:choose><br/>
									</div>
								</authz:authorize>
							</c:when>
							<c:otherwise>
								<c:out value="${adresseCivileConjoint.rue}"/>
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column sortable ="true" titleKey="label.localite" property="localite"/>
					<display:column sortable ="true" titleKey="label.pays">
						<c:if test="${adresseCivileConjoint.paysOFS != null }">
							<unireg:pays ofs="${adresseCivileConjoint.paysOFS}" displayProperty="nomCourt" date="${adresseCivileConjoint.dateDebut}"/>
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
