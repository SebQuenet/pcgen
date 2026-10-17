<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:str="http://xsltsl.sourceforge.net/string.html"
	xmlns:xalan="http://xml.apache.org/xalan"
	>
	<xsl:import href="../../../../xsltsl-1.1/stdlib.xsl"/>
	<xsl:import href="../inc_pagedimensions.xslt"/>
	<!--
====================================
====================================
	TEMPLATE - DOMAINS
====================================
====================================-->
	<xsl:template match="domains">
		<!-- BEGIN Domains Table -->
		<xsl:call-template name="bold.list">
			<xsl:with-param name="attribute" select="'domains'" />
			<xsl:with-param name="title" select="key('label','in_sheet_domains')" />
			<xsl:with-param name="list" select="domain"/>
			<xsl:with-param name="name.tag" select="'name'"/>
			<xsl:with-param name="desc.tag" select="'power'"/>
		</xsl:call-template>
		<!-- END Domains Table -->
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - WEAPON PROFICIENCIES
====================================
====================================-->
	<xsl:template match="weapon_proficiencies">
		<!-- BEGIN weapon_proficiencies Table -->
		<xsl:call-template name="list">
			<xsl:with-param name="attribute" select="'proficiencies'"/>
			<xsl:with-param name="title" select="key('label','in_sheet_proficienciesPdf')"/>
			<xsl:with-param name="value" select="." />
		</xsl:call-template>
		<!-- END weapon_proficiencies Table -->
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - LANGUAGES
====================================
====================================-->
	<xsl:template match="languages">
		<!-- BEGIN Languages Table -->
		<xsl:call-template name="list">
			<xsl:with-param name="attribute" select="'languages'"/>
			<xsl:with-param name="title" select="key('label','in_sheet_languagesPdf')"/>
			<xsl:with-param name="value" select="." />
		</xsl:call-template>
		<!-- END Languages Table -->
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - TEMPLATES
====================================
====================================-->
	<xsl:template match="templates">
		<!-- BEGIN Templates Table -->
		<xsl:call-template name="stripped.list">
			<xsl:with-param name="attribute" select="'templates'" />
			<xsl:with-param name="title" select="key('label','in_sheet_templatesPdf')" />
			<xsl:with-param name="list" select="template"/>
			<xsl:with-param name="name.tag" select="'name'"/>
		</xsl:call-template>
		<!-- END Templates Table -->
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - PROHIBITED
====================================
====================================-->
	<xsl:template match="prohibited_schools">
		<xsl:if test=". != ''">
			<xsl:call-template name="list">
				<xsl:with-param name="attribute" select="'prohibited'"/>
				<xsl:with-param name="title" select="key('label','in_sheet_prohibitedPdf')"/>
				<xsl:with-param name="value" select="." />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - COMPANIONS
