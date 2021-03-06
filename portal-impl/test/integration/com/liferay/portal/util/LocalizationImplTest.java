/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.util;

import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Localization;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.test.LiferayIntegrationTestRule;
import com.liferay.portal.util.test.RandomTestUtil;
import com.liferay.portlet.PortletPreferencesImpl;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletPreferences;

import org.apache.commons.collections.map.ReferenceMap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletRequest;

/**
 * @author Connor McKay
 * @author Peter Borkuti
 */
public class LocalizationImplTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_cache = LocalizationImpl.class.getDeclaredField("_cache");

		_cache.setAccessible(true);

		_defaultLocale = LocaleUtil.getDefault();
		_localization = LocalizationUtil.getLocalization();
	}

	@AfterClass
	public static void tearDownClass() {
		LocaleUtil.setDefault(
			_defaultLocale.getLanguage(), _defaultLocale.getCountry(),
			_defaultLocale.getVariant());
	}

	@Before
	public void setUp() throws Exception {
		StringBundler sb = new StringBundler();

		sb.append("<?xml version='1.0' encoding='UTF-8'?>");

		sb.append("<root available-locales=\"en_US,es_ES\" ");
		sb.append("default-locale=\"en_US\">");
		sb.append("<static-content language-id=\"es_ES\">");
		sb.append("foo&amp;bar");
		sb.append("</static-content>");
		sb.append("<static-content language-id=\"en_US\">");
		sb.append("<![CDATA[Example in English]]>");
		sb.append("</static-content>");
		sb.append("</root>");

		_xml = sb.toString();

		_cache.set(
			_localization,
			new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.HARD));
	}

	@Test
	public void testChunkedText() {
		String translation = LocalizationUtil.getLocalization(_xml, "es_ES");

		Assert.assertNotNull(translation);
		Assert.assertEquals("foo&bar", translation);
		Assert.assertEquals(7, translation.length());

		translation = LocalizationUtil.getLocalization(_xml, "en_US");

		Assert.assertNotNull(translation);
		Assert.assertEquals(18, translation.length());
	}

	@Test
	public void testGetAvailableLanguageIds() throws DocumentException {
		Document document = SAXReaderUtil.read(_xml);

		String[] documentAvailableLanguageIds =
			LocalizationUtil.getAvailableLanguageIds(document);

		Assert.assertEquals(documentAvailableLanguageIds.length, 2);

		String[] xmlAvailableLanguageIds =
			LocalizationUtil.getAvailableLanguageIds(_xml);

		Assert.assertEquals(xmlAvailableLanguageIds.length, 2);

		Arrays.sort(documentAvailableLanguageIds);
		Arrays.sort(xmlAvailableLanguageIds);

		Assert.assertTrue(
			"Available language IDs between the document and XML do not match",
			Arrays.equals(
				documentAvailableLanguageIds, xmlAvailableLanguageIds));
	}

	@Test
	public void testGetDefaultLanguageId() throws DocumentException {
		Document document = SAXReaderUtil.read(_xml);

		String languageIdsFromDoc = LocalizationUtil.getDefaultLanguageId(
			document);
		String languageIdsFromXml = LocalizationUtil.getDefaultLanguageId(_xml);

		Assert.assertEquals(
			"The default language ids from Document and XML don't match",
			languageIdsFromDoc, languageIdsFromXml);
	}

	@Test
	public void testGetModifiedLocales() throws Exception {
		String key = RandomTestUtil.randomString();

		String defaultLanguageId = LocaleUtil.toLanguageId(
			LocaleUtil.getDefault());

		PortletPreferences preferences = new PortletPreferencesImpl();

		LocalizationUtil.setPreferencesValue(
			preferences, key, defaultLanguageId, "A0");
		LocalizationUtil.setPreferencesValue(
			preferences, key, _GERMAN_LANGUAGE_ID, "B0");

		Map<Locale, String> oldLocalizationMap =
			LocalizationUtil.getLocalizationMap(preferences, key);

		LocalizationUtil.setPreferencesValue(
			preferences, key, defaultLanguageId, "A1");
		LocalizationUtil.setPreferencesValue(
			preferences, key, _GERMAN_LANGUAGE_ID, "B1");

		Map<Locale, String> newLocalizationMap =
			LocalizationUtil.getLocalizationMap(preferences, key);

		List<Locale> modifiedLocales = LocalizationUtil.getModifiedLocales(
			oldLocalizationMap, newLocalizationMap);

		Assert.assertTrue(modifiedLocales.contains(LocaleUtil.getDefault()));
		Assert.assertTrue(modifiedLocales.contains(LocaleUtil.GERMANY));
	}

	@Test
	public void testGetPreferencesValue() throws Exception {
		String key = RandomTestUtil.randomString();

		LocaleUtil.setDefault(
			LocaleUtil.US.getLanguage(), LocaleUtil.US.getCountry(),
			LocaleUtil.US.getVariant());

		PortletPreferences preferences = new PortletPreferencesImpl();

		LocalizationUtil.setPreferencesValue(
			preferences, key, _ENGLISH_LANGUAGE_ID, "A");
		LocalizationUtil.setPreferencesValue(
			preferences, key, _GERMAN_LANGUAGE_ID, "B");

		Assert.assertEquals(
			"A",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _ENGLISH_LANGUAGE_ID));
		Assert.assertEquals(
			"B",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _GERMAN_LANGUAGE_ID));
		Assert.assertEquals(
			"A",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _SPANISH_LANGUAGE_ID));

		LocaleUtil.setDefault(
			LocaleUtil.GERMANY.getLanguage(), LocaleUtil.GERMANY.getCountry(),
			LocaleUtil.GERMANY.getVariant());

		Assert.assertEquals(
			"A",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _ENGLISH_LANGUAGE_ID));
		Assert.assertEquals(
			"B",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _GERMAN_LANGUAGE_ID));
		Assert.assertEquals(
			"B",
			LocalizationUtil.getPreferencesValue(
				preferences, key, _SPANISH_LANGUAGE_ID));
	}

	@Test
	public void testLocalizationsXML() {
		String xml = StringPool.BLANK;

		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _ENGLISH_HELLO, _ENGLISH_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);
		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _GERMAN_HELLO, _GERMAN_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);

		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getLocalization(xml, _ENGLISH_LANGUAGE_ID));
		Assert.assertEquals(
			_GERMAN_HELLO,
			LocalizationUtil.getLocalization(xml, _GERMAN_LANGUAGE_ID));
	}

	@Test
	public void testLocalizationsXMLDefaultValue() {
		String xml = StringPool.BLANK;

		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _ENGLISH_HELLO, _ENGLISH_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);
		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _GERMAN_HELLO, _GERMAN_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);

		String defaultValue = "Default Value";

		Assert.assertEquals(
			defaultValue,
			LocalizationUtil.getLocalization(
				xml, _SPANISH_LANGUAGE_ID, false, defaultValue));
		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getLocalization(
				xml, _SPANISH_LANGUAGE_ID, true, defaultValue));
	}

	@Test
	public void testLocalizationsXMLUseDefault() {
		String xml = StringPool.BLANK;

		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _ENGLISH_HELLO, _ENGLISH_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);
		xml = LocalizationUtil.updateLocalization(
			xml, "greeting", _GERMAN_HELLO, _GERMAN_LANGUAGE_ID,
			_ENGLISH_LANGUAGE_ID);

		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getLocalization(xml, _SPANISH_LANGUAGE_ID, true));
	}

	@Test
	public void testLongTranslationText() {
		StringBundler sb = new StringBundler();

		sb.append("<?xml version='1.0' encoding='UTF-8'?>");

		sb.append("<root available-locales=\"en_US,es_ES\" ");
		sb.append("default-locale=\"en_US\">");
		sb.append("<static-content language-id=\"es_ES\">");
		sb.append("<![CDATA[");

		int loops = 2000000;

		for (int i = 0; i < loops; i++) {
			sb.append("1234567890");
		}

		sb.append("]]>");
		sb.append("</static-content>");
		sb.append("<static-content language-id=\"en_US\">");
		sb.append("<![CDATA[Example in English]]>");
		sb.append("</static-content>");
		sb.append("</root>");

		int totalSize = loops * 10;

		Assert.assertTrue(sb.length() > totalSize);

		String translation = LocalizationUtil.getLocalization(
			sb.toString(), "es_ES");

		Assert.assertNotNull(translation);
		Assert.assertEquals(totalSize, translation.length());

		translation = LocalizationUtil.getLocalization(sb.toString(), "en_US");

		Assert.assertNotNull(translation);
		Assert.assertEquals(18, translation.length());
	}

	@Test
	public void testPreferencesLocalization() throws Exception {
		PortletPreferences preferences = new PortletPreferencesImpl();

		LocalizationUtil.setPreferencesValue(
			preferences, "greeting", _ENGLISH_LANGUAGE_ID, _ENGLISH_HELLO);
		LocalizationUtil.setPreferencesValue(
			preferences, "greeting", _GERMAN_LANGUAGE_ID, _GERMAN_HELLO);

		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getPreferencesValue(
				preferences, "greeting", _ENGLISH_LANGUAGE_ID));
		Assert.assertEquals(
			_GERMAN_HELLO,
			LocalizationUtil.getPreferencesValue(
				preferences, "greeting", _GERMAN_LANGUAGE_ID));
	}

	@Test
	public void testSetLocalizedPreferencesValues() throws Exception {
		MockPortletRequest request = new MockPortletRequest();

		request.setParameter(
			"greeting_" + _ENGLISH_LANGUAGE_ID, _ENGLISH_HELLO);
		request.setParameter("greeting_" + _GERMAN_LANGUAGE_ID, _GERMAN_HELLO);

		PortletPreferences preferences = new PortletPreferencesImpl();

		LocalizationUtil.setLocalizedPreferencesValues(
			request, preferences, "greeting");

		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getPreferencesValue(
				preferences, "greeting", _ENGLISH_LANGUAGE_ID));
		Assert.assertEquals(
			_GERMAN_HELLO,
			LocalizationUtil.getPreferencesValue(
				preferences, "greeting", _GERMAN_LANGUAGE_ID));
	}

	@Test
	public void testUpdateLocalization() {
		Map<Locale, String>localizationMap = new HashMap<>();

		localizationMap.put(LocaleUtil.US, _ENGLISH_HELLO);

		StringBundler sb = new StringBundler();

		sb.append("<?xml version='1.0' encoding='UTF-8'?>");
		sb.append("<root available-locales=\"en_US,de_DE\" ");
		sb.append("default-locale=\"en_US\">");
		sb.append("<greeting language-id=\"de_DE\">");
		sb.append("<![CDATA[Beispiel auf Deutsch]]>");
		sb.append("</greeting>");
		sb.append("<greeting language-id=\"en_US\">");
		sb.append("<![CDATA[Example in English]]>");
		sb.append("</greeting>");
		sb.append("</root>");

		String xml = LocalizationUtil.updateLocalization(
			localizationMap, sb.toString(), "greeting",
			LocaleUtil.toLanguageId(LocaleUtil.getDefault()));

		Assert.assertEquals(
			_ENGLISH_HELLO,
			LocalizationUtil.getLocalization(xml, _ENGLISH_LANGUAGE_ID, false));
		Assert.assertEquals(
			StringPool.BLANK,
			LocalizationUtil.getLocalization(xml, _GERMAN_LANGUAGE_ID, false));
	}

	private static final String _ENGLISH_HELLO = "Hello World";

	private static final String _ENGLISH_LANGUAGE_ID = LocaleUtil.toLanguageId(
		LocaleUtil.US);

	private static final String _GERMAN_HELLO = "Hallo Welt";

	private static final String _GERMAN_LANGUAGE_ID = LocaleUtil.toLanguageId(
		LocaleUtil.GERMANY);

	private static final String _SPANISH_LANGUAGE_ID = LocaleUtil.toLanguageId(
		LocaleUtil.SPAIN);

	private static Field _cache;
	private static Locale _defaultLocale;
	private static Localization _localization;

	private String _xml;

}