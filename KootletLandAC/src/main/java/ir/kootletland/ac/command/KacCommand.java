package ir.kootletland.ac.command;

import ir.kootletland.ac.engine.AntiCheatEngine;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public final class KacCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final AntiCheatEngine engine;
    public KacCommand(Plugin plugin,AntiCheatEngine engine){this.plugin=plugin;this.engine=engine;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("kootletlandac.admin")){s.sendMessage("§cNo permission.");return true;}
        if(a.length==0){s.sendMessage("§e/kac debug <player> §7| §e/kac alerts §7| §e/kac stats §7| §e/kac reload §7| §e/kac verbose <player>");return true;}
        switch(a[0].toLowerCase()){
            case "debug","verbose" -> {if(a.length<2){s.sendMessage("§cPlayer required.");return true;} Player p=Bukkit.getPlayerExact(a[1]); if(p==null){s.sendMessage("§cPlayer not found.");return true;} s.sendMessage(engine.debug(p)); if(a[0].equalsIgnoreCase("verbose")) engine.get(p.getUniqueId()).evidence().stream().limit(8).forEach(e->s.sendMessage("§8- §e"+e.check()+" §7conf="+String.format("%.1f%%",e.confidence()*100)+" §7vl="+String.format("%.2f",e.violation())+" §7"+e.data()));}
            case "stats" -> s.sendMessage("§eKootletAC §7checks="+engine.checks()+" detections="+engine.detections()+" alerts="+engine.alerts()+" players="+engine.players().size()+" avgCheck="+String.format("%.2fµs",engine.metrics().avgMicros())+" metricChecks="+engine.metrics().checks()+" uptime="+engine.uptime()/1000+"s");
            case "alerts" -> s.sendMessage("§eKootletAC §7alerts are " + (plugin.getConfig().getBoolean("settings.alerts")?"enabled":"disabled")+". Automatic punishment: §cOFF");
            case "reload" -> {plugin.reloadConfig();engine.reloadChecks();s.sendMessage("§aKootletAC configuration reloaded without resetting player state.");}
            default -> s.sendMessage("§cUnknown subcommand.");
        } return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){if(args.length==1)return List.of("debug","verbose","alerts","stats","reload"); if(args.length==2 && (args[0].equalsIgnoreCase("debug")||args[0].equalsIgnoreCase("verbose")))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();return List.of();}
}
