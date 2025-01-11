package org.grobid.analyzers.grobidkr.utils;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.grobid.analyzers.grobidkr.morph.MorphException;

public class KoreanEnv {

	public static final String FILE_SYLLABLE_FEATURE = "org/grobid/analyzers/grobidkr/dic/syllable.dic";
	public static final String FILE_DICTIONARY = "org/grobid/analyzers/grobidkr/dic/total.dic";
	public static final String FILE_JOSA = "org/grobid/analyzers/grobidkr/dic/josa.dic";
	public static final String FILE_EOMI = "org/grobid/analyzers/grobidkr/dic/eomi.dic";	
	public static final String FILE_EXTENSION = "org/grobid/analyzers/grobidkr/dic/extension.dic";
	public static final String FILE_PREFIX = "org/grobid/analyzers/grobidkr/dic/prefix.dic";	
	public static final String FILE_SUFFIX = "org/grobid/analyzers/grobidkr/dic/suffix.dic";
	public static final String FILE_COMPOUNDS = "org/grobid/analyzers/grobidkr/dic/compounds.dic";	
	public static final String FILE_UNCOMPOUNDS = "org/grobid/analyzers/grobidkr/dic/compounds.dic";
	public static final String FILE_CJ = "org/grobid/analyzers/grobidkr/dic/cj.dic";
	
	public static final String FILE_KOREAN_PROPERTY = "src/main/java/org/grobid/analyzers/grobidkr/korean.properties";
	
	private Properties defaults = null;

	/**
	 * The props member gets its values from the configuration in the property file.
	 */
	private Properties props = null;
	
	private static volatile KoreanEnv instance;
	
	/**
	 * The constructor loads property values from the property file.
	 */
	private KoreanEnv() throws MorphException {
		try {
			initDefaultProperties();
			props = loadProperties(defaults);
		} 
		catch (MorphException e) {
			throw new MorphException ("Failure while initializing property values:\n"+e.getMessage());
		}
	}
	
	public static KoreanEnv getInstance() throws MorphException {
		if (instance == null) {
			synchronized (KoreanEnv.class) {
				if (instance == null) {
					getNewInstance();
				}
			}
		}
		return instance;
	}
	

	private static synchronized void getNewInstance() throws MorphException {
		instance = new KoreanEnv();
	}

	/**
	 * Initialize the default property values.
	 */
	private void initDefaultProperties() {
		defaults = new Properties();
		
		defaults.setProperty(FILE_SYLLABLE_FEATURE,"org/grobid/analyzers/grobidkr/dic/syllable.dic");
		defaults.setProperty(FILE_DICTIONARY,"org/grobid/analyzers/grobidkr/dic/dictionary.dic");
		defaults.setProperty(FILE_DICTIONARY,"org/grobid/analyzers/grobidkr/dic/extension.dic");		
		defaults.setProperty(FILE_JOSA,"org/grobid/analyzers/grobidkr/dic/josa.dic");	
		defaults.setProperty(FILE_EOMI,"org/grobid/analyzers/grobidkr/dic/eomi.dic");	
		defaults.setProperty(FILE_PREFIX,"org/grobid/analyzers/grobidkr/dic/prefix.dic");		
		defaults.setProperty(FILE_SUFFIX,"org/grobid/analyzers/grobidkr/dic/suffix.dic");	
		defaults.setProperty(FILE_COMPOUNDS,"org/grobid/analyzers/grobidkr/dic/compounds.dic");	
		defaults.setProperty(FILE_UNCOMPOUNDS,"org/grobid/analyzers/grobidkr/dic/uncompounds.dic");
		defaults.setProperty(FILE_CJ,"org/grobid/analyzers/grobidkr/dic/cj.dic");
	 }

	/**
	 * Given a property file name, load the property file and return an object
	 * representing the property values.
	 *
	 * @param propertyFile The name of the property file to load.
	 * @param def Default property values, or <code>null</code> if there are no defaults.
	 * @return The loaded SortedProperties object.
	 */
	private Properties loadProperties(Properties def) throws MorphException {
		Properties properties = new Properties();

		if (def != null) {
			properties = new Properties(def);
		}

		File file = null;
		try {
			file = new File(FILE_KOREAN_PROPERTY);
			if (file != null && file.exists()) {
				properties.load(new FileInputStream(file));
				return properties;
			}
			byte[] in = FileUtil.readByteFromCurrentJar(FILE_KOREAN_PROPERTY);
			properties.load(new ByteArrayInputStream(in));
		} catch (Exception e) {
			//System.err.println("Failure while trying to load properties file '"+FILE_KOREAN_PROPERTY);
			//System.err.println("using default in jar resources");
			//System.err.println(e.getMessage());
			//throw new MorphException("Failure while trying to load properties file " + (file==null?"null":file.getPath()), e);
		}
		return properties;
	}
	
	/**
	 * Returns the value of a property.
	 *
	 * @param name The name of the property whose value is to be retrieved.
	 * @return The value of the property.
	 */
	public String getValue(String name) {
		return props.getProperty(name);
	}
}
