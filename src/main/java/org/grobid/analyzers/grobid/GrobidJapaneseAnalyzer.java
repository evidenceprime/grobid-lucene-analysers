package org.grobid.analyzers.grobid;


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

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;
import org.grobid.analyzers.FilterDeleteSpaceBetweenDigits;
import org.grobid.analyzers.FilterDeleteSpaceBetweenSameAlphabet;
import org.grobid.analyzers.GrobidFilterTwoBytesLatinChars;

/**
 * Analyzer for Japanese that uses morphological analysis.
 * @see JapaneseTokenizer
 */
public class GrobidJapaneseAnalyzer extends StopwordAnalyzerBase {
  private final Mode mode;

  private UserDictionary userDict;
  
  public GrobidJapaneseAnalyzer() { // Provide a default constructor
	  this(Version.LUCENE_45,null, Mode.NORMAL);
  }

  public GrobidJapaneseAnalyzer(Version matchVersion) {
    this(matchVersion, null,  Mode.NORMAL);
  }
  
  public GrobidJapaneseAnalyzer(Version matchVersion, UserDictionary userDict, Mode mode) {
    super(matchVersion);
    this.userDict = userDict;
    this.mode = mode;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
	  
    Tokenizer tokenizer = new JapaneseTokenizer(reader, userDict, false, mode);
    TokenStream stream = tokenizer;

    stream = new GrobidFilterTwoBytesLatinChars(stream);
    stream = new FilterDeleteSpaceBetweenDigits(stream);

    return new TokenStreamComponents(tokenizer, stream);
  }
  
}
