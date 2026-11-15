/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownSyntaxHighlighter extends SyntaxHighlighterBase {

    public final static Pattern BOLD = Pattern.compile("(?<=(\\n|^|\\s|\\[|\\{|\\())(([*_]){2,3})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s|\\.|,|:|;|-|\\]|\\}|\\)))");
    public final static Pattern ITALICS = Pattern.compile("(?<=(\\n|^|\\s|\\[|\\{|\\())([*_])(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s|\\.|,|:|;|-|\\]|\\}|\\)))");
    public final static Pattern HEADING = Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))");
    public final static Pattern HEADING_SIMPLE = Pattern.compile("(?m)^(#{1,6}\\s.*$)");
    // Group 1 matches image, Group 2 matches text, group 3 matches path
    public static final Pattern LINK = Pattern.compile("(?m)(!)?\\[([^]]*)]\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(\\n|^)\\s{0,16}([*+-])( \\[[ xX]\\])?(?= )");
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^\\s{0,16}(\\d+)(:?\\.|\\))\\s");
    public final static Pattern QUOTATION = Pattern.compile("(\\n|^)>");
    public final static Pattern STRIKETHROUGH = Pattern.compile("~{2}(.*?)\\S~{2}");
    public final static Pattern CODE = Pattern.compile("(?m)(`(?!`)(.*?)`)|(^[^\\S\\n]{4}(?![0-9\\-*+]).*$)");
    public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n");

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0x60afafaf;

    public MarkdownSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    private boolean _highlightLineEnding;
    private boolean _highlightCodeChangeFont;
    private boolean _highlightBiggerHeadings;
    private boolean _highlightCodeBlock;

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isHighlightCodeMonospaceFont();
        _highlightBiggerHeadings = _appSettings.isHighlightBiggerHeadings();
        _highlightCodeBlock = _appSettings.isHighlightCodeBlock();
        _delay = _appSettings.getMarkdownHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        // Fork: in dark mode the Pixel Cold War palette picked with the editor colour preset, as ghostwriter on the
        // desktop: headings, list signs and links in its accent, quotes olive, code on its bakelite ground.
        // Light mode keeps Markor's colours.
        final Context context = _appSettings.getContext();
        final int accent = ContextCompat.getColor(context, _appSettings.getForkPcwColor(AppSettings.PCW_ACCENT));
        final int cHeading = _isDarkMode ? accent : MD_COLOR_HEADING;
        final int cLink = _isDarkMode ? accent : MD_COLOR_LINK;
        final int cList = _isDarkMode ? accent : MD_COLOR_LIST;
        final int cQuote = _isDarkMode ? ContextCompat.getColor(context, R.color.md_quote_dark) : MD_COLOR_QUOTE;
        final int cCodeblock = _isDarkMode ? ContextCompat.getColor(context, _appSettings.getForkPcwColor(AppSettings.PCW_PLATE)) : MD_COLOR_CODEBLOCK;

        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();

        if (_highlightBiggerHeadings) {
            createSpanForMatches(HEADING, new WrMarkdownHeaderSpanCreator(_spannable, cHeading));
        } else {
            createColorSpanForMatches(HEADING, cHeading);
        }

        createColorSpanForMatches(LINK, cLink);
        createColorSpanForMatches(LIST_UNORDERED, cList);
        createColorSpanForMatches(LIST_ORDERED, cList);

        if (_highlightLineEnding) {
            createColorBackgroundSpan(DOUBLESPACE_LINE_ENDING, cCodeblock);
        }

        createStyleSpanForMatches(BOLD, Typeface.BOLD);
        createStyleSpanForMatches(ITALICS, Typeface.ITALIC);
        createColorSpanForMatches(QUOTATION, cQuote);
        createStrikeThroughSpanForMatches(STRIKETHROUGH);

        if (_highlightCodeChangeFont) {
            createMonospaceSpanForMatches(CODE);
        }

        if (_highlightCodeBlock) {
            createColorBackgroundSpan(CODE, cCodeblock);
        }
    }
}