====================================
====================================-->
	<xsl:template match="companions">
		<!-- BEGIN Companions Table -->
		<xsl:apply-templates select="familiar"/>
		<xsl:apply-templates select="psicrystal"/>
		<xsl:apply-templates select="mount"/>
		<xsl:apply-templates select="companion"/>
		<xsl:call-template name="followers.list"/>
		<!-- END Companions Table -->
	</xsl:template>
	<xsl:template match="familiar">
		<!-- BEGIN Familiar Table -->
		<xsl:call-template name="show_companion">
			<xsl:with-param name="followerType" select="key('label','in_sheet_familiar')"/>
		</xsl:call-template>
		<!-- END Familiar Table -->
	</xsl:template>
	<xsl:template match="psicrystal">
		<!-- BEGIN Psicrystal Table -->
		<xsl:call-template name="show_companion">
			<xsl:with-param name="followerType" select="key('label','in_sheet_psicrystal')"/>
		</xsl:call-template>
		<!-- END Familiar Table -->
	</xsl:template>
	<xsl:template match="mount">
		<!-- BEGIN Familiar Table -->
		<xsl:call-template name="show_companion">
			<xsl:with-param name="followerType" select="key('label','in_sheet_specialMount')"/>
		</xsl:call-template>
		<!-- END Familiar Table -->
	</xsl:template>
	<xsl:template match="companion">
		<!-- BEGIN Familiar Table -->
		<xsl:call-template name="show_companion">
			<xsl:with-param name="followerType" select="key('label','in_sheet_animalCompanion')"/>
		</xsl:call-template>
		<!-- END Familiar Table -->
	</xsl:template>
	<xsl:template name="followers.list">
		<xsl:if test="count(follower) &gt; 0">
			<fo:table table-layout="fixed" width="100%" space-after.optimum="2mm">
				<fo:table-column>
					<xsl:attribute name="column-width"><xsl:value-of select="0.5 * ($pagePrintableWidth - 2)" />mm</xsl:attribute>
				</fo:table-column>
				<fo:table-body>
					<fo:table-row keep-with-next.within-column="always">
					<fo:table-cell>
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'companions.title'"/>
							</xsl:call-template>
							<fo:block font-size="10pt" font-weight="bold"><xsl:value-of select="key('label','in_sheet_followers')"/> </fo:block>
						</fo:table-cell>
					</fo:table-row>
					<fo:table-row keep-with-next.within-column="always">
						<fo:table-cell>
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'companions'"/>
							</xsl:call-template>
							<xsl:for-each select="follower">
								<fo:block font-size="8pt">
									<xsl:value-of select="name"/>
								</fo:block>
							</xsl:for-each>
						</fo:table-cell>
					</fo:table-row>
				</fo:table-body>
			</fo:table>
		</xsl:if>
	</xsl:template>
	<xsl:template name="show_companion">
		<xsl:param name="followerType" select="Follower"/>
		<fo:table table-layout="fixed" width="100%" space-before.optimum="2mm" keep-together="always">
				<fo:table-column>
					<xsl:attribute name="column-width"><xsl:value-of select="0.5 * ($pagePrintableWidth - 2) - 69" />mm</xsl:attribute>
				</fo:table-column>
			<fo:table-column column-width="15mm"/>
			<fo:table-column column-width="13mm"/>
			<fo:table-column column-width="14mm"/>
			<fo:table-column column-width="13mm"/>
			<fo:table-column column-width="14mm"/>
			<fo:table-body keep-together="always">
				<fo:table-row keep-with-next.within-column="always">
					<fo:table-cell number-columns-spanned="6">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="10pt" font-weight="bold">
							<xsl:value-of select="$followerType"/>: <xsl:value-of select="name"/> (<xsl:value-of select="race"/>)</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<fo:table-row keep-with-next.within-column="always">
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_hpPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="hp"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_acPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="ac"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_initPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="initiative_mod"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<fo:table-row keep-with-next.within-column="always">
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_fortPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="fortitude"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_refPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="reflex"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell text-align="end">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_willPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="8pt">
							<xsl:value-of select="will"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<xsl:for-each select="attacks/attack">
					<xsl:if test="string-length(common/name/long) &gt; 0">
						<fo:table-row keep-with-next.within-column="always">
							<fo:table-cell text-align="end">
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions.title'"/>
								</xsl:call-template>
								<fo:block font-size="8pt">
									<xsl:variable name="name" select="substring-before(common/name/long,'(')"/>
									<xsl:variable name="description" select="substring-after(common/name/long,'(')"/>
									<xsl:value-of select="$name"/>
									<xsl:if test="string-length($name) = 0">
										<xsl:value-of select="common/name/long"/>
									</xsl:if>
									<xsl:if test="string-length($description) &gt; 0">
										<fo:inline font-size="5pt">
											<xsl:text>(</xsl:text>
											<xsl:value-of select="$description"/>
										</fo:inline>
									</xsl:if>
								</fo:block>
							</fo:table-cell>
							<fo:table-cell>
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions'"/>
								</xsl:call-template>
								<fo:block font-size="8pt">
									<xsl:value-of select="simple/to_hit"/>
								</fo:block>
							</fo:table-cell>
							<fo:table-cell text-align="end">
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions.title'"/>
								</xsl:call-template>
								<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_damPdf')"/></fo:block>
							</fo:table-cell>
							<fo:table-cell>
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions'"/>
								</xsl:call-template>
								<fo:block font-size="8pt">
									<xsl:value-of select="simple/damage"/>
								</fo:block>
							</fo:table-cell>
							<fo:table-cell text-align="end">
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions.title'"/>
								</xsl:call-template>
								<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_crit')"/></fo:block>
							</fo:table-cell>
							<fo:table-cell>
								<xsl:call-template name="attrib">
									<xsl:with-param name="attribute" select="'companions'"/>
								</xsl:call-template>
								<fo:block font-size="8pt">
									<xsl:value-of select="common/critical/range"/>/x<xsl:value-of select="common/critical/multiplier"/>
								</fo:block>
							</fo:table-cell>
						</fo:table-row>
					</xsl:if>
				</xsl:for-each>
				<fo:table-row keep-with-next.within-column="always">
					<fo:table-cell text-align="left">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions.title'"/>
						</xsl:call-template>
						<fo:block font-size="8pt"><xsl:value-of select="key('label','in_sheet_specialPdf')"/></fo:block>
					</fo:table-cell>
					<fo:table-cell number-columns-spanned="5">
						<xsl:call-template name="attrib">
							<xsl:with-param name="attribute" select="'companions'"/>
						</xsl:call-template>
						<fo:block font-size="7pt" text-align="left">
							<xsl:value-of select="special_properties"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<xsl:if test="count(companion/trick) &gt; 0">
					<fo:table-row keep-with-next.within-column="always">
						<fo:table-cell text-align="left">
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'companions.title'"/>
							</xsl:call-template>
							<fo:block font-size="8pt" text-align="left"><xsl:value-of select="key('label','in_sheet_tricks')"/></fo:block>
						</fo:table-cell>
						<fo:table-cell number-columns-spanned="5">
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'companions'"/>
							</xsl:call-template>
							<fo:block font-size="7pt">
								<xsl:value-of select="trick"/>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
				</xsl:if>
			</fo:table-body>
		</fo:table>
	</xsl:template>
<!-- Disable Previous Equipment Block> -->
<!-- Diable Previous Equipment Stuff -->
	<!--
====================================
====================================
	TEMPLATE - Other Companions
====================================
====================================-->
	<xsl:template match="misc/companions">
		<xsl:if test="count(companion) &gt; 0">	
			<fo:table table-layout="fixed" width="100%" space-before.optimum="2mm">
				<xsl:call-template name="attrib">
					<xsl:with-param name="attribute" select="'magic.border'"/>
				</xsl:call-template>
				<fo:table-column>
					<xsl:attribute name="column-width"><xsl:value-of select="0.5 * ($pagePrintableWidth - 2)" />mm</xsl:attribute>
				</fo:table-column>
				<fo:table-header>
					<fo:table-row keep-with-next.within-column="always">
						<fo:table-cell padding-top="1pt">
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'magic.title'"/>
							</xsl:call-template>
							<fo:block font-size="9pt"><xsl:value-of select="key('label','in_sheet_otherCompanionsPdf')"/></fo:block>
						</fo:table-cell>
					</fo:table-row>
				</fo:table-header>
				<fo:table-body>
					<fo:table-row keep-with-next.within-column="always">
											
						<fo:table-cell>
							<xsl:call-template name="attrib">
								<xsl:with-param name="attribute" select="'magic.lightline'"/>
							</xsl:call-template>
							<fo:block font-size="7pt">
								<xsl:call-template name="paragraghlist">
									<xsl:with-param name="tag" select="'companion'"/>
								</xsl:call-template>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
				</fo:table-body>
			</fo:table>
		</xsl:if>
	</xsl:template>
	<!--
====================================
====================================
	TEMPLATE - Temporary Bonuses
====================================
====================================-->
	<xsl:template match="tempbonuses">
		<xsl:if test="count(tempbonus) &gt; 0">
			<xsl:call-template name="stripped.list">
				<xsl:with-param name="attribute" select="'tempbonuses'" />
				<xsl:with-param name="title" select="key('label','in_sheet_temporaryBonus')" />
				<xsl:with-param name="list" select="tempbonus" />
				<xsl:with-param name="name.tag" select="'name'"/>
				<xsl:with-param name="desc.tag" select="''"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
