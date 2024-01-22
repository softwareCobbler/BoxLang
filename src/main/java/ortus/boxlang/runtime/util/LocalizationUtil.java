/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Locale;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;

/**
 * A Collection of Common Static Properties and Methods to support Localization
 **/
public final class LocalizationUtil {

	public static final Struct commonLocales = new Struct(
	    new HashMap<Key, Locale>() {

		    {
			    put( Key.of( "Canada" ), Locale.CANADA );
			    put( Key.of( "Canadian" ), Locale.CANADA );
			    put( Key.of( "Canada_French" ), Locale.CANADA_FRENCH );
			    put( Key.of( "French Canadian" ), Locale.CANADA_FRENCH );
			    put( Key.of( "China" ), Locale.CHINA );
			    put( Key.of( "Chinese" ), Locale.CHINESE );
			    put( Key.of( "English" ), Locale.ENGLISH );
			    put( Key.of( "France" ), Locale.FRANCE );
			    put( Key.of( "French" ), Locale.FRENCH );
			    put( Key.of( "German" ), Locale.GERMAN );
			    put( Key.of( "Germany" ), Locale.GERMANY );
			    put( Key.of( "Italian" ), Locale.ITALIAN );
			    put( Key.of( "Italy" ), Locale.ITALY );
			    put( Key.of( "Japan" ), Locale.JAPAN );
			    put( Key.of( "Japanese" ), Locale.JAPANESE );
			    put( Key.of( "Korea" ), Locale.KOREA );
			    put( Key.of( "Korean" ), Locale.KOREAN );
			    put( Key.of( "PRC" ), Locale.PRC );
			    put( Key.of( "root" ), Locale.ROOT );
			    put( Key.of( "Simplified_Chinese" ), Locale.SIMPLIFIED_CHINESE );
			    put( Key.of( "Taiwan" ), Locale.TAIWAN );
			    put( Key.of( "Traditional_Chinese" ), Locale.TRADITIONAL_CHINESE );
			    put( Key.of( "UK" ), Locale.UK );
			    put( Key.of( "United Kingdom" ), Locale.UK );
			    put( Key.of( "British" ), Locale.UK );
			    put( Key.of( "US" ), Locale.US );
			    put( Key.of( "United States" ), Locale.US );
		    }
	    }
	);

	public static Locale parseLocale( String requestedLocale ) {
		Locale localeObj = null;
		if ( requestedLocale != null && commonLocales.containsKey( requestedLocale ) ) {
			localeObj = ( Locale ) commonLocales.get( requestedLocale );
		} else if ( requestedLocale != null ) {
			var		localeParts	= requestedLocale.split( "-|_| " );
			String	ISOLang		= localeParts[ 0 ];
			String	ISOCountry	= null;
			if ( localeParts.length > 1 ) {
				ISOCountry = localeParts[ 1 ];
			}
			localeObj = ISOCountry == null ? new Locale( ISOLang ) : new Locale( ISOLang, ISOCountry );
		}
		return localeObj;
	}

	public static Locale parseLocaleOrDefault( String requestedLocale, Locale defaultLocale ) {
		Locale locale = parseLocale( requestedLocale );
		return locale != null ? locale : defaultLocale;
	}

	public static ZoneId parseZoneId( String timezone, IBoxContext context ) {
		if ( timezone == null ) {
			timezone = StringCaster.cast( context.getConfigItem( Key.timezone, ZoneId.systemDefault().toString() ) );
		}
		return ZoneId.of( timezone );
	}

	public static DateTimeFormatter localizedDateFormatter( Locale locale, FormatStyle style ) {
		return DateTimeFormatter.ofLocalizedDate( style ).withLocale( locale );
	}

	/**
	 * Returns a localized set of ZonedDateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleZonedDateTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    // Localized styles
		    .appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of DateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */
	public static DateTimeFormatter getLocaleDateTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT, FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM, FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG, FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL, FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ).withLocale( locale ) )
		    // Generic styles
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.ISO_DATE_TIME_MILIS_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_DATETIME_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_INSTANT )
		    .appendOptional( DateTimeFormatter.ISO_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE_TIME )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of Date parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleDateParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ) )
		    // The ISO date methods don't account for leading zeros :(
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy.MM.dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "MM/dd/yyyy" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_DATE_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_DATE )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE )
		    .appendOptional( DateTimeFormatter.BASIC_ISO_DATE )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of Time parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_TIME_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_TIME )
		    .toFormatter( locale );
	}
}
