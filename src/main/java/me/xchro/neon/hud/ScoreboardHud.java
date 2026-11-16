package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.scores.*;

/** Optional movable presentation of the native sidebar, including team colors and number formats. */
final class ScoreboardHud extends HudCard {
    private static final Comparator<PlayerScoreEntry> ORDER=Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner,String.CASE_INSENSITIVE_ORDER);
    private Component title=Component.empty();
    private FormattedCharSequence heading=FormattedCharSequence.EMPTY;
    private FormattedCharSequence[] names=new FormattedCharSequence[0],scores=new FormattedCharSequence[0];
    private int[] scoreWidths=new int[0];
    private int width=80,headingWidth;
    ScoreboardHud() { super(HudKind.SCOREBOARD); }
    @Override protected void update(Minecraft mc,boolean preview) {
        var labels=new ArrayList<Component>();var values=new ArrayList<Component>();
        Objective objective=null;
        if(mc.level!=null&&mc.player!=null) {
            var board=mc.level.getScoreboard();var team=board.getPlayersTeam(mc.player.getScoreboardName());
            if(team!=null&&team.getColor().isPresent())objective=board.getDisplayObjective(team.getColor().get().displaySlot());
            if(objective==null)objective=board.getDisplayObjective(DisplaySlot.SIDEBAR);
            if(objective!=null) {
                title=objective.getDisplayName();var format=objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
                board.listPlayerScores(objective).stream().filter(entry->!entry.isHidden()).sorted(ORDER).limit(Neon.config.scoreboardRows).forEach(entry->{
                    labels.add(PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()),entry.ownerName()));
                    values.add(Neon.config.scoreboardScores?entry.formatValue(format):Component.empty());
                });
            }
        }
        if(objective==null) {
            title=Component.literal("NEON");
            if(preview) { labels.add(Component.literal("Scoreboard"));values.add(Component.literal(Neon.config.scoreboardScores?"12":"")); }
            else { lines=new String[0];return; }
        }
        var font=mc.font;boolean showTitle=Neon.config.scoreboardTitle;
        width=showTitle?Math.max(80,text.width(title.getVisualOrderText())):80;
        for(int i=0;i<labels.size();i++)width=Math.max(width,text.width(labels.get(i).getVisualOrderText())+text.width(values.get(i).getVisualOrderText())+8);
        width=Math.min(220,width);int count=labels.size();
        names=new FormattedCharSequence[count];scores=new FormattedCharSequence[count];scoreWidths=new int[count];
        lines=new String[count+(showTitle?1:0)];if(showTitle)lines[0]=title.getString();
        heading=clip(title,width);headingWidth=text.width(heading);
        for(int i=0;i<count;i++) {
            scores[i]=clip(values.get(i),width/2);scoreWidths[i]=text.width(scores[i]);
            names[i]=clip(labels.get(i),width-(scoreWidths[i]>0?scoreWidths[i]+8:0));
            lines[i+(showTitle?1:0)]=labels.get(i).getString()+(values.get(i).getString().isEmpty()?"":"  "+values.get(i).getString());
        }
    }
    private FormattedCharSequence clip(Component value,int width) {
        var full=value.getVisualOrderText();if(text.width(full)<=width)return full;
        var font=Minecraft.getInstance().font;int lo=0,hi=font.width(value);
        while(lo<hi) { int mid=(lo+hi+1)/2;var candidate=Language.getInstance().getVisualOrder(font.substrByWidth(value,mid));if(text.width(candidate)<=width)lo=mid;else hi=mid-1; }
        return Language.getInstance().getVisualOrder(font.substrByWidth(value,lo));
    }
    @Override protected int contentWidth() { return width; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var theme=Neon.config.theme;boolean shadow=Neon.config.hud(kind).shadow;int y=3;
        if(Neon.config.scoreboardTitle) { text.draw(g,heading,5+(width-headingWidth)/2,y,theme.color(ThemeColor.ACCENT),shadow);y+=text.height()+3; }
        for(int i=0;i<names.length;i++) {
            text.draw(g,names[i],5,y,theme.color(ThemeColor.TEXT),shadow);
            if(scoreWidths[i]>0)text.draw(g,scores[i],5+width-scoreWidths[i],y,theme.color(ThemeColor.MUTED),shadow);
            y+=text.height()+3;
        }
    }
}
