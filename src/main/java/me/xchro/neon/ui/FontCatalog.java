package me.xchro.neon.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.*;

/** Local font families exposed by the operating system; no font files leave the machine. */
public final class FontCatalog {
    public static final String DEFAULT="Inter";
    private static final class Installed {
        static final List<String> FAMILIES=load();
        private static List<String> load(){
            var names=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            names.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)));
            names.remove(DEFAULT);var result=new ArrayList<String>();result.add(DEFAULT);result.addAll(names);return List.copyOf(result);
        }
    }
    private FontCatalog(){}
    public static List<String> families(){return Installed.FAMILIES;}
    public static String canonical(String family){
        if(family==null||DEFAULT.equalsIgnoreCase(family))return DEFAULT;
        for(String name:families())if(name.equalsIgnoreCase(family))return name;
        return null;
    }
    public static boolean available(String family){return canonical(family)!=null;}
    static Font system(String family,boolean heading){
        String name=canonical(family);if(name==null||DEFAULT.equals(name))return null;
        return new Font(name,heading?Font.BOLD:Font.PLAIN,12).deriveFont(Map.of(TextAttribute.KERNING,TextAttribute.KERNING_ON,TextAttribute.LIGATURES,TextAttribute.LIGATURES_ON));
    }
}
