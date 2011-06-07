package marubinotto.piggydb.presentation.wiki;

import static marubinotto.util.RegexUtils.compile;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Iterator;

import marubinotto.piggydb.model.TagRepository;
import marubinotto.util.Assert;
import marubinotto.util.RegexUtils;
import marubinotto.util.RegexUtils.MatchProcessor;

import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;

public class DefaultWikiParser extends WikiParser {
	
	protected DocumentBuilder documentBuilder;
	protected boolean sectionEnabled = false;

	public DefaultWikiParser() {
	}

	public void setDocumentBuilder(DocumentBuilder documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	public void setSectionEnabled(boolean sectionEnabled) {
		this.sectionEnabled = sectionEnabled;
	}

	@Override
	public void doParse(String wikiText, ParseContext context) throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader(wikiText));
		String line = null;
        while ((line = reader.readLine()) != null) {
        	try {
				processLine(line, context);
			} 
        	catch (StackOverflowError e) {
				context.println(this.documentBuilder.processErrorLine(line));
			}
        }
        this.documentBuilder.finish(context);
	}
	
	@Override
	protected void doParsePreformattedText(String preformattedText, ParseContext context)
	throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader(preformattedText));
		String line = null;
        while ((line = reader.readLine()) != null) {
        	try {
				context.println(processPreformattedLine(context, line));
			} 
        	catch (StackOverflowError e) {
        		context.println(this.documentBuilder.processErrorLine(line));
			}
        }
	}
	
	public static final Pattern P_BLOCK_SEPARATOR = compile("^\\s*$");
	public static final Pattern P_SECTION = compile("^(\\*{1,3})(.*)");
	public static final Pattern P_HORIZONTAL_RULE = compile("^----$");
	public static final Pattern P_BLOCK_QUOTE = compile("^(>{1,3})(.*)");
	public static final Pattern P_UNORDERED_LIST = compile("^(-+)(.*)");
	public static final Pattern P_ORDERED_LIST = compile("^(\\++)(.*)");
	public static final Pattern P_DEFINITION_LIST = compile("^:(.+?):(.*)");
	public static final Pattern P_PREFORMATTED_TEXT = compile("^(\\s+.*)$");
	public static final Pattern P_TABLE_ROW = compile("^(\\|\\|).+(\\|\\|\\s*)$");
	public static final String COLUMN_SEP = "||";

	public void processLine(String line, ParseContext context) throws Exception {
		Assert.Arg.notNull(context, "context");
		if (line == null) {
			return;
		}

		PatternMatcher matcher = context.getMatcher();
		if (matcher.matches(line, P_BLOCK_SEPARATOR)) {
			this.documentBuilder.breakBlocks(context);
        }
		else if (this.sectionEnabled && matcher.matches(line, P_SECTION)) {
			MatchResult result = matcher.getMatch();
			int level = result.group(1).length();
			String title = processInline(context, result.group(2));
			this.documentBuilder.startSection(context, level, title);
		}
		else if (matcher.matches(line, P_HORIZONTAL_RULE)) {
			this.documentBuilder.putHorizontalRule(context);
		}
		else if (matcher.matches(line, P_BLOCK_QUOTE)) {
			MatchResult result = matcher.getMatch();
			int level = result.group(1).length();
			String content = processInline(context, result.group(2));
			this.documentBuilder.appendToBlockquote(context, level, content);
		}
		else if (matcher.matches(line, P_UNORDERED_LIST)) {
			MatchResult result = matcher.getMatch();
			int level = result.group(1).length();
			String content = processInline(context, result.group(2));
			this.documentBuilder.addUnorderedListElement(context, level, content);
		}
		else if (matcher.matches(line, P_ORDERED_LIST)) {
			MatchResult result = matcher.getMatch();
			int level = result.group(1).length();
			String content = processInline(context, result.group(2));
			this.documentBuilder.addOrderedListElement(context, level, content);
		}
		else if (matcher.matches(line, P_DEFINITION_LIST)) {
			MatchResult result = matcher.getMatch();
			String term = processInline(context, result.group(1));
            String description = processInline(context, result.group(2));
            this.documentBuilder.addDefinitionListEntry(context, term, description);
		}
		else if (matcher.matches(line, P_PREFORMATTED_TEXT)) {
			String content = processPreformattedLine(context, line);
			this.documentBuilder.appendToPreformattedText(context, content);
		}
		else if (matcher.matches(line, P_TABLE_ROW)) {
			line = line.trim().substring(COLUMN_SEP.length(), line.length() - COLUMN_SEP.length());
			String[] values = StringUtils.splitByWholeSeparator(line, COLUMN_SEP);
			for (int i = 0; i < values.length; i++) {
				values[i] = processInline(context, values[i]);
			}
			this.documentBuilder.addTableRow(context, values);
		}
		else {
			String content = processInline(context, line);
			this.documentBuilder.appendToParagraph(context, content);
		}
	}

	public static final Pattern P_BOLD = compile("'''([^']+?)'''");
	public static final Pattern P_ITALIC = compile("''([^']+?)''");
	public static final Pattern P_DELETE = compile("__([^_]+?)__");
	
	public static final String PS_URL = "((http|https|ftp|file|fragment):([^\\x00-\\x20()<>\\x7F-\\xFF])*)";	
	public static final Pattern P_URL = compile(PS_URL);
	public static final String PS_LABELED_LINK = "\\[" + PS_URL + "\\ (.+?)\\]";
	public static final Pattern P_LABELED_LINK = compile(PS_LABELED_LINK);
	public static final String PS_FRAGMENT_REF = "(#\\d+)";
	public static final String PS_BREAK = "~";

	protected String processInline(final ParseContext context, String inline) 
	throws Exception {
		inline = this.documentBuilder.escape(inline);
		
		final PatternMatcher matcher = context.getMatcher();
		
		// Bold
		inline = RegexUtils.substitute(matcher, P_BOLD, 1, new MatchProcessor() {
			public String process(String match) {
				return documentBuilder.processBold(context, match);
			}			
		}, inline);
		
		// Italic
		inline = RegexUtils.substitute(matcher, P_ITALIC, 1, new MatchProcessor() {
			public String process(String match) {
				return documentBuilder.processItalic(context, match);
			}
		}, inline);
		
		// Delete
		inline = RegexUtils.substitute(matcher, P_DELETE, 1, new MatchProcessor() {
			public String process(String match) {
				return documentBuilder.processDelete(context, match);
			}
		}, inline);
		
		// Link		
		Pattern linkPattern = compile(
			"(" + PS_URL + "|" + PS_LABELED_LINK + "|" + PS_FRAGMENT_REF + 
				getAllTagNamesRegexAsAdditionalForm(context) + ")");
		
		inline = RegexUtils.substitute(matcher, linkPattern, 1, new MatchProcessor() {
			public String process(String match) {
				if (match.matches(PS_URL)) {
					if (match.matches("^(http|https|ftp|file):.*$")) {
						return documentBuilder.processStandardUrl(context, match, false);
					}
					else if (match.startsWith(FragmentUrl.PREFIX)) {
						FragmentUrl fragmentUrl = new FragmentUrl(match);
						return fragmentUrl.toMarkup(documentBuilder, context);
					}
					return match;
				}
				else if (matcher.matches(match, P_LABELED_LINK)) {
					MatchResult result = matcher.getMatch();
					String label = result.group(4);
					String url = result.group(1);
					if (StringUtils.isBlank(label)) {
						return match;
					}
					url = piggydbUrlToWebUrl(url, context);
					return documentBuilder.processLabeledLink(context, label, url);
				}
				else if (match.matches(PS_FRAGMENT_REF)) {
					long id = Long.parseLong(match.substring(1));
					return documentBuilder.processFragmentRef(context, match, id);
				}
				else {
					return documentBuilder.processTagName(context, match);
				}
			}	
		}, inline);
		
		// Break (low priority than URL, tag names)
		if (inline.endsWith(PS_BREAK)) {
			inline =  this.documentBuilder.appendBreak(StringUtils.chop(inline));
		}

		return inline;
	}
	
	protected String processPreformattedLine(final ParseContext context, String inline) 
	throws Exception {
		inline = this.documentBuilder.escape(inline);

		inline = RegexUtils.substitute(context.getMatcher(), P_URL, 1, new MatchProcessor() {
			public String process(String match) {
				if (match.matches("^(http|https|ftp|file):.*$")) {
					return documentBuilder.processStandardUrl(context, match, true);
				}
				return match;
			}	
		}, inline);

		return inline;
	}
	
	private static String piggydbUrlToWebUrl(String url, ParseContext context) {
		if (url.startsWith(FragmentUrl.PREFIX)) {
			Long id = new FragmentUrl(url).getId();
			if (id != null) return context.getWebResources().fragmentPath(id);
		}
		return url;
	}
	
	private String getAllTagNamesRegexAsAdditionalForm(ParseContext context) 
	throws Exception {
		String tagNamesRegex = getAllTagNamesRegex(context);
		return context.isAuthenticated() && tagNamesRegex != null ? 
				("|" + tagNamesRegex) : "";
	}

	private String getAllTagNamesRegex(ParseContext context) throws Exception {
		TagRepository tagRepository = context.getTagRepository();
		if (tagRepository.size() == 0) {
			return null;
		}
		
		StringBuffer regex = new StringBuffer();
		regex.append("(");
		boolean first = true;
		for (Iterator<String> i = tagRepository.iterateAllTagNames(); i.hasNext();) {
			if (!first) {
				regex.append("|");
			}
			else {
				first = false;
			}
			regex.append(
				RegexUtils.escapeRegex(
					this.documentBuilder.escape(i.next())));
		}
		regex.append(")");
		// logger.debug("AllTagNamesRegex: " + regex);
		return regex.toString();
	}
}