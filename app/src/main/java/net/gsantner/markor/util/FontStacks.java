/*#######################################################
 *
 *   Fork addition (papillon991): font fallback stacks for editor and view mode.
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Bundled fonts (assets/fontstacks) are licensed under the SIL Open Font License, see OFL.txt
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

/**
 * Two font sets, each an ordered list of families (like a CSS font-family list):
 * <ul>
 * <li>myeongjo: Lora, MaruBuri, Shippori Mincho, serif</li>
 * <li>plex: IBM Plex Sans JP, IBM Plex Sans KR, IBM Plex Sans, sans-serif</li>
 * </ul>
 * The font preference stores "stack:myeongjo" / "stack:plex".
 * The editor gets a {@link Typeface} with a custom fallback chain (Android 10+),
 * the view mode gets @font-face CSS and a font-family list.
 */
public final class FontStacks {
    public static final String PREFIX = "stack:";
    public static final String MYEONGJO = PREFIX + "myeongjo";
    public static final String PLEX = PREFIX + "plex";

    /** Fake https origin the view mode loads the bundled fonts from, see MarkorWebViewClient */
    public static final String WEB_BASE = "https://fontstacks.markor.local/";
    public static final String ASSET_DIR = "fontstacks";

    private static final class Face {
        final String file;
        final int weight;
        final boolean italic;
        final boolean variable;

        Face(String file, int weight, boolean italic, boolean variable) {
            this.file = file;
            this.weight = weight;
            this.italic = italic;
            this.variable = variable;
        }
    }

    private static final class Family {
        final String name;
        final Face[] faces;

        Family(String name, Face... faces) {
            this.name = name;
            this.faces = faces;
        }
    }

    private static final Family[] MYEONGJO_FAMILIES = {
            new Family("Lora",
                    new Face("Lora.ttf", 400, false, true), new Face("Lora.ttf", 700, false, true),
                    new Face("Lora-Italic.ttf", 400, true, true), new Face("Lora-Italic.ttf", 700, true, true)),
            new Family("MaruBuri",
                    new Face("MaruBuri-Regular.ttf", 400, false, false), new Face("MaruBuri-Bold.ttf", 700, false, false)),
            new Family("Shippori Mincho",
                    new Face("ShipporiMincho-Regular.ttf", 400, false, false), new Face("ShipporiMincho-Bold.ttf", 700, false, false)),
    };

    private static final Family[] PLEX_FAMILIES = {
            new Family("IBM Plex Sans JP",
                    new Face("IBMPlexSansJP-Regular.ttf", 400, false, false), new Face("IBMPlexSansJP-Bold.ttf", 700, false, false)),
            new Family("IBM Plex Sans KR",
                    new Face("IBMPlexSansKR-Regular.ttf", 400, false, false), new Face("IBMPlexSansKR-Bold.ttf", 700, false, false)),
            new Family("IBM Plex Sans",
                    new Face("IBMPlexSans.ttf", 400, false, true), new Face("IBMPlexSans.ttf", 700, false, true)),
    };

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private FontStacks() {
    }

    public static boolean isStack(final String fontValue) {
        return fontValue != null && fontValue.startsWith(PREFIX);
    }

    private static Family[] families(final String value) {
        return MYEONGJO.equals(value) ? MYEONGJO_FAMILIES : PLEX_FAMILIES;
    }

    private static String systemFallback(final String value) {
        return MYEONGJO.equals(value) ? "serif" : "sans-serif";
    }

    public static synchronized Typeface typeface(final Context context, final String value) {
        Typeface tf = CACHE.get(value);
        if (tf == null) {
            tf = build(context.getApplicationContext().getAssets(), value);
            CACHE.put(value, tf);
        }
        return tf;
    }

    private static Typeface build(final AssetManager am, final String value) {
        final String sys = systemFallback(value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Typeface.create(sys, Typeface.NORMAL);
        }
        try {
            Typeface.CustomFallbackBuilder builder = null;
            for (final Family family : families(value)) {
                Font first = null;
                FontFamily.Builder fb = null;
                for (final Face face : family.faces) {
                    final Font.Builder b = new Font.Builder(am, ASSET_DIR + "/" + face.file)
                            .setWeight(face.weight)
                            .setSlant(face.italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
                    if (face.variable) {
                        b.setFontVariationSettings("'wght' " + face.weight);
                    }
                    final Font font = b.build();
                    if (fb == null) {
                        fb = new FontFamily.Builder(font);
                    } else {
                        fb.addFont(font);
                    }
                }
                final FontFamily ff = fb.build();
                if (builder == null) {
                    builder = new Typeface.CustomFallbackBuilder(ff);
                } else {
                    builder.addCustomFallback(ff);
                }
            }
            builder.setSystemFallback(sys);
            builder.setStyle(new FontStyle(FontStyle.FONT_WEIGHT_NORMAL, FontStyle.FONT_SLANT_UPRIGHT));
            return builder.build();
        } catch (Exception | LinkageError e) {
            return Typeface.create(sys, Typeface.NORMAL);
        }
    }

    /** CSS font-family list (names unquoted so it also works inside style='...' attributes) */
    public static String cssFamilyList(final String value) {
        final StringBuilder sb = new StringBuilder();
        for (final Family family : families(value)) {
            sb.append(family.name).append(',');
        }
        return sb.append(systemFallback(value)).toString();
    }

    /** @font-face rules for the bundled files, served by MarkorWebViewClient */
    public static String cssFontFaces(final String value) {
        final StringBuilder sb = new StringBuilder();
        for (final Family family : families(value)) {
            for (final Face face : family.faces) {
                if (face.variable && face.weight != 400) {
                    continue; // one rule covers the whole weight range
                }
                final String weight = face.variable ? "400 700" : String.valueOf(face.weight);
                sb.append("@font-face{font-family:'").append(family.name)
                        .append("';src:url('").append(WEB_BASE).append(face.file.replace(" ", "%20"))
                        .append("') format('truetype');font-weight:").append(weight)
                        .append(";font-style:").append(face.italic ? "italic" : "normal")
                        .append(";font-display:swap;}");
            }
        }
        return sb.toString();
    }

    /** True if the url points to a bundled font, returns the asset file name or null */
    public static String assetNameForUrl(final String url) {
        if (url == null || !url.startsWith(WEB_BASE)) {
            return null;
        }
        final String name = url.substring(WEB_BASE.length()).replace("%20", " ");
        return name.matches("[A-Za-z0-9_\\- ]+\\.ttf") ? name : null;
    }
}
