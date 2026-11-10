package me.xchro.neon.core;

import java.awt.Color;
import java.util.*;
import net.minecraft.ChatFormatting;

/** Unity ColorArgumentType's accepted paste formats, without its command/UI dependencies. */
public final class ColorValue {
    private ColorValue() {}
    public static OptionalInt parse(String input) {
        try {
            String value=input.trim();
            if(value.matches("#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
                if(value.length()==4)value="#"+value.charAt(1)+value.charAt(1)+value.charAt(2)+value.charAt(2)+value.charAt(3)+value.charAt(3);
                long number=Long.parseLong(value.substring(1),16);
                return OptionalInt.of(value.length()==9?(int)(number>>>8)|(int)(number&255)<<24:0xff000000|(int)number);
            }
            for(var format:ChatFormatting.values()) { var named=net.minecraft.network.chat.TextColor.fromLegacyFormat(format); if(named!=null&&value.equalsIgnoreCase(named.serialize()))return OptionalInt.of(0xff000000|named.getValue()); }
            boolean hsb=value.startsWith("hsb:"); if(hsb)value=value.substring(4);
            int[] parts=Arrays.stream(value.split(",",-1)).map(String::trim).mapToInt(Integer::parseInt).toArray();
            if(hsb){ if(parts.length!=3||parts[0]<0||parts[0]>360||parts[1]<0||parts[1]>100||parts[2]<0||parts[2]>100)return OptionalInt.empty(); return OptionalInt.of(Color.HSBtoRGB(parts[0]/360f,parts[1]/100f,parts[2]/100f)); }
            if(parts.length!=3&&parts.length!=4)return OptionalInt.empty(); for(int part:parts)if(part<0||part>255)return OptionalInt.empty();
            return OptionalInt.of((parts.length==4?parts[3]:255)<<24|parts[0]<<16|parts[1]<<8|parts[2]);
        }catch(RuntimeException invalid){return OptionalInt.empty();}
    }
}
