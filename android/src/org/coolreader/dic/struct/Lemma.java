package org.coolreader.dic.struct;

import java.util.ArrayList;
import java.util.List;

public class Lemma {
	public String lemmaText = ""; // unsctuctured text for LingvoDSL
	public List<DictEntry> dictEntries = new ArrayList<>();
	public List<TranslLine> translLines = new ArrayList<>();
}